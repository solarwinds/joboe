package com.tracelytics.instrumentation.http.ws.server;

import java.lang.reflect.Method;

import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.span.impl.ScopeManager;
import com.tracelytics.joboe.span.impl.Span;
import com.tracelytics.joboe.span.impl.Span.TraceProperty;

public abstract class BaseRestServerInstrumentation extends ClassInstrumentation {
    public static void reportTransactionName(String transactionName) {
        if (transactionName != null && Context.isValid()) {
            Span currentSpan = ScopeManager.INSTANCE.activeSpan();
            if (currentSpan != null) {
                currentSpan.setTracePropertyValue(TraceProperty.TRANSACTION_NAME, transactionName);
            }
        }
    }
    
    public static void reportControllerAction(Method resourceMethod) {
        if (resourceMethod != null && Context.isValid()) {
            Span currentSpan = ScopeManager.INSTANCE.activeSpan();
            if (currentSpan != null) {
                String controller = resourceMethod.getDeclaringClass() != null ? resourceMethod.getDeclaringClass().getName() : null;
                String action = resourceMethod.getName();
                
                if (controller != null) {
                    currentSpan.setTag("Controller", controller);   
                    currentSpan.setTracePropertyValue(TraceProperty.CONTROLLER, controller);
                }
                if (action != null) {
                    currentSpan.setTag("Action", action);
                    currentSpan.setTracePropertyValue(TraceProperty.ACTION, action);
                }
            }
        }
    }

    public static void reportInvokedMethod(Method resourceMethod) {
        if (resourceMethod != null && Context.isValid()) {
            Span currentSpan = ScopeManager.INSTANCE.activeSpan();
            if (currentSpan != null) {
                String declaringClass = resourceMethod.getDeclaringClass() != null ? resourceMethod.getDeclaringClass().getName() : null;
                String methodName = resourceMethod.getName();
                currentSpan.setTag("InvokedMethod", declaringClass != null ? declaringClass + "." + methodName : methodName);
            }
        }
    }
    
    
}
