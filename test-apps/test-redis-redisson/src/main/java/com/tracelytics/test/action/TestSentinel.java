package com.tracelytics.test.action;

import org.redisson.Config;
import org.redisson.Redisson;
import org.redisson.core.RBucket;

@SuppressWarnings("serial")
@org.apache.struts2.convention.annotation.Results({
    @org.apache.struts2.convention.annotation.Result(name="success", location="index.jsp"),
    @org.apache.struts2.convention.annotation.Result(name="error", location="index.jsp"),
})
public class TestSentinel extends AbstractRedissonAction {
    protected static final String REDIS_SENTINEL_PORT = "26379";
    protected static final String REDIS_MASTER_NAME = "mymaster";
    
    @Override
    protected String test() throws Exception {
        Config config = new Config();
        config.useSentinelConnection().addSentinelAddress(REDIS_HOST + ":" + REDIS_SENTINEL_PORT).setMasterName(REDIS_MASTER_NAME);
        Redisson redisson = Redisson.create(config);
        
        RBucket<String> bucket = redisson.getBucket(KEY);
        bucket.set(VALUE);
        
        bucket = redisson.getBucket(KEY);
        bucket.get();
        
        printToOutput("Finished testing sentinel");
        
        return SUCCESS;
    }

}
