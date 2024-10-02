package com.appoptics.instrumentation.mq.kafka;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.NotFoundException;
import com.tracelytics.instrumentation.ClassInstrumentation;

public abstract class KafkaInstrumentation extends ClassInstrumentation {
    @Override
    protected boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes) throws Exception {
        try {
            classPool.getCtClass("org.apache.kafka.clients.ApiVersions");
            return applyKafkaInstrumentation(cc, className, classBytes);
        } catch (NotFoundException e) {
            logger.debug("Skipping instrumentation " + className + " . Not a supported Kafka version");
            return false;
        }

    }

    protected abstract boolean applyKafkaInstrumentation(CtClass cc, String className, byte[] classBytes) throws Exception;
}
