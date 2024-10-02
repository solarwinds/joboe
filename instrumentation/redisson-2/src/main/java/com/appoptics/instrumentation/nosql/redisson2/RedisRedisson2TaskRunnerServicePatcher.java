package com.appoptics.instrumentation.nosql.redisson2;

import com.google.auto.service.AutoService;
import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.Module;
import com.tracelytics.instrumentation.*;
import com.tracelytics.instrumentation.nosql.redis.redisson.RedisRedissonVersionReader;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Patches `org.redisson.executor.TasksRunnerService` to avoid instrumentation of internal scripts on `renewRetryTime`
 *
 */
@AutoService(ClassInstrumentation.class)
@Instrument(targetType = { "org.redisson.executor.TasksRunnerService" },
        module = Module.REDIS,
        appLoaderPackage = { "com.appoptics.apploader.instrumenter.nosql.redisson2" })
public class RedisRedisson2TaskRunnerServicePatcher extends ClassInstrumentation {
    private static final String INSTRUMENTER_CLASS = "com.appoptics.apploader.instrumenter.nosql.redisson2.Redisson2Instrumenter.SINGLETON";

    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<OpType>> methodMatchers = Arrays.asList(
            new MethodMatcher<OpType>("renewRetryTime", new String[]{ "java.lang.String" }, "void", OpType.RENEW_RETRY_TIME, true)
    );

    private enum OpType { RENEW_RETRY_TIME }
    private FrameworkVersion version;

    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
            throws Exception {
        if (version == null) {
            version = new RedisRedissonVersionReader(classPool).getFrameworkVersion();
        }

        if (!isSupportedVersion(version)) {
            return false;
        }

        Map<CtMethod, OpType> matchingMethods = findMatchingMethods(cc, methodMatchers);

        for (CtMethod method : matchingMethods.keySet()) {
            insertBefore(method, instrumenterClass() + ".flagInternalScript(true);");
            insertAfter(method, instrumenterClass() + ".flagInternalScript(false);", true);
        }

        return true;
    }

    protected String instrumenterClass() {
        return INSTRUMENTER_CLASS;
    }

    protected boolean isSupportedVersion(FrameworkVersion version) {
        return version.getMajorVersion() == 2;
    }


}