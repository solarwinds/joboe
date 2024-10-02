package com.tracelytics.test.action;

import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisConnection;
import com.lambdaworks.redis.SortArgs;

@SuppressWarnings("serial")
@org.apache.struts2.convention.annotation.Results({
    @org.apache.struts2.convention.annotation.Result(name="success", location="index.jsp"),
    @org.apache.struts2.convention.annotation.Result(name="error", location="index.jsp"),
})
public class TestMulti extends AbstractRedissonAction {
    
    @Override
    protected String test() throws Exception {
        RedisClient client = new RedisClient(REDIS_HOST);
        RedisConnection<String, String> connection = client.connect();
        
        connection.multi();
        connection.set(KEY, VALUE);
        connection.get(KEY);
        connection.del("random-list");
        connection.lpush("random-list", LIST);
        connection.sortStore("random-list", SortArgs.Builder.asc(), "new-list");
        connection.exec();
        
        connection.multi();
        connection.sortStore("random-list", SortArgs.Builder.asc(), "new-list");
        connection.exec();
        
        connection.multi();
        connection.sortStore("random-list", SortArgs.Builder.asc(), "new-list");
        connection.discard();
                                
        printToOutput("Finished testing on multi");
        return SUCCESS;
    }

}
