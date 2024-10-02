package com.tracelytics.test.action;

import java.util.UUID;

import org.redisson.Redisson;
import org.redisson.core.RBucket;

@SuppressWarnings("serial")
@org.apache.struts2.convention.annotation.Results({
    @org.apache.struts2.convention.annotation.Result(name="success", location="index.jsp"),
    @org.apache.struts2.convention.annotation.Result(name="error", location="index.jsp"),
})
public class TestSet extends AbstractRedissonAction {

    @Override
    protected String test() throws Exception {
        RBucket bucket = redisson.getBucket(KEY);
        bucket.set(VALUE);
        
        bucket = redisson.getBucket(KEY);
        bucket.set(1);
        
        bucket = redisson.getBucket(KEY);
        bucket.set(1.15);
        
        bucket = redisson.getBucket(KEY);
        bucket.set(true);
        
        bucket = redisson.getBucket(KEY);
        bucket.set(UUID.randomUUID());
        
        bucket = redisson.getBucket(KEY);
        bucket.set(VALUE);
        
        printToOutput("Finished testing SET");
        
        return SUCCESS;
    }

}
