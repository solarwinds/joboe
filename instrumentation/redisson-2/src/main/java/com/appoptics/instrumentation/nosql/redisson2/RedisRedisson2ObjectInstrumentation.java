package com.appoptics.instrumentation.nosql.redisson2;

import com.google.auto.service.AutoService;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.FrameworkVersion;
import com.tracelytics.instrumentation.Instrument;
import com.tracelytics.instrumentation.Module;
import com.tracelytics.instrumentation.nosql.redis.redisson.BaseRedisRedissonObjectInstrumentation;

import java.util.Arrays;
import java.util.List;

/**
 * Instruments the `org.redisson.api.RObject` and `org.redisson.api.RScript` for high level Redission v2 object operations
 * such as `org.redission.api.RList`. Extends `BaseRedisRedissonObjectInstrumentation` which implements basic
 * instrumentation for Redisson object. This class provides a specific list of class for Redisson version 2 via `getTargetTypes`
 */
@AutoService(ClassInstrumentation.class)
@Instrument(targetType = { "org.redisson.api.RObject", "org.redisson.api.RScript" },
        module = Module.REDIS,
        appLoaderPackage = { "com.appoptics.apploader.instrumenter.nosql.redisson2" })
public class RedisRedisson2ObjectInstrumentation extends BaseRedisRedissonObjectInstrumentation {
    private static final String INSTRUMENTER_CLASS = "com.appoptics.apploader.instrumenter.nosql.redisson2.Redisson2Instrumenter.SINGLETON";

    @Override
    protected String getAsyncHandling() {
        return instrumenterClass() + ".handleAsync($_);";
    }

    @Override
    protected List<String> getTargetTypes() {
        return Arrays.asList(
                "org.redisson.api.RAtomicLong",
                "org.redisson.api.RBucket",
                "org.redisson.api.RCountDownLatch",
                "org.redisson.api.RDeque",
                "java.util.Deque",
                "org.redisson.api.RExpirable",
                "org.redisson.api.RHyperLogLog",
                "org.redisson.api.RList",
                "java.util.List",
                "org.redisson.api.RLock",
                "java.util.concurrent.locks.Lock",
                "org.redisson.api.RMap",
                "java.util.Map",
                "java.util.concurrent.ConcurrentMap",
                "org.redisson.api.RObject",
                "org.redisson.api.RQueue",
                "java.util.Queue",
                "org.redisson.api.RSet",
                "java.util.Set",
                "org.redisson.api.RSortedSet",
                "java.util.SortedSet",
                "org.redisson.api.RGeo",
                "org.redisson.api.RScoredSortedSet",
                "org.redisson.api.RLexSortedSet",
                "org.redisson.api.RMultimap",
                "org.redisson.api.RListMultimap",
                "org.redisson.api.RSetMultimap",
                "org.redisson.api.RTopic",
                "org.redisson.api.RAtomicLongAsync",
                "org.redisson.api.RBucketAsync",
                "org.redisson.api.RCountDownLatchAsync",
                "org.redisson.api.RCollectionAsync",
                "org.redisson.api.RDequeAsync",
                "org.redisson.api.RExpirableAsync",
                "org.redisson.api.RHyperLogLogAsync",
                "org.redisson.api.RListAsync",
                "org.redisson.api.RLockAsync",
                "org.redisson.api.RMapAsync",
                "org.redisson.api.RObjectAsync",
                "org.redisson.api.RQueueAsync",
                "org.redisson.api.RSetAsync",
                "org.redisson.api.RGeoAsync",
                "org.redisson.api.RScoredSortedSetAsync",
                "org.redisson.api.RLexSortedSetAsync",
                "org.redisson.api.RMultimapAsync",
                "org.redisson.api.RTopicAsync",
                "org.redisson.api.RScript",
                "org.redisson.api.RScriptAsync");
    }

    @Override
    protected boolean isSupportedVersion(FrameworkVersion version) {
        return version.getMajorVersion() == 2;
    }

    protected String instrumenterClass() {
        return INSTRUMENTER_CLASS;
    }
}