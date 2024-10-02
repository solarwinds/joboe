package com.appoptics.instrumentation.mq.rabbitmq;

import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import com.google.auto.service.AutoService;
import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.Instrument;
import com.tracelytics.instrumentation.MethodMatcher;
import com.tracelytics.instrumentation.Module;

/**
 * Instruments RabbitMQ's channel for "publish" operations
 * @author pluk
 *
 */

@AutoService(ClassInstrumentation.class)
@Instrument(targetType = "com.rabbitmq.client.Channel", module = Module.RABBIT_MQ, appLoaderPackage = "com.appoptics.apploader.instrumenter.mq.rabbitmq")
public class RabbitMqChannelInstrumentation extends ClassInstrumentation {
    private static final String INSTRUMENTER_CLASS_NAME = "com.appoptics.apploader.instrumenter.mq.rabbitmq.RabbitMqChannelInstrumenter";

    private enum OpType { PUBLISH, CONSUME, GET }
            
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<OpType>> methodMatchers = Arrays.asList(
        new MethodMatcher<OpType>("basicPublish", new String[]{ "java.lang.String", "java.lang.String", "boolean", "boolean", "com.rabbitmq.client.AMQP$BasicProperties", "byte[]" }, "void", OpType.PUBLISH),
        new MethodMatcher<OpType>("basicConsume", new String[]{ "java.lang.String", "boolean", "java.lang.String", "boolean", "boolean", "java.util.Map", "com.rabbitmq.client.Consumer" }, "java.lang.String", OpType.CONSUME),
        new MethodMatcher<OpType>("basicGet", new String[]{ "java.lang.String" }, "com.rabbitmq.client.GetResponse", OpType.GET)
    );


    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
            throws Exception {
        for (Entry<CtMethod, OpType> matchingMethodEntry : findMatchingMethods(cc, methodMatchers).entrySet()) {
            OpType type = matchingMethodEntry.getValue();
            CtMethod method = matchingMethodEntry.getKey();
            if (type == OpType.PUBLISH) {
                insertBefore(method, "$5 = " + INSTRUMENTER_CLASS_NAME + ".beforePublish(this, $1, $2, $5, $6);", false);
                insertAfter(method, INSTRUMENTER_CLASS_NAME + ".afterPublish();", true, false);
            } else if (type == OpType.GET) {
                insertBefore(method, INSTRUMENTER_CLASS_NAME + ".beforeGet();", false);
                insertAfter(method, INSTRUMENTER_CLASS_NAME + ".afterGet(this, $1, $_);", true, false);

            } else if (type == OpType.CONSUME) { //we only want to flag consumers bound with the `consume` call
                insertBefore(method, INSTRUMENTER_CLASS_NAME + ".beforeConsume($7, $1, this);", false);
            }
        }
        
        return true;
    }
    


}