package com.tracelytics.instrumentation.jms;

import com.tracelytics.ext.javassist.*;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.MethodMatcher;
import com.tracelytics.instrumentation.Module;
import com.tracelytics.joboe.XTraceHeader;
import com.tracelytics.joboe.span.impl.Scope;
import com.tracelytics.joboe.span.impl.Span;
import com.tracelytics.joboe.span.impl.Tracer;
import com.tracelytics.util.TimeUtils;

import java.util.*;


/**
 * The instrumentation class for JMS MessageConsumer. The MessageConsumer is the interface for synchronously
 * consuming the messages (while MessageListener is for asynchronous purpose). Listeners will then be notified
 * with the message (instrumented by MessageListenerInstrumentation as a separate trace)
 *
 *
 * Please Note that in this class the trace is started in the method `layerExit`. The method `layerEntry`
 * only records the start timestamp. The reason is that previously we want to extract the xTrace ID from the
 * producer side to build a distributed trace. This is not the case now as the SourceTrace KV is used instead,
 * - it doesn't hurt even we choose `SourceTrace` and I'd keep it for the ease of any future change.
 */
public class MessageConsumerInstrumentation extends ClassInstrumentation {
    private static final String CLASS_NAME = MessageConsumerInstrumentation.class.getName();
    private static final List<MethodMatcher<Object>> methodMatchers = new ArrayList<MethodMatcher<Object>>();
    enum Type { RECEIVE, RECEIVE_NOWAIT, DEQUEUE}
    static {
        methodMatchers.add(new MethodMatcher<Object>(
                "receive",
                new String[]{},
                "javax.jms.Message", Type.RECEIVE));
        methodMatchers.add(new MethodMatcher<Object>(
                "receiveNoWait",
                new String[]{},
                "javax.jms.Message", Type.RECEIVE_NOWAIT));
        methodMatchers.add(new MethodMatcher<Object>(
                "dequeue",
                new String[]{"long"},
                "org.apache.activemq.command.MessageDispatch", Type.DEQUEUE));
    }

    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
            throws Exception {
        Map<CtMethod, Object> receiveMethods = findMatchingMethods(cc, methodMatchers);
        cc.addField(CtField.make("private ThreadLocal tvSpanStartTimestamp = new ThreadLocal();", cc));
        cc.addMethod(CtNewMethod.make("public void tvSetSpanStartTimestamp(Long ts) { if (ts != null) { tvSpanStartTimestamp.set(ts);} else {tvSpanStartTimestamp.remove();} }", cc));
        cc.addMethod(CtNewMethod.make("public Long tvGetSpanStartTimestamp() { return (Long)tvSpanStartTimestamp.get(); }", cc));


        for (Map.Entry<CtMethod, Object> receiveMethod : receiveMethods.entrySet()) {
            if (receiveMethod.getValue() == Type.DEQUEUE) {
                modifyDequeue(className, receiveMethod.getKey());
            } else {
                modifyReceive(className, receiveMethod.getKey());
            }
        }
        return true;
    }
    private void modifyDequeue(String className, CtMethod method)
            throws CannotCompileException {
        insertAfter(method, "tvSetSpanStartTimestamp(" + CLASS_NAME + ".layerEntry());", true, false);
    }

    private void modifyReceive(String className, CtMethod method)
            throws CannotCompileException, NotFoundException {
        addErrorReporting(method, "javax.jms.JMSException", null, classPool);
        String methodName = method.getName();
        String vendorName = JmsUtils.getVendorName(className);
        if (vendorName == null) {
            vendorName = "";
        }
        insertAfter(method,
                "if ($_ != null) { " +
                    "String queueName = null;" +
                    "javax.jms.Destination dest = $_.getJMSDestination();" +
                    "if (dest instanceof javax.jms.Queue) {" +
                        "queueName = ((javax.jms.Queue)dest).getQueueName();" +
                    "}" +
                    "String topicName = null;" +
                    "if (dest instanceof javax.jms.Topic) {" +
                        "topicName = ((javax.jms.Topic)dest).getTopicName();" +
                    "}" +
                    CLASS_NAME + ".layerExit(\"" + vendorName + "\", \"" + className + "\", \"" + methodName + "\", tvGetSpanStartTimestamp(), $_.getStringProperty(\"XTraceId\"), queueName, topicName);"+
                "}" +
                "tvSetSpanStartTimestamp(null);",
                true, false);
    }

    public static Long layerEntry() {
        return TimeUtils.getTimestampMicroSeconds();
    }

    public static void layerExit(String vendorName, String className, String methodName, Long startTimestamp, String xTraceId, String queueName, String topicName) {
//        if (xTraceId == null) {
//            return;
//        }

        Map<XTraceHeader, String> headers = new HashMap<XTraceHeader, String>();
//        if (xTraceId != null) {
//            headers.put(XTraceHeader.TRACE_ID, xTraceId);
//        }

        String layerName = "jms.consumer" + (vendorName.equals("") ? "" : "." + vendorName);

        Tracer.SpanBuilder spanBuilder = getStartTraceSpanBuilder(layerName, headers, queueName, true);
        if (startTimestamp != null) {
            spanBuilder.withStartTimestamp(startTimestamp);
        }

        Scope scope = spanBuilder.startActive();
        Span span = scope.span();

        ClassInstrumentation.addBackTrace(span, 1, Module.JMS);

        String transactionName = "jms.consume";
        if (queueName != null) {
            transactionName = transactionName + "." + queueName;
            span.setTag("Queue", queueName);
        }
        if (topicName != null) {
            span.setTag("Topic", topicName);
        }

        if (span.isRoot()) {
            span.setTracePropertyValue(Span.TraceProperty.TRANSACTION_NAME, transactionName);
            span.setTracePropertyValue(Span.TraceProperty.CONTROLLER, className);
            span.setTracePropertyValue(Span.TraceProperty.ACTION, methodName);
        }
        if (xTraceId != null) {
        span.setTag("SourceTrace", xTraceId);
        }
        span.setTag("Spec", "consumer");

        scope.close();
    }
}
