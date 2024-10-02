package com.tracelytics.instrumentation.nosql.redis.redisson;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.MethodMatcher;

import java.util.Arrays;
import java.util.List;

/**
 * Patches Iterator implementation within Redisson classes. Those operation should always be considered as synchronous
 * even though the `hasNext` operation triggers the `getAsync` from the Redisson object
 */
public class RedisRedissonIteratorPatcher extends ClassInstrumentation {
    private static List<MethodMatcher<MethodType>> methodMatchers = Arrays.asList(
            new MethodMatcher<MethodType>("hasNext", new String[]{ }, "boolean", MethodType.HAS_NEXT)
    );

    private enum MethodType {
        HAS_NEXT
    }
    @Override
    protected boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes) throws Exception {
        for (CtMethod method : findMatchingMethods(cc, methodMatchers).keySet()) {
            insertBefore(method, BaseRedisRedissonObjectInstrumentation.class.getName() + ".flagIteratorOperation(true);");
            insertAfter(method, BaseRedisRedissonObjectInstrumentation.class.getName() + ".flagIteratorOperation(false);", true);
        }


        return true;
    }
}
