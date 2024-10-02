package com.appoptics.instrumentation.mq.rabbitmq;

import com.google.auto.service.AutoService;
import com.tracelytics.ext.javassist.*;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.Instrument;
import com.tracelytics.instrumentation.MethodMatcher;
import com.tracelytics.instrumentation.Module;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.XTraceHeader;
import com.tracelytics.joboe.span.impl.Scope;
import com.tracelytics.joboe.span.impl.ScopeManager;
import com.tracelytics.joboe.span.impl.Span;
import com.tracelytics.joboe.span.impl.Tracer;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Instruments RabbitMQ's channel for "publish" operations
 * @author pluk
 *
 */

@AutoService(ClassInstrumentation.class)
@Instrument(targetType = "com.rabbitmq.client.Consumer", module = Module.RABBIT_MQ, appLoaderPackage = "com.appoptics.apploader.instrumenter.mq.rabbitmq")
public class RabbitMqConsumerInstrumentation extends ClassInstrumentation {
    private static final String CLASS_NAME = RabbitMqConsumerInstrumentation.class.getName();
    private static final String SPAN_NAME = "rabbit-mq-consumer";
    
    private enum OpType { HANDLE_DELIVERY }
            
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<OpType>> methodMatchers = Arrays.asList(
        new MethodMatcher<OpType>("handleDelivery", new String[]{ "java.lang.String", "com.rabbitmq.client.Envelope", "com.rabbitmq.client.AMQP$BasicProperties", "byte[]"}, "void", OpType.HANDLE_DELIVERY)
    );
    

    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
            throws Exception {
        addFieldsAndMethods(cc);

        for (Entry<CtMethod, OpType> matchingMethodEntry : findMatchingMethods(cc, methodMatchers).entrySet()) {
            OpType type = matchingMethodEntry.getValue();
            CtMethod method = matchingMethodEntry.getKey();
            if (type == OpType.HANDLE_DELIVERY) {
                insertBefore(method,
                        "if (tvGetQueue() != null) {" +
                                    CLASS_NAME + ".beforeHandleDelivery(" +
                                "   $1," +
                                "   $4," +
                                "   $3 != null ? $3.getHeaders() : null, " +
                                "   $2 != null ? $2.getRoutingKey() : null, " +
                                "   $2 != null ? $2.getExchange() : null, " +
                                "   $3 != null ? $3.getCorrelationId() : null, " +
                                "   $3 != null ? $3.getReplyTo() : null," +
                                "   tvGetQueue(), tvGetChannelHost());" +
                                "}"

                                , false);
                addErrorReporting(method, Throwable.class.getName(), SPAN_NAME, classPool, true);
                insertAfter(method, "if (tvGetQueue() != null) { " + CLASS_NAME + ".afterHandleDelivery(); }", true, false);
            }
        }
        
        return true;
    }

    private void addFieldsAndMethods(CtClass cc) throws CannotCompileException, NotFoundException {
        cc.addField(CtField.make("private String tvQueue;", cc));
        cc.addMethod(CtNewMethod.make("public String tvGetQueue() { return this.tvQueue; }", cc));
        cc.addMethod(CtNewMethod.make("public void tvSetQueue(String queue) { this.tvQueue = queue; }", cc));

        cc.addField(CtField.make("private String tvChannelHost;", cc));
        cc.addMethod(CtNewMethod.make("public String tvGetChannelHost() { return this.tvChannelHost; }", cc));
        cc.addMethod(CtNewMethod.make("public void tvSetChannelHost(String channelHost) { this.tvChannelHost = channelHost; }", cc));

        tagInterface(cc, RabbitMqConsumer.class.getName());
    }


    public static void beforeHandleDelivery(String consumerTag, byte[] body, final Map<String, Object> headers, String routingKey, String exchange, String correlationId, String replyTo, String queue, String channelHost) {
        Map<XTraceHeader, String> xTraceHeaders = extractXTraceHeaders(new HeaderExtractor<String, String>() {
            @Override
            public String extract(String key) {
                if (headers != null) {
                    Object value = headers.get(key);
                    return value != null ? value.toString() : null;
                } else {
                    return null;
                }
            }
        });

        //for now we do NOT want to continue trace if there's an incoming x-trace header
        //we would report the `SourceTrace` KV instead
        String xTraceId = xTraceHeaders.get(XTraceHeader.TRACE_ID);
        if (xTraceId != null) {
            xTraceHeaders.remove(XTraceHeader.TRACE_ID);
        }

        //For consumer, it should always be considered an entry point instead of a child span of the one that spins it up
        Context.clearMetadata();
        ScopeManager.INSTANCE.removeAllScopes();

        Tracer.SpanBuilder builder = ClassInstrumentation.getStartTraceSpanBuilder(SPAN_NAME, xTraceHeaders, exchange, true);
        builder.withTag("Spec", "Consumer");
        builder.withTag("Flavor", "amqp");

        if (exchange != null) {
            builder.withTag("ExchangeName", exchange);
        }

        if (routingKey != null) {
            builder.withTag("RoutingKey", routingKey);
        }

        if (correlationId != null) {
            builder.withTag("CorrelationId", correlationId);
        }

        if (replyTo != null) {
            builder.withTag("ReplyTo", replyTo);
        }

        if (xTraceId != null) {
            builder.withTag("SourceTrace", xTraceId); //use SourceTrace KV to keep reference to the original trace
        }

        if (queue != null) {
            builder.withTag("Queue", queue);
        }

        if (channelHost != null) {
            builder.withTag("RemoteHost", channelHost);
        }

        if (body != null) {
            builder.withTag("MessageLength", body.length);
        }

        Span span = builder.startActive(true).span();

        if (exchange != null && !"".equals(exchange)) {
            span.setTracePropertyValue(Span.TraceProperty.TRANSACTION_NAME, "amqp.consume." + exchange);
        } else {
            span.setTracePropertyValue(Span.TraceProperty.TRANSACTION_NAME, "amqp.consume");
        }

        ClassInstrumentation.addBackTrace(span, 1, Module.RABBIT_MQ);

    }

    public static void afterHandleDelivery() {
        Scope scope = ScopeManager.INSTANCE.active();
        if (scope != null && SPAN_NAME.equals(scope.span().getOperationName())) {
            scope.close();
        } else {
            if (scope == null) {
                logger.warn("Active scope is not found for rabbitmq handle delivery span exit");
            } else {
                logger.warn("Active scope [" + scope.span().getOperationName() + "] does not match with the expected [" + SPAN_NAME + "] operation name");
            }
        }
    }

}