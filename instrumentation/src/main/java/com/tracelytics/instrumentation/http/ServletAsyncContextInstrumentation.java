package com.tracelytics.instrumentation.http;

import java.util.Arrays;
import java.util.List;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtField;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.ext.javassist.CtNewMethod;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.MethodMatcher;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.span.impl.Scope;
import com.tracelytics.joboe.span.impl.Span;

/**
 * Instruments servlet 3.0 class {@link javax.servlet.AsyncContext}. 
 * 
 * When a <code>ServletRequest</code> enters asynchronous mode via {@link javax.servlet.ServletRequest#startAsync}, an <code>AsyncContext</code> is created. 
 * 
 * The <code>startAsync</code> method will cause committal of the associated response to be delayed until <code>AsyncContext#complete</code> is called on the returned <code>AsyncContext</code>
 * . Also <code>AsyncContext#dispatch</code> can pass the control to a container managed thread to dispatch to another path and returns immediately.
 * 
 * Therefore, when in asynchronous mode, we no longer always want to create the layer/trace exit on exit of <code>ServletRequest.service</code>, instead we might want to exit the layer/trace on the 
 * <code>AsyncContext#complete</code> or after the handling of the dispatched target, whichever happens last. 
 * 
 * In order to achieve such a behavior, we have to flag whether the code flow is in active asynchronous mode such that {@link ServletInstrumentation} can make decision of whether to 
 * start/end trace. When <code>ServletRequest#startAsync<code> is invoked we will flag the <code>AsyncContext</code> as active, and when <code>javax.servlet.AsyncContext#complete</code> is called
 * we will check whether the context is active and whether this is the last handling remaining (by checking <code>com.tracelytics.instrumentation.http.HttpServletResponse.tlysReqCount()</code>).
 * If it is the last remaining handling, then we should create exit event to end the trace
 * 
 *  
 * @author pluk
 *
 */
public class ServletAsyncContextInstrumentation extends ClassInstrumentation {
    private static final String CLASS_NAME = ServletAsyncContextInstrumentation.class.getName();
    
    // List of method matchers that declare method with signature patterns that we want to instrument
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<OpType>> methodMatchers = Arrays.asList(
         new MethodMatcher<OpType>("complete", new String[] { }, "void", OpType.COMPLETE)
    );
    
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {
        cc.addField(CtField.make("private java.util.List tvSpanStack;", cc));
        cc.addMethod(CtNewMethod.make("public java.util.List tvGetSpanStack() { return tvSpanStack; }", cc));
        cc.addMethod(CtNewMethod.make("public void tvSetSpanStack(java.util.List spanStack) { tvSpanStack = spanStack; }", cc));
        
        tagInterface(cc, ServletAsyncContext.class.getName());
                
        for (CtMethod completeMethod : findMatchingMethods(cc, methodMatchers).keySet()) {
            if (shouldModify(cc, completeMethod)) {
                //has to insert BEFORE the complete() call, otherwise some newer servlet implementation might throw except for calling getRequest() after complete()
                insertBefore(completeMethod, CLASS_NAME + ".layerExit(getRequest(), getResponse() , this);", false);
            }
        }
        
        return true;
    }
    
    /**
     * When the asyncContext completes, we might want to create the exit event if the tlysReqCount is zero
     * @param requestObject
     * @param responseObject
     * @param asyncContext
     */
    public static void layerExit(Object requestObject, Object responseObject, ServletAsyncContext asyncContext) {
        if (requestObject instanceof HttpServletRequest && responseObject instanceof HttpServletResponse) {
            HttpServletResponse response = (HttpServletResponse)responseObject;
            List<Span> spanStack = asyncContext.tvGetSpanStack();
            if (spanStack != null) { //the original dispatch has exited, which means this "complete" call will have effect {@link AsyncContext#complete}
                for (int i = spanStack.size() - 1; i >= 0 ; i --) { //finishes the "top" of the stack first (which is the last element added to the list
                    Span span = spanStack.get(i);
                    span.setTag("Status", response.tlysGetStatus());

                    span.finish();
                }


                asyncContext.tvSetSpanStack(null); //should remove it from async context as it has been activated (restored)
                Context.clearMetadata();
                response.tlysSetXTraceID(null); // ID is no longer valid once event has been sent.
            }
        } else {
        	logger.warn("Cannot create exit event from async mode as the request and response are not expected types");
        }
    }
            
     
    
    private enum OpType {
        COMPLETE
    }
}
