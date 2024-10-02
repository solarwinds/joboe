package com.tracelytics.instrumentation.nosql.redis.lettuce;

/**
 * Used to tag Lettuce {@code RedisClient} so we can extract Host and Port set against the client instance
 * @author Patson Luk
 *
 */
public interface RedisLettuceClient  {
    String tvGetHost();
    int tvGetPort();
}
