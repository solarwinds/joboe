package com.appoptics.instrumentation.nosql.redisson3;

import com.appoptics.instrumentation.nosql.redisson2.RedisRedisson2ConnectionInstrumentation;
import com.appoptics.instrumentation.nosql.redisson2.RedisRedisson2ObjectInstrumentation;
import com.google.auto.service.AutoService;
import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.*;
import com.tracelytics.instrumentation.Module;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Instruments `org.redisson.client.RedisConnection` of Redisson 3 to capture the start of
 * the lower level "command span" on `send` invocation.
 *
 * Extends `RedisRedisson2ConnectionInstrumentation` as most of the captured behaviors are the same as version 2
 *
 * This class provides a different `instrumenterClass` so it uses the correct "Instrumenter" to handle
 * the Redission version 3 objects
 *
 */
@AutoService(ClassInstrumentation.class)
@Instrument(targetType = { "org.redisson.client.RedisConnection" },
        module = Module.REDIS,
        appLoaderPackage = { "com.appoptics.apploader.instrumenter.nosql.redisson2", "com.appoptics.apploader.instrumenter.nosql.redisson3"})
public class RedisRedisson3ConnectionInstrumentation extends RedisRedisson2ConnectionInstrumentation {
    private static final String INSTRUMENTER_CLASS = "com.appoptics.apploader.instrumenter.nosql.redisson3.Redisson3Instrumenter.SINGLETON";

    @Override
    protected String instrumenterClass() {
        return INSTRUMENTER_CLASS;
    }

    @Override
    protected boolean isSupportedVersion(FrameworkVersion version) {
        return version.getMajorVersion() >= 3;
    }


}