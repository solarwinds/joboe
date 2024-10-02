package com.tracelytics.instrumentation.jms;

import com.tracelytics.ext.javassist.*;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.MethodMatcher;
import com.tracelytics.instrumentation.Module;
import com.tracelytics.joboe.span.impl.Scope;
import com.tracelytics.joboe.span.impl.ScopeManager;
import com.tracelytics.joboe.span.impl.Span;

import java.util.*;

public class MessageProducerInstrumentation extends ClassInstrumentation {
    private static final String CLASS_NAME = MessageProducerInstrumentation.class.getName();

    private static ThreadLocal<Integer> callDepth = new ThreadLocal<Integer>() {
        protected Integer initialValue() {
            return 0;
        }
    };

    private static final List<MethodMatcher<Object>> methodMatchers = new ArrayList<MethodMatcher<Object>>();
    enum Type {SEND, SEND_WITH_DEST}
    static {
        methodMatchers.add(new MethodMatcher<Object>(
                "send",
                new String[]{"javax.jms.Message"},
                "void", Type.SEND, false));
        methodMatchers.add(new MethodMatcher<Object>(
                "send",
                new String[]{"javax.jms.Destination", "javax.jms.Message"},
                "void", Type.SEND_WITH_DEST, false));
    }
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
            throws Exception {
        Map<CtMethod, Object> methodsMap = findMatchingMethods(cc, methodMatchers);

        for (Map.Entry<CtMethod, Object> sendMethod : methodsMap.entrySet()) {
            Type type = (Type)sendMethod.getValue();
            String offset;
            if (type == Type.SEND) {
                offset = "1";
            } else {
                offset = "2";
            }
            modifySend(className, sendMethod.getKey(), offset);
        }
        return true;
    }
    private void modifySend(String className, CtMethod method, String offset)
            throws CannotCompileException {
        String methodName = method.getName();
        String vendorName = JmsUtils.getVendorName(className);
        if (vendorName == null) {
            vendorName = "";
        }
        String patchCode = "String queueName = null;" +
                        "javax.jms.Destination dest = $0.getDestination();" +
                        "if (dest == null && $1 instanceof javax.jms.Destination) {" +
                            "dest = $1;" +
                        "}" +
                        "if (dest instanceof javax.jms.Queue) {" +
                        "    queueName = ((javax.jms.Queue)dest).getQueueName();" +
                        "}" +
                        "String topicName = null;" +
                        "if (dest instanceof javax.jms.Topic) {" +
                            "topicName = ((javax.jms.Topic)dest).getTopicName();" +
                        "}" +
                        "String xid = " + CLASS_NAME + ".layerEntry(\"" + vendorName + "\", queueName, topicName);" +
                        "if (xid != null) {" +
                        "$" + offset +".setStringProperty(\"XTraceId\", xid);" +
                        "}";
        insertBefore(method, patchCode);
        insertAfter(method, CLASS_NAME + ".layerExit();", true);
    }

    public static String layerEntry(String vendorName, String queueName, String topicName) {
        int depth = callDepth.get();
        callDepth.set(depth+1);
        if (depth > 0) {
            return null;
        }

        String layerName = "jms.producer" + (vendorName.equals("") ? "" : "." + vendorName);

        Span span = buildTraceEventSpan(layerName)
                .asChildOf(ScopeManager.INSTANCE.activeSpan())
                .startActive()
                .span();
        ClassInstrumentation.addBackTrace(span, 1, Module.JMS);
        if (queueName != null) {
            span.setTag("Queue", queueName);
        }
        if (topicName != null) {
            span.setTag("Topic", topicName);
        }

        span.setTag("Spec", "producer");
        span.setTag("Flavor", "jms");
        return span.context().getMetadata().toHexString();
    }

    public static void layerExit() {
        int depth = callDepth.get();
        callDepth.set(depth-1);
        if (depth > 1) {
            return;
        }

        Scope scope = scopeManager.active();
        if (scope != null) {
            scope.close();
        }
    }
}

