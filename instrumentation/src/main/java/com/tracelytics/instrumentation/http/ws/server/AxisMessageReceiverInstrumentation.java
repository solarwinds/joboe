package com.tracelytics.instrumentation.http.ws.server;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.MethodMatcher;
import com.tracelytics.joboe.span.impl.ScopeManager;
import com.tracelytics.joboe.span.impl.Span;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Instruments `org.apache.axis2.receivers.AbstractMessageReceiver` to extract controller and action KVs from call to `receive`
 *
 * Controller - name of `AxisService` from the context
 * Action - the local part of the name of `AxisOperation` from the context
 */
public class AxisMessageReceiverInstrumentation extends ClassInstrumentation {
    private static final String CLASS_NAME = AxisMessageReceiverInstrumentation.class.getName();
    // List of method matchers that declare method with signature patterns that we want to instrument
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<OpType>> methodMatchers = Arrays.asList(
            new MethodMatcher<OpType>("receive", new String[] { "org.apache.axis2.context.MessageContext" }, "void", OpType.RECEIVE, true)
    );

    private enum OpType {
        RECEIVE
    }

    @Override
    protected boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes) throws Exception {
        boolean modified = false;
        for (Map.Entry<CtMethod, OpType> entry : findMatchingMethods(cc, methodMatchers).entrySet()) {
            CtMethod method = entry.getKey();
            OpType type = entry.getValue();
            if (type == OpType.RECEIVE) {
                insertAfter(method,
                        "String controller = null;" +
                                "String action = null;" +
                                "if ($1 != null && $1.getAxisService() != null) {" +
                                "   controller = $1.getAxisService().getName();" +
                                "}" +
                                "if ($1 != null && $1.getAxisOperation() != null && $1.getAxisOperation().getName() != null) {" +
                                "   action = $1.getAxisOperation().getName().getLocalPart();" +
                                "}" +
                                CLASS_NAME + ".recordControllerAction(controller, action);"
                        , true, false);
                modified = true;
            }
        }
        return modified;
    }

    public static void recordControllerAction(String controller, String action) {
        Span currentSpan = ScopeManager.INSTANCE.activeSpan();
        if (currentSpan != null) {
            if (action != null) {
                currentSpan.setTag("Action", action);
                currentSpan.setTracePropertyValue(Span.TraceProperty.ACTION, action);
            }

            if (controller != null) {
                currentSpan.setTag("Controller", controller);
                currentSpan.setTracePropertyValue(Span.TraceProperty.CONTROLLER, controller);
            }

        }
    }
}
