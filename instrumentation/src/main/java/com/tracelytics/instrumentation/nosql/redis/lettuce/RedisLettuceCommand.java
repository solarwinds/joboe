package com.tracelytics.instrumentation.nosql.redis.lettuce;

import com.tracelytics.joboe.Metadata;

/**
 * Used to tag a patched Lettuce {@code Command} to better keep track of various status
 * @author Patson Luk
 *
 */
public interface RedisLettuceCommand  {
    /**
     * @return  The name of the command
     * 
     */
    String tvGetName();
    /**
     * @return  Whether it is a KV hit, only makes sense for GET operations
     */
    boolean tvIsHit();
    /**
     * 
     * @return  CommandArgs used
     */
    Object tvGetArgs();
    /**
     * @return  Whenever this is a command used in a MULTI operation (transaction)
     */
    boolean tvIsMulti();
    void tvSetMulti(boolean isMulti);

    /**
     * @return  Whenever this command has been sent (that an entry event is created)
     */
    boolean tvHasSent();
    void tvSetHasSent(boolean hasSent);
    
    void tvSetMetadata(Metadata metadata);
    Metadata tvGetMetadata();
}
