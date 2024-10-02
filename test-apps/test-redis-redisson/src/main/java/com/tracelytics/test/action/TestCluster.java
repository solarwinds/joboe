package com.tracelytics.test.action;

import org.redisson.Config;
import org.redisson.Redisson;
import org.redisson.core.RBucket;

@SuppressWarnings("serial")
@org.apache.struts2.convention.annotation.Results({
    @org.apache.struts2.convention.annotation.Result(name="success", location="index.jsp"),
    @org.apache.struts2.convention.annotation.Result(name="error", location="index.jsp"),
})
public class TestCluster extends AbstractRedissonAction {
    private static final String[] REDIS_NODE_ADDRESSES = new String[] { REDIS_HOST + ":7000" };//, REDIS_HOST + ":7001", REDIS_HOST + ":7002" };
    
    @Override
    protected String test() throws Exception {
        Config config = new Config();
        config.useClusterServers().addNodeAddress(REDIS_NODE_ADDRESSES);
        Redisson redisson = Redisson.create(config);
        
        for (int i = 0; i < 10; i++) {
            RBucket<Integer> bucket = redisson.getBucket(String.valueOf(i));
            bucket.set(i);
        }
        
        for (int i = 0; i < 10; i++) {
            RBucket<Integer> bucket = redisson.getBucket(String.valueOf(i));
            bucket.get();
        }
        
        printToOutput("Finished testing cluster");
        
        return SUCCESS;
    }

}
