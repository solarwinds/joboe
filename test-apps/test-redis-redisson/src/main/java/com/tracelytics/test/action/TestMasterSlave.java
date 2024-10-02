package com.tracelytics.test.action;

import org.redisson.Config;
import org.redisson.Redisson;
import org.redisson.core.RBucket;

@SuppressWarnings("serial")
@org.apache.struts2.convention.annotation.Results({
    @org.apache.struts2.convention.annotation.Result(name="success", location="index.jsp"),
    @org.apache.struts2.convention.annotation.Result(name="error", location="index.jsp"),
})
public class TestMasterSlave extends AbstractRedissonAction {
    protected static final String REDIS_SLAVE_PORT = "6381";
    
    @Override
    protected String test() throws Exception {
        Config config = new Config();
        config.useMasterSlaveConnection().setMasterAddress(REDIS_HOST + ":" + REDIS_PORT).addSlaveAddress(REDIS_HOST + ":" + REDIS_SLAVE_PORT);
        Redisson redisson = Redisson.create(config);
        
        RBucket<String> bucket = redisson.getBucket(KEY);
        bucket.set(VALUE);
        
        bucket = redisson.getBucket(KEY);
        bucket.get();
        
        printToOutput("Finished testing master-slave");
        
        return SUCCESS;
    }

}
