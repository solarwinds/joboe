package com.tracelytics.instrumentation.cache.spymemcached;

import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import com.tracelytics.ext.javassist.CannotCompileException;
import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.ext.javassist.NotFoundException;
import com.tracelytics.ext.javassist.expr.ExprEditor;
import com.tracelytics.ext.javassist.expr.MethodCall;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.MethodMatcher;
import com.tracelytics.instrumentation.TvContextObjectAware;
import com.tracelytics.joboe.Metadata;

/**
 * Usually we set the context to the callback when the corresponding Operation is first submitted as async layer entry. The context set to the callback will then 
 * be used to provide linkage back to the async entry for the layer exit event when complete() of the callback is called (and trace result size when gotData() is invoked).
 * 
 * However, this does not always work, for multi-get operations (getBulk), multiple Operation instances are created but the all share the same callback instance. 
 * This results as the context value kept in the callback being overridden by different Operation instances which trigger problems for the async exit events as linkage
 * back to the original async entry event for related Operation can no longer be found. 
 * 
 * In order to tackle this, we inject calls to set(refresh) context on the Callback before the Callback.complete()/Callback.gotData() is invoked. 
 * This approach should work as spy memcached handles IO using a single thread pattern, therefore the actual Callback.complete()/Callback.gotData() should have the 
 * correct refreshed context for event creation.  
 *  
 * @author Patson Luk
 *
 */
public class SpyMemcachedOperationInstrumentation extends ClassInstrumentation {
    private static final String CLASS_NAME = SpyMemcachedOperationInstrumentation.class.getName();
    
    // List of method matchers that declare method with signature patterns that we want to instrument
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<OpType>> methodMatchers = Arrays.asList(
         new MethodMatcher<OpType>("cancel", new String[] { }, "void", OpType.CANCEL), 
         new MethodMatcher<OpType>("timeOut", new String[] { }, "void", OpType.TIMEOUT)
    );
            
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
            throws Exception {
        addTvContextObjectAware(cc);
        
        cc.instrument(new ExprEditor() {
            public void edit(MethodCall m) throws CannotCompileException {
                if (m.getMethodName().equals("complete") || m.getMethodName().equals("gotData")) {
                    //refresh the context before calling complete or gotData since mulitple instances of Operation (hence different context) can share the same Callback
                    insertBeforeMethodCall(m, "if ($0 instanceof com.tracelytics.instrumentation.TvContextObjectAware && getTvContext() != null) { ((com.tracelytics.instrumentation.TvContextObjectAware)$0).setTvContext(getTvContext()); } ");
                } 
            }
        });
        

        boolean hasGetCallbackMethod;
        try {
            cc.getMethod("getCallback", "()Lnet/spy/memcached/ops/OperationCallback;");
            hasGetCallbackMethod = true;
        } catch (NotFoundException e) {
            logger.warn("SpyMemcached does not provide getCallback() method, some of the edge cases might not have correct status reported");
            hasGetCallbackMethod = false;
        }

        //instrument cancel and timeout to trace the status, since some older version (2.7.3 and before) does not have consistent flow of calling CallBack.receivedStatus() as it supposed to be
        for (Entry<CtMethod,OpType> entry : findMatchingMethods(cc, methodMatchers).entrySet()) {
            if (hasGetCallbackMethod) {
                if (entry.getValue() == OpType.CANCEL) {
                    insertBefore(entry.getKey(), CLASS_NAME + ".traceStatus(getCallback(), \"cancelled\");");
                } else if (entry.getValue() == OpType.TIMEOUT) {
                    insertBefore(entry.getKey(), CLASS_NAME + ".traceStatus(getCallback(), \"time out\");");
                }
            }
            
        }
        
        
        return true;
    }
    
    public static void setTvContext(Object callback, Metadata asyncEntryEventMetadata) {
        if (callback instanceof TvContextObjectAware) {
            if (asyncEntryEventMetadata != null) {
                ((com.tracelytics.instrumentation.TvContextObjectAware)callback).setTvContext(asyncEntryEventMetadata);
            }
        } else {
            if (callback != null) {
                logger.warn("Expect callback to be instance of [" + TvContextObjectAware.class.getName() + "] but it is not. The class is [" + callback.getClass().getName() + "]");
            }
        }
    }
    
    public static void traceStatus(Object callbackObj, String status) {
        if (callbackObj instanceof SpyMemcachedCallback) {
            ((SpyMemcachedCallback)callbackObj).setTvStatusString(status);
        }
    }
    
    private enum OpType {
        CANCEL, TIMEOUT;
    }
}