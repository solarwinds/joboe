package com.appoptics.apploader.instrumenter.mq.rabbitmq;

import com.appoptics.instrumentation.mq.rabbitmq.RabbitMqConsumer;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Consumer;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.Module;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.Event;
import com.tracelytics.joboe.XTraceHeader;
import com.tracelytics.joboe.span.impl.Span;
import com.tracelytics.joboe.span.impl.Tracer;
import com.tracelytics.logging.Logger;
import com.tracelytics.logging.LoggerFactory;
import com.tracelytics.util.TimeUtils;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;


public class RabbitMqChannelInstrumenter {
    private static final Logger logger = LoggerFactory.getLogger();

    private static ThreadLocal<Integer> getDepthThreadLocal = new ThreadLocal<Integer>() {
        protected Integer initialValue() {
            return 0;
        }
    };

    private static ThreadLocal<Long> getStartTimeThreadLocal = new ThreadLocal<Long>();

    private static final String SPAN_PREFIX = "rabbit-mq-";


    public static AMQP.BasicProperties beforePublish(Channel channel, String exchange, String routingKey, AMQP.BasicProperties basicProperties, byte[] body) {
        if (Context.isValid()) {
            if (Context.getMetadata().isSampled()) {
                Event event = Context.createEvent();
                event.addInfo("Label", "entry",
                        "Layer", SPAN_PREFIX + "publish",
                        "Spec", "producer",
                        "Flavor", "amqp");

                if (exchange != null) {
                    event.addInfo("ExchangeName", exchange);
                }

                if (routingKey != null) {
                    event.addInfo("RoutingKey", routingKey);
                }

                if (channel.getConnection() != null) {
                    InetAddress channelHost = channel.getConnection().getAddress();
                    int channelPort = channel.getConnection().getPort();
                    if (channelHost != null) {
                        event.addInfo("RemoteHost", channelHost.getHostAddress() + ":" + channelPort);
                    }
                }

                if (body != null) {
                    event.addInfo("MessageLength", body.length);
                }

                ClassInstrumentation.addBackTrace(event, 1, Module.RABBIT_MQ);

                event.report();
            }

            //always inject header when context is valid even if not sampled
            Map<String, Object> newHeaders;
            if (basicProperties == null) {
                basicProperties =  new AMQP.BasicProperties.Builder().build();
            }
            if (basicProperties.getHeaders() == null) {
                newHeaders = new HashMap<String, Object>();
            } else {
                newHeaders = new HashMap<String, Object>(basicProperties.getHeaders()); //make it mutable
            }

            newHeaders.put(ClassInstrumentation.XTRACE_HEADER, Context.getMetadata().toHexString());
            return basicProperties.builder().headers(newHeaders).build();
        } else {
            return basicProperties;
        }
    }


    public static void afterPublish() {
        Event event = Context.createEvent();
        event.addInfo("Label", "exit",
                "Layer", SPAN_PREFIX + "publish");

        event.report();

    }

    public static void afterGet(Channel channel, String queue, com.rabbitmq.client.GetResponse response) {
        if (shouldEndGetExtent()) {
            if (response != null && response.getBody() != null) {
                final Map<String, Object> headers = response.getProps().getHeaders();
                int messageLenth = response.getBody().length;
                String routingKey = response.getEnvelope().getRoutingKey();
                String exchange = response.getEnvelope().getExchange();

                InetAddress channelHost = null;
                int channelPort = -1;
                if (channel.getConnection() != null) {
                    channelHost = channel.getConnection().getAddress();
                    channelPort = channel.getConnection().getPort();
                }

                Map<XTraceHeader, String> xTraceHeaders = ClassInstrumentation.extractXTraceHeaders(new ClassInstrumentation.HeaderExtractor<String, String>() {
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

                Tracer.SpanBuilder builder = ClassInstrumentation.getStartTraceSpanBuilder(SPAN_PREFIX + "basic-get", xTraceHeaders, exchange, true);
                builder.withTag("Spec", "Consumer");
                builder.withTag("Flavor", "amqp");

                if (exchange != null) {
                    builder.withTag("ExchangeName", exchange);
                }

                if (queue != null) {
                    builder.withTag("Queue", queue);
                }

                if (routingKey != null) {
                    builder.withTag("RoutingKey", routingKey);
                }

                if (xTraceId != null) {
                    builder.withTag("SourceTrace", xTraceId); //use SourceTrace KV to keep reference to the original trace
                }

                builder.withTag("MessageLength", messageLenth);

                if (channelHost != null) {
                    builder.withTag("RemoteHost", channelHost.getHostAddress() + ":" + channelPort);
                }

                builder.withStartTimestamp(getStartTimeThreadLocal.get());
                getStartTimeThreadLocal.remove();

                Span span = builder.start();

                if (exchange != null && !"".equals(exchange)) {
                    span.setTracePropertyValue(Span.TraceProperty.TRANSACTION_NAME, "amqp.basic-get." + exchange);
                } else {
                    span.setTracePropertyValue(Span.TraceProperty.TRANSACTION_NAME, "amqp.basic-get");
                }

                ClassInstrumentation.addBackTrace(span, 1, Module.RABBIT_MQ);

                span.finish();
            }
        }
    }

    /**
     * Record only the start time here as we might want the incoming x-trace ID to have distributed traces (in the future)
     */
    public static void beforeGet() {
        if (shouldStartGetExtent()) {
            getStartTimeThreadLocal.set(TimeUtils.getTimestampMicroSeconds());
        }
    }

    public static void beforeConsume(Consumer consumer, String queue, Channel channel) {
        if (consumer instanceof RabbitMqConsumer) {
            ((RabbitMqConsumer) consumer).tvSetQueue(queue);

            if (channel != null && channel.getConnection() != null) {
                String channelHost = channel.getConnection().getAddress().getHostAddress() + ":" + channel.getConnection().getPort();
                ((RabbitMqConsumer) consumer).tvSetChannelHost(channelHost);
            }
        }
    }


    /**
     * Checks whether the current method call should start a new extent. If there is already an active extent, then do not start one
     * @return
     */
    private static boolean shouldStartExtent(ThreadLocal<Integer> threadLocal) {
        int currentDepth = threadLocal.get();
        threadLocal.set(currentDepth + 1);

        if (currentDepth == 0) {
            return true;
        } else {
            return false;
        }
    }



    protected static boolean shouldEndGetExtent() {
        return shouldEndExtent(getDepthThreadLocal);
    }

    /**
     * Checks whether the current method call is the active extent. If it is, then creates an exit event to end it
     * @return
     */
    protected static boolean shouldEndExtent(ThreadLocal<Integer> threadLocal) {
        int currentDepth = threadLocal.get();
        threadLocal.set(currentDepth - 1);

        if (currentDepth == 1) {
            return true;
        } else {
            return false;
        }
    }


    protected static boolean shouldStartGetExtent() {
        return shouldStartExtent(getDepthThreadLocal);
    }


}
