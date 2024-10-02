package com.tracelytics.instrumentation.nosql.redis.lettuce;

/**
 * Used to tag a patched Lettuce {@code CommandArgs} such that we can more conveniently retrieve the key and script value set against the {@code CommandArgs} instance
 * @author Patson Luk
 *
 */
public interface RedisLettuceCommandArgs  {
    void tvSetKey(Object key);
    Object tvGetKey();
    int tvGetKeyCount();
    
    void tvSetScript(Object script);
    Object tvGetScript();
    
}
