package com.appoptics.apploader.instrumenter.mq.kafka;

import com.appoptics.instrumentation.mq.kafka.KafkaConstants;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.Module;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.XTraceHeader;
import com.tracelytics.joboe.config.ConfigManager;
import com.tracelytics.joboe.config.ConfigProperty;
import com.tracelytics.joboe.span.impl.Scope;
import com.tracelytics.joboe.span.impl.Span;
import com.tracelytics.joboe.span.impl.Tracer;
import com.tracelytics.logging.Logger;
import com.tracelytics.logging.LoggerFactory;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;

import java.util.Map;

/**
 * Creates entry and exit for Kafka consumer span on `org.apache.kafka.common.utils.AbstractIterator#hasNext` and
 * `org.apache.kafka.common.utils.AbstractIterator#next`
 *
 * This is based on the assumption of common iterator usage pattern, which first invokes `hasNext` then invoke `next`
 * with the iteration ends with a trailing `hasNext` which returns false :
 * <code>
 *      ConsumerRecords messages = consumer.poll(1000);
 *      for (ConsumerRecord<String, String> message : messages) {
 *        //message processing
 *      }
 * </code>
 *
 * Take note that we wrap the original callback with our own callback so we get notified on the completion of the operation
 */
public class KafkaConsumerIteratorInstrumenter {
    private static final String SPAN_NAME = "kafka-consumer";
    private static final Logger logger = LoggerFactory.getLogger();
    private static final boolean CONTEXT_PROPAGATION_ENABLED = ConfigManager.getConfigOptional(ConfigProperty.AGENT_KAFKA_PROPAGATION, KafkaConstants.DEFAULT_CONTEXT_PROPAGATION_ENABLED);

    public static void onHasNext(Scope previousScope) {
        if (previousScope != null) {
            previousScope.close();

            Context.clearMetadata();
        }
    }

    public static Scope onNext(Object obj) {
        if (obj instanceof ConsumerRecord) {
            final ConsumerRecord<?, ?> consumerRecord = (ConsumerRecord<?, ?>) obj;
            Map<XTraceHeader, String> xTraceHeaders = ClassInstrumentation.extractXTraceHeaders(new ClassInstrumentation.HeaderExtractor<String, String>() {
                @Override
                public String extract(String key) {
                    Header header = consumerRecord.headers().lastHeader(key);
                    return header != null ? new String(header.value()) : null;
                }
            });

            String xTraceId = null;
            if (CONTEXT_PROPAGATION_ENABLED && xTraceHeaders.containsKey(XTraceHeader.TRACE_ID)) {
                 xTraceId = xTraceHeaders.remove(XTraceHeader.TRACE_ID); //do not do distributed traces as data backend might drop it
            }

            Context.clearMetadata(); //always clear contxt as this should be considered as an entry point

            String topic = consumerRecord.topic();
            Tracer.SpanBuilder builder = ClassInstrumentation.getStartTraceSpanBuilder(SPAN_NAME, xTraceHeaders, topic, true);
            Integer partition = consumerRecord.partition();
            long offset = consumerRecord.offset();
            builder.withTag("Spec", "consumer");
            builder.withTag("Flavor", "kafka");
            if (topic != null) {
                builder.withTag("Topic", topic);
            }
            if (partition != null) {
                builder.withTag("Partition", partition);
            }
            builder.withTag("Offset", offset);

            if (xTraceId != null) {
                builder.withTag("SourceTrace", xTraceId); //use SourceTrace KV to keep reference to the original trace
            }

            Scope scope = builder.startActive(true);

            ClassInstrumentation.addBackTrace(scope.span(), 1, Module.KAFKA);

            scope.span().setTracePropertyValue(Span.TraceProperty.TRANSACTION_NAME, "kafka.consume." + topic);

            return scope;
        }
        return null;
    }


}