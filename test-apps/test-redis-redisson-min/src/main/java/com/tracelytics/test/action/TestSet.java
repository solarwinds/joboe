package com.tracelytics.test.action;

import org.redisson.Redisson;
import org.redisson.core.RAtomicLong;


@SuppressWarnings("serial")
@org.apache.struts2.convention.annotation.Results({
    @org.apache.struts2.convention.annotation.Result(name="success", location="index.jsp"),
    @org.apache.struts2.convention.annotation.Result(name="error", location="index.jsp"),
})
public class TestSet extends AbstractRedissonAction {
    @Override
    protected String test() throws Exception {
        Redisson redisson = Redisson.create(config);
        
        RAtomicLong atomicLong = redisson.getAtomicLong("atomic");
        atomicLong.set(new Long(1000));
        atomicLong.addAndGet(5);
        atomicLong.incrementAndGet();
        
        RAtomicLong bucket = redisson.getAtomicLong(KEY);
        bucket.set(100);
        
        redisson.shutdown();
        
        printToOutput("Finished testing SET");
        return SUCCESS;

    }
    
}
