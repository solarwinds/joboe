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

/**
 * Instruments `org.apache.kafka.clients.producer.KafkaProducer`
 *
 * @see com.appoptics.apploader.instrumenter.mq.kafka.KafkaProducerInstrumenter
 */
@Instrument(targetType = "org.apache.kafka.clients.producer.KafkaProducer", module = Module.KAFKA,
        appLoaderPackage = "com.appoptics.apploader.instrumenter.mq.kafka")
@AutoService(ClassInstrumentation.class)
public class KafkaProducerInstrumentation extends KafkaInstrumentation {
    private static final String INSTRUMENTER_CLASS_NAME = "com.appoptics.apploader.instrumenter.mq.kafka.KafkaProducerInstrumenter";

    private enum OpType { SEND }
            
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<OpType>> methodMatchers = Arrays.asList(
        new MethodMatcher<OpType>("send", new String[]{ "org.apache.kafka.clients.producer.ProducerRecord", "org.apache.kafka.clients.producer.Callback"} , "java.util.concurrent.Future", OpType.SEND)
    );
    
    public boolean applyKafkaInstrumentation(CtClass cc, String className, byte[] classBytes)
            throws Exception {
        for (CtMethod method : findMatchingMethods(cc, methodMatchers).keySet()) {
            insertBefore(method, "$2 = " + INSTRUMENTER_CLASS_NAME + ".onSend($1, $2, apiVersions);");
        }
        
        return true;
    }
}