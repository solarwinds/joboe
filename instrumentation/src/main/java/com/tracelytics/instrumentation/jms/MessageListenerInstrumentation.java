package com.tracelytics.instrumentation.jms;

import com.tracelytics.ext.javassist.CannotCompileException;
import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.ext.javassist.NotFoundException;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.MethodMatcher;
import com.tracelytics.instrumentation.Module;
import com.tracelytics.joboe.XTraceHeader;
import com.tracelytics.joboe.span.impl.Scope;
import com.tracelytics.joboe.span.impl.Span;

import java.util.*;


/**
 * The instrumentation for JMS' MessageListener interface, which is used to asynchronously process message
 * consumed by MessageConsumer (2 separate traces created - one for message consumption from the queue and another one
 * for listener processing)
 */
public abstract class MessageListenerInstrumentation extends ClassInstrumentation {
    private static final String CLASS_NAME = MessageListenerInstrumentation.class.getName();
    protected abstract String getPackagePrefix();
    protected abstract List<MethodMatcher<Object>> getMethodMatchers();

    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
            throws Exception {
        Set<CtMethod> onMessageMethods = findMatchingMethods(cc, getMethodMatchers()).keySet();

        for (CtMethod onMessageMethod : onMessageMethods) {
            modifyOnMessage(className, onMessageMethod);
        }
        return true;
    }


    private void modifyOnMessage(String className, CtMethod method)
            throws CannotCompileException, NotFoundException {
        String methodName = method.getName();
        String vendorName = JmsUtils.getVendorName(className);
        if (vendorName == null) {
            vendorName = "";
        }
        insertBefore(method,
                "if ($1 != null) { " +
                        "String queueName = null;" +
                        getPackagePrefix() + ".jms.Destination dest = $1.getJMSDestination();" +
                        "if (dest instanceof " + getPackagePrefix() + ".jms.Queue) {" +
                        "queueName = ((" + getPackagePrefix() + ".jms.Queue)dest).getQueueName();" +
                        "}" +
                        "String topicName = null;" +
                        "if (dest instanceof " + getPackagePrefix() + ".jms.Topic) {" +
                        "topicName = ((" + getPackagePrefix() + ".jms.Topic)dest).getTopicName();" +
                        "}" +
                        CLASS_NAME + ".layerEntry(\"" + vendorName + "\", \"" + className + "\", \"" + methodName + "\", $1.getStringProperty(\"XTraceId\"), queueName, topicName);"+
                        "}",
                false);
        addErrorReporting(method, Throwable.class.getName(), null, classPool, true);
        insertAfter(method, CLASS_NAME + ".layerExit();", true, false);
    }

    public static void layerEntry(String vendorName, String className, String methodName, String xTraceId, String queueName, String topicName) {
        Map<XTraceHeader, String> headers = new HashMap<XTraceHeader, String>();
//        if (xTraceId != null) {
//            headers.put(XTraceHeader.TRACE_ID, xTraceId);
//        }
        String layerName = "jms.onMessage" + (vendorName.equals("") ? "" : "." + vendorName);
        Scope scope = startTraceAsScope(layerName, headers, queueName, new HashMap<String, Object>(), true);
        Span span = scope.span();

        ClassInstrumentation.addBackTrace(span, 1, Module.JMS);

        String transactionName = "jms.onMessage";
        if (queueName != null) {
            transactionName = transactionName + "." + queueName;
            span.setTag("Queue", queueName);
        }
        if (topicName != null) {
            span.setTag("Topic", topicName);
        }

        span.setTracePropertyValue(Span.TraceProperty.TRANSACTION_NAME, transactionName);
        span.setTracePropertyValue(Span.TraceProperty.CONTROLLER, className);
        span.setTracePropertyValue(Span.TraceProperty.ACTION, methodName);
        if (xTraceId != null) {
            span.setTag("SourceTrace", xTraceId);
        }
        span.setTag("Spec", "consumer");
    }

    public static void layerExit() {
        Scope scope = scopeManager.active();
        if (scope != null) {
            scope.close();
        }
    }
}

