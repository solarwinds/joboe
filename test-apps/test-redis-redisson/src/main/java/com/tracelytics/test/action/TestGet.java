package com.tracelytics.test.action;

import org.redisson.core.RBucket;

@SuppressWarnings("serial")
@org.apache.struts2.convention.annotation.Results({
    @org.apache.struts2.convention.annotation.Result(name="success", location="index.jsp"),
    @org.apache.struts2.convention.annotation.Result(name="error", location="index.jsp"),
})
public class TestGet extends AbstractRedissonAction {
    private static final String LONG_STRING_KEY;
    static {
        StringBuffer longStringBuffer = new StringBuffer("long-string-key");
        for (int i = 0; i < 1000; i ++) {
            longStringBuffer.append(i);
        }
        LONG_STRING_KEY = longStringBuffer.toString();
    }

    @Override
    protected String test() throws Exception {
        RBucket<String> bucket = redisson.getBucket(KEY);
        bucket.get();
        
        RBucket<String> notFoundBucket = redisson.getBucket("not-exist");
        notFoundBucket.get();
        
        notFoundBucket = redisson.getBucket(LONG_STRING_KEY);
        notFoundBucket.get();
        
        printToOutput("Finished testing GET");
        return SUCCESS;
    }

}
