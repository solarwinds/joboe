package com.tracelytics.instrumentation.nosql.redis.lettuce;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtConstructor;
import com.tracelytics.ext.javassist.CtField;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.ConstructorMatcher;
import com.tracelytics.instrumentation.MethodMatcher;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.Event;

/**
 * Not the standard Redis operation. However, it is found that the client connect and shutdown takes up a long time in test cases. Therefore, it is good that
 * we can explain those long time spent in the library even tho it is not standard Redis operation 
 * @author Patson Luk
 *
 */
public class RedisLettuceClientInstrumentation extends ClassInstrumentation {
    private static final String CLASS_NAME = RedisLettuceClientInstrumentation.class.getName();
    private static final String LAYER_NAME = "redis-lettuce";
    
    private enum OpType { CONNECT, SHUT_DOWN, OLD_CONSTRUCTOR, NEW_CONSTRUCTOR }
            
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<OpType>> methodMatchers = Arrays.asList(
        new MethodMatcher<OpType>("connectAsync", new String[]{ "com.lambdaworks.redis.codec.RedisCodec" }, "com.lambdaworks.redis.RedisAsyncConnection", OpType.CONNECT, true),
        new MethodMatcher<OpType>("shutdown", new String[]{ }, "void", OpType.SHUT_DOWN, true)
    );
    
    @SuppressWarnings("unchecked")
    private static List<ConstructorMatcher<OpType>> constructorMatchers = Arrays.asList(
        new ConstructorMatcher<OpType>(new String[]{ "java.lang.String", "int" }, OpType.OLD_CONSTRUCTOR),
        new ConstructorMatcher<OpType>(new String[]{ "java.lang.Object", "java.lang.String", "int" }, OpType.NEW_CONSTRUCTOR)
    );
    
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
            throws Exception {
        
        if (cc.equals(classPool.get("com.lambdaworks.redis.RedisClient"))) { //only modify the fields and ctors of the base class
            cc.addField(CtField.make("protected String tvHost;", cc));
            cc.addField(CtField.make("protected int tvPort;", cc));
            for (Entry<CtConstructor, OpType> entry : findMatchingConstructors(cc, constructorMatchers).entrySet()) {
                CtConstructor constructor = entry.getKey();
                OpType opType = entry.getValue();
                
                if (opType == OpType.OLD_CONSTRUCTOR) {                
                    insertAfter(constructor, "tvHost = $1; tvPort = $2;");
                } else if (opType == OpType.NEW_CONSTRUCTOR) {
                    insertAfter(constructor, "tvHost = $2; tvPort = $3;");
                }
            }
        }
        
        Map<CtMethod, OpType> matchingMethods = findMatchingMethods(cc, methodMatchers);
        
        for (Entry<CtMethod, OpType> matchingMethodEntry : matchingMethods.entrySet()) {
            OpType type = matchingMethodEntry.getValue();
            CtMethod method = matchingMethodEntry.getKey();
            if (type == OpType.CONNECT) {
                insertBefore(method, CLASS_NAME + ".operationEntry(\"CLIENT_CONNECT\", tvHost, tvPort);");
                insertAfter(method, CLASS_NAME + ".operationExit();", true);
            } else if (type == OpType.SHUT_DOWN) {
                insertBefore(method, CLASS_NAME + ".operationEntry(\"CLIENT_SHUT_DOWN\", tvHost, tvPort);");
                insertAfter(method, CLASS_NAME + ".operationExit();", true);
            }
        }
        
        return true;
    }
    
    public static void operationEntry(String operation, String host, int port) {
        Event event = Context.createEvent();
        event.addInfo("Label", "entry",
                      "Layer", LAYER_NAME);
        
        if (host != null) {
            event.addInfo("RemoteHost", host + ":" + port);
        }
        
        event.addInfo("KVOp", operation);
        event.setAsync();
        event.report();
    }
    
    public static void operationExit() {
        Event event = Context.createEvent();
        event.addInfo("Label", "exit",
                      "Layer", LAYER_NAME);
        
        event.report();
    }
}