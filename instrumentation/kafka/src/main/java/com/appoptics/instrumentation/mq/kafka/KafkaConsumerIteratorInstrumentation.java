package com.appoptics.instrumentation.mq.kafka;

import com.google.auto.service.AutoService;
import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtField;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.ext.javassist.NotFoundException;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.Instrument;
import com.tracelytics.instrumentation.MethodMatcher;
import com.tracelytics.instrumentation.Module;
import com.tracelytics.joboe.span.impl.Scope;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Instruments `org.apache.kafka.common.utils.AbstractIterator` for Kafka consumer handling
 *
 * @see com.appoptics.apploader.instrumenter.mq.kafka.KafkaConsumerIteratorInstrumenter
 */
@AutoService(ClassInstrumentation.class)
@Instrument(targetType = "org.apache.kafka.common.utils.AbstractIterator", module = Module.KAFKA, appLoaderPackage = "com.appoptics.apploader.instrumenter.mq.kafka")
public class KafkaConsumerIteratorInstrumentation extends KafkaInstrumentation {
    private static final String INSTRUMENTER_CLASS_NAME = "com.appoptics.apploader.instrumenter.mq.kafka.KafkaConsumerIteratorInstrumenter";

    private enum OpType { NEXT, HAS_NEXT }

    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<OpType>> methodMatchers = Arrays.asList(
            new MethodMatcher<OpType>("next", new String[]{}, "java.lang.Object", OpType.NEXT),
            new MethodMatcher<OpType>("hasNext", new String[]{}, "boolean", OpType.HAS_NEXT)
    );

    public boolean applyKafkaInstrumentation(CtClass cc, String className, byte[] classBytes)
            throws Exception {
        try {
            cc.getField("aoScope");
        } catch (NotFoundException e) {
            cc.addField(CtField.make("protected " + Scope.class.getName() + " aoScope;", cc));
        }

        for (Map.Entry<CtMethod, OpType> entry : findMatchingMethods(cc, methodMatchers).entrySet()) {
            CtMethod method = entry.getKey();
            OpType type = entry.getValue();
            if (type == OpType.HAS_NEXT) {
                insertAfter(method, INSTRUMENTER_CLASS_NAME + ".onHasNext(aoScope);" +
                        "aoScope = null;", true, false);
            } else if (type == OpType.NEXT) {
                insertAfter(method, "aoScope = " + INSTRUMENTER_CLASS_NAME + ".onNext($_);", true, false);
            }

        }

        return true;
    }
}