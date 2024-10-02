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
import org.apache.kafka.streams.processor.internals.StampedRecord;

import java.util.Map;

/**
 * Creates entry and exit for Kafka Streams span on `org.apache.kafka.streams.processor.internals.StreamTask#process`.
 *
 * Logically it makes sense to create entry and exit event on the beginning and end of such a method which processes
 * one Kafka message from the stream. Unfortunately, the message itself is unavailable on the entry point of such a method.
 *
 * The message is required if we want to "continue" a trace by reading the x-trace ID, which we do NOT allow due to limitation
 * of collector (which it might drop span start events if they are reported after exiting the root span).
 *
 * However, we might want to create a complete distributed traces in the future, therefore we delay the span entry
 * a little bit to when the message is retrieved from the `PartitionGroup#nextRecord` method.
 *
 * We use threadlocal to keep track of the method scope state to ensure the call to `PartitionGroup#nextRecord` is indeed
 * within the expected method scope before creating the Kafka Streams span/scope.
 */
public class KafkaStreamTaskInstrumenter {
    private static final String SPAN_NAME = "kafka-stream";
    private static final Logger logger = LoggerFactory.getLogger();
    private static final boolean CONTEXT_PROPAGATION_ENABLED = ConfigManager.getConfigOptional(ConfigProperty.AGENT_KAFKA_PROPAGATION, KafkaConstants.DEFAULT_CONTEXT_PROPAGATION_ENABLED);

    private static final ThreadLocal<Boolean> isProcessing = new ThreadLocal<Boolean>();
    private static final ThreadLocal<Scope> processingScope = new ThreadLocal<Scope>();

    public static void beforeProcess() {
        isProcessing.set(true);
    }

    public static void afterProcess() {
        if (processingScope.get() != null) {
            processingScope.get().close();
            processingScope.remove();
        }

        isProcessing.remove();
    }

    /**
     * When `org.apache.kafka.streams.processor.internals.PartitionGroup#nextRecord` returns, we mark this as the start
     * of the Streams processing (span entry)
     * @param record
     */
    public static void onNextRecord(final StampedRecord record) {
        if (isProcessing() && processingScope.get() == null && record != null) { //ensure `nextRecord` is called within `StreamTask#process` and that no scope has been created yet
            Map<XTraceHeader, String> xTraceHeaders = ClassInstrumentation.extractXTraceHeaders(new ClassInstrumentation.HeaderExtractor<String, String>() {
                @Override
                public String extract(String key) {
                    Header header = record.value.headers().lastHeader(key);
                    return header != null ? new String(header.value()) : null;
                }
            });

            String xTraceId = null;
            if (CONTEXT_PROPAGATION_ENABLED && xTraceHeaders.containsKey(XTraceHeader.TRACE_ID)) {
                xTraceId = xTraceHeaders.remove(XTraceHeader.TRACE_ID); //do not do distributed traces as data backend might drop it
            }

            Context.clearMetadata(); //always clear context as this should be considered as an entry point

            String topic = record.topic();
            Tracer.SpanBuilder builder = ClassInstrumentation.getStartTraceSpanBuilder(SPAN_NAME, xTraceHeaders, topic, true);
            Integer partition = record.partition();
            long offset = record.offset();
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

            scope.span().setTracePropertyValue(Span.TraceProperty.TRANSACTION_NAME, "kafka.stream." + topic);

            processingScope.set(scope);
        }
    }

    private static boolean isProcessing() {
        return isProcessing.get() != null ? isProcessing.get() : false;
    }
}