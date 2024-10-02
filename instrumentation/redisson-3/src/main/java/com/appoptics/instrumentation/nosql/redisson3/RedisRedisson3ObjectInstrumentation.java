package com.appoptics.instrumentation.nosql.redisson3;

import com.appoptics.instrumentation.nosql.redisson2.RedisRedisson2ObjectInstrumentation;
import com.google.auto.service.AutoService;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.FrameworkVersion;
import com.tracelytics.instrumentation.Instrument;
import com.tracelytics.instrumentation.Module;

/**
 * Instruments the `org.redisson.api.RObject` and `org.redisson.api.RScript` for high level Redission v3 object operations
 * such as `org.redission.api.RList`. Extends `RedisRedisson2ObjectInstrumentation`, which is a child class of `BaseRedisRedissonObjectInstrumentation`,
 * which implements basic instrumentation for Redisson object.
 *
 * This class provides a different `instrumenterClass` so it uses the correct "Instrumenter" to handle
 * the Redission version 3 objects
 */
@AutoService(ClassInstrumentation.class)
@Instrument(targetType = { "org.redisson.api.RObject", "org.redisson.api.RScript" },
        module = Module.REDIS,
        appLoaderPackage = { "com.appoptics.apploader.instrumenter.nosql.redisson2", "com.appoptics.apploader.instrumenter.nosql.redisson3"})
public class RedisRedisson3ObjectInstrumentation extends RedisRedisson2ObjectInstrumentation {
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