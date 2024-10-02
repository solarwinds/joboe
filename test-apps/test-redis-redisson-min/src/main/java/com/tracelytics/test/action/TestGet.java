package com.tracelytics.test.action;

import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisConnection;

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
        RedisClient client = new RedisClient(REDIS_HOST);
        RedisConnection<String, String> connection = client.connect();
        
        System.out.println(connection.get(KEY));
        System.out.println(connection.getbit(KEY, 1));
        connection.getrange(KEY, 0, 1);
        
        connection.get(LONG_STRING_KEY);
        
        connection.close();
        
        printToOutput("Finished testing on GET");
        return SUCCESS;
    }

}
