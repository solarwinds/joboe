package com.appoptics.instrumentation.mq.rabbitmq;

import java.net.InetAddress;
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
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.Event;

/**
 * Instruments RabbitMQ's RPC client for RPC operations
 * @author pluk
 *
 */

@AutoService(ClassInstrumentation.class)
@Instrument(targetType = "com.rabbitmq.client.RpcClient", module = Module.RABBIT_MQ, appLoaderPackage = "com.appoptics.apploader.instrumenter.mq.rabbitmq")
public class RabbitMqRpcClientInstrumentation extends ClassInstrumentation {
    private static final String CLASS_NAME = RabbitMqRpcClientInstrumentation.class.getName();
    private static final String LAYER_NAME = "rabbit-mq-rpc";
    
    private enum OpType { CALL }
            
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<OpType>> methodMatchers = Arrays.asList(
        new MethodMatcher<OpType>("primitiveCall", new String[]{ "byte[]" }, "byte[]", OpType.CALL)
    );
    
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
            throws Exception {
        for (Entry<CtMethod, OpType> matchingMethodEntry : findMatchingMethods(cc, methodMatchers).entrySet()) {
            OpType type = matchingMethodEntry.getValue();
            CtMethod method = matchingMethodEntry.getKey();
            if (type == OpType.CALL) {
                insertBefore(method, CLASS_NAME + ".rpcEntry(getExchange(), getRoutingKey(), "
                        + "getChannel() != null && getChannel().getConnection() != null ? getChannel().getConnection().getAddress() : null, "
                        + "getChannel() != null && getChannel().getConnection() != null ? getChannel().getConnection().getPort() : -1);");
                insertAfter(method, CLASS_NAME + ".rpcExit();", true);
            }
        }
        
        return true;
    }
    
    public static void rpcEntry(String exchange, String routingKey, InetAddress remoteHost, int port) {
        Event event = Context.createEvent();
        event.addInfo("Label", "entry",
                      "Layer", LAYER_NAME,
                      "Flavor", "amqp");
        
        if (exchange != null) {
            event.addInfo("ExchangeName", exchange);
        }
        
        if (routingKey != null) {
            event.addInfo("RoutingKey", routingKey);
        }
        
        if (remoteHost != null) {
            event.addInfo("RemoteHost", remoteHost.getHostAddress() + ":" + port);
        }
        event.report();
    }
    
    
    
    public static void rpcExit() {
        Event event = Context.createEvent();
        event.addInfo("Label", "exit",
                      "Layer", LAYER_NAME);
        event.report();
    }
}