package com.appoptics.apploader.instrumenter.mq.kafka;

import com.appoptics.instrumentation.mq.kafka.KafkaConstants;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.Module;
import com.tracelytics.joboe.config.ConfigManager;
import com.tracelytics.joboe.config.ConfigProperty;
import com.tracelytics.joboe.span.impl.Span;
import com.tracelytics.joboe.span.impl.TraceEventSpanReporter;
import com.tracelytics.joboe.span.impl.Tracer;
import com.tracelytics.logging.Logger;
import com.tracelytics.logging.LoggerFactory;
import org.apache.kafka.clients.ApiVersions;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.record.RecordBatch;

/**
 * Creates entry and exit for Kafka producer span on `org.apache.kafka.clients.producer.KafkaProducer#send` and `onCompletion`
 * for the return callback of the `send` method
 *
 * Take note that we wrap the original callback with our own callback so we get notified on the completion of the operation
 */
public class KafkaProducerInstrumenter {
    private static final String SPAN_NAME = "kafka-producer";
    private static final Logger logger = LoggerFactory.getLogger();
    private static final boolean CONTEXT_PROPAGATION_ENABLED = ConfigManager.getConfigOptional(ConfigProperty.AGENT_KAFKA_PROPAGATION, KafkaConstants.DEFAULT_CONTEXT_PROPAGATION_ENABLED);

    public static Callback onSend(ProducerRecord record, Callback callback, ApiVersions apiVersions) {
        String topic = record.topic();
        Integer partition = record.partition();

        Tracer.SpanBuilder builder = Tracer.INSTANCE.buildSpan(SPAN_NAME).withReporters(TraceEventSpanReporter.REPORTER);

        builder.withTag("Spec", "producer");
        builder.withTag("Flavor", "kafka");
        if (topic != null) {
            builder.withTag("Topic", topic);
        }
        if (partition != null) {
            builder.withTag("Partition", partition);
        }

        Span span = builder.start();

        if (CONTEXT_PROPAGATION_ENABLED && apiVersions.maxUsableProduceMagic() >= RecordBatch.MAGIC_VALUE_V2) {
            try {
                record.headers().add(ClassInstrumentation.XTRACE_HEADER, span.context().getMetadata().toHexString().getBytes());
            } catch (IllegalStateException e) {
                logger.debug("Cannot inject x-trace header into Kafka ProducerRecord : " + e.getMessage());
            }
        } else {
            logger.debug("Cannot inject x-trace header into Kafka ProducerRecord, Kafka Version has no support : " + apiVersions.maxUsableProduceMagic());
        }

        ClassInstrumentation.addBackTrace(span, 1, Module.KAFKA);

        return new WrappedCallback(callback, span);
    }

    private static class WrappedCallback implements Callback {
        private final Span span;
        private Callback originalCallback;

        private WrappedCallback(Callback originalCallback, Span span) {
            this.originalCallback = originalCallback;
            this.span = span;
        }


        @Override
        public void onCompletion(RecordMetadata recordMetadata, Exception e) {
            try {
                if (originalCallback != null) {
                    originalCallback.onCompletion(recordMetadata, e);
                }
            } finally {
                if (e != null) {
                    ClassInstrumentation.reportError(span, e);
                }
                span.finish();
            }
        }
    }


}