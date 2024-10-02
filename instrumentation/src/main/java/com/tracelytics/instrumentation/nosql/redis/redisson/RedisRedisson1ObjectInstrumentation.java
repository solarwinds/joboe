package com.tracelytics.instrumentation.nosql.redis.redisson;

import java.util.Arrays;
import java.util.List;
import com.tracelytics.instrumentation.FrameworkVersion;

/**
 * Instruments the "distribute" java object implemented by Redisson, this is the higher level object manipulation which get translated to 
 * lower level Redis Operation by Redisson and handled by Lettuce. The actual Redis operation is instrumented in Lettuce instead
 * 
 * @author Patson Luk
 *
 */
public class RedisRedisson1ObjectInstrumentation extends BaseRedisRedissonObjectInstrumentation {

    @Override
    protected List<String> getTargetTypes() {
        return Arrays.asList(
                "org.redisson.core.RAtomicLong",
                "org.redisson.core.RBucket",
                "org.redisson.core.RCountDownLatch",
                "org.redisson.core.RDeque",
                "java.util.Deque",
                "org.redisson.core.RExpirable",
                "org.redisson.core.RHyperLogLog",
                "org.redisson.core.RList",
                "java.util.List",
                "org.redisson.core.RLock",
                "java.util.concurrent.locks.Lock",
                "org.redisson.core.RMap",
                "java.util.Map",
                "java.util.concurrent.ConcurrentMap",
                "org.redisson.core.RObject",
                "org.redisson.core.RQueue",
                "java.util.Queue",
                "org.redisson.core.RSet",
                "java.util.Set",
                "org.redisson.core.RSortedSet",
                "java.util.SortedSet",
                "org.redisson.core.RTopic");
    }

    @Override
    protected boolean isSupportedVersion(FrameworkVersion version) {
        return version.getMajorVersion() == 1;
    }
}