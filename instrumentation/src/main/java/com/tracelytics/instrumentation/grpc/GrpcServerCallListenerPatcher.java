package com.tracelytics.instrumentation.grpc;

import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.ContextPropagationPatcher;
import com.tracelytics.instrumentation.MethodMatcher;
import com.tracelytics.instrumentation.TvContextObjectAware;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.Metadata;

/**
 * Patches `io.grpc.ServerCall$Listener`. For all the `onXXX` method, the context created by the "grpc-server" span is restored.
 * 
 * This is necessary as most of the "actual" work done on the server will be enclosed by this call. 
 * 
 * With propagated context, all those operations will as well be captured automatically
 * 
 * 
 * @author Patson
 *
 */
public class GrpcServerCallListenerPatcher extends ClassInstrumentation {
    private static final String CLASS_NAME = GrpcServerCallListenerPatcher.class.getName();
    
 // List of method matchers that declare method with signature patterns that we want to instrument
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<OpType>> methodMatchers = Arrays
            .asList(new MethodMatcher<OpType>("onMessage", new String[] {} , "void", OpType.ON_MESSAGE),
                    new MethodMatcher<OpType>("onHalfClose", new String[] {} , "void", OpType.OTHER),
                    new MethodMatcher<OpType>("onCancel", new String[] {} , "void", OpType.OTHER),
                    new MethodMatcher<OpType>("onComplete", new String[] {} , "void", OpType.OTHER),
                    new MethodMatcher<OpType>("onReady", new String[] {} , "void", OpType.OTHER));
    
    private enum OpType { ON_MESSAGE, OTHER };
    
    
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes) throws Exception {
        for (Entry<CtMethod, OpType> methodEntry : findMatchingMethods(cc, methodMatchers).entrySet()) {
            CtMethod method = methodEntry.getKey();
            insertBefore(method, CLASS_NAME + ".preOnEvent(this);", false);
            insertAfter(method, CLASS_NAME + ".postOnEvent(this);", true, false);
        }
        
        addTvContextObjectAware(cc);
        return true;
    }
    
    public static void preOnEvent(Object listenerObject) {
        //Do not use ContextPropagationPatcher as this is a special case that
        //1. for UnaryListener, even if everything happens on same thread, we still want a fork
        //2. We want the listenerObject be able to restore context on various methods repeatedly, not just once as in ContextPropagationPatcher
        TvContextObjectAware listener = (TvContextObjectAware) listenerObject;
        //keep a reference of existing context value before setting it. Upon completion of this run method, we should reset it back to this existing context value
        Metadata currentContext = Context.getMetadata(); 
        listener.setTvPreviousContext(currentContext.isValid() ? currentContext : null);
        
        Metadata restoringContext = listener.getTvContext(); //get the context captured previously and try to restore it here
        
        if (restoringContext != null && restoringContext.isValid()) {
            Metadata clone = new Metadata(restoringContext); //use a clone, do not use the object directly as we want a fork
            clone.setIsAsync(true); //always consider onXXX handling as asynchronous as the onXXX method might call onCompleted that close the server span while the onXXX handling itself is not yet closed  
            Context.setMetadata(clone);
            
            listener.setTvRestored(true); //flag that this context as restored that it's overwritten an existing context; such that upon exit of the run method, it should reset back to the existing context 
        }
    }
    
    public static void postOnEvent(Object listenerObject) {
        ContextPropagationPatcher.resetContext(listenerObject);
    }
    
    
}