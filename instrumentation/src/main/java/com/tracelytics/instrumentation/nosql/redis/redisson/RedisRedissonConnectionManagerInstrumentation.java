package com.tracelytics.instrumentation.nosql.redis.redisson;

import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.MethodMatcher;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.Event;

/**
 * Not standard Redis operation. However it is found in test case that this operation takes a considerable amount of time to perform. Therefore it is a good
 * instrument this as well to explain the gap
 * @author Patson Luk
 *
 */
public class RedisRedissonConnectionManagerInstrumentation extends ClassInstrumentation {
    private static final String CLASS_NAME = RedisRedissonConnectionManagerInstrumentation.class.getName();
    private static final String LAYER_NAME = "redis-redisson";
    
    private enum OpType { SHUT_DOWN }
            
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<OpType>> methodMatchers = Arrays.asList(
        new MethodMatcher<OpType>("shutdown", new String[]{ }, "void", OpType.SHUT_DOWN, true)
    );
    
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
            throws Exception {
        for (Entry<CtMethod, OpType> matchingMethodEntry : findMatchingMethods(cc, methodMatchers).entrySet()) {
            OpType type = matchingMethodEntry.getValue();
            CtMethod method = matchingMethodEntry.getKey();
            if (type == OpType.SHUT_DOWN) {
                insertBefore(method, CLASS_NAME + ".operationEntry(\"CLIENT_MANAGER_SHUT_DOWN\");");
                insertAfter(method, CLASS_NAME + ".operationExit();", true);
            }
        }
        
        return true;
    }
    
    public static void operationEntry(String operation) {
        Event event = Context.createEvent();
        event.addInfo("Label", "entry",
                      "Layer", LAYER_NAME,
                      "KVOp", operation);
        
        event.report();
    }
    
    public static void operationExit() {
        Event event = Context.createEvent();
        event.addInfo("Label", "exit",
                      "Layer", LAYER_NAME);
        
        event.report();
    }
}