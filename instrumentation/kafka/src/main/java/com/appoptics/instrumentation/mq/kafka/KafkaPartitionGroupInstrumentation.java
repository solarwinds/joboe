package com.appoptics.instrumentation.mq.kafka;

import com.google.auto.service.AutoService;
import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.Instrument;
import com.tracelytics.instrumentation.MethodMatcher;
import com.tracelytics.instrumentation.Module;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Instruments `org.apache.kafka.streams.processor.internals.PartitionGroup` to capture entry point of Stream span as consumer
 *
 * @see com.appoptics.apploader.instrumenter.mq.kafka.KafkaStreamTaskInstrumenter
 */
@AutoService(ClassInstrumentation.class)
@Instrument(targetType = "org.apache.kafka.streams.processor.internals.PartitionGroup", module = Module.KAFKA, appLoaderPackage = "com.appoptics.apploader.instrumenter.mq.kafka")
public class KafkaPartitionGroupInstrumentation extends KafkaInstrumentation {
    private static final String INSTRUMENTER_CLASS_NAME = "com.appoptics.apploader.instrumenter.mq.kafka.KafkaStreamTaskInstrumenter";

    private enum OpType { NEXT_RECORD }

    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<OpType>> methodMatchers = Arrays.asList(
            new MethodMatcher<OpType>("nextRecord", new String[]{ "org.apache.kafka.streams.processor.internals.PartitionGroup$RecordInfo" }, "org.apache.kafka.streams.processor.internals.StampedRecord", OpType.NEXT_RECORD)
    );

    public boolean applyKafkaInstrumentation(CtClass cc, String className, byte[] classBytes)
            throws Exception {

        for (Map.Entry<CtMethod, OpType> entry : findMatchingMethods(cc, methodMatchers).entrySet()) {
            CtMethod method = entry.getKey();
            OpType type = entry.getValue();
            if (type == OpType.NEXT_RECORD) {
                insertAfter(method, INSTRUMENTER_CLASS_NAME + ".onNextRecord($_);", true, false);
            }

        }

        return true;
    }
}