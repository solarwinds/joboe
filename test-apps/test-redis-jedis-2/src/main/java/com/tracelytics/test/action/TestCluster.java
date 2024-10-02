package com.tracelytics.test.action;

import java.util.Collections;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;

@SuppressWarnings("serial")
@org.apache.struts2.convention.annotation.Results({
    @org.apache.struts2.convention.annotation.Result(name="success", location="index.jsp"),
    @org.apache.struts2.convention.annotation.Result(name="error", location="index.jsp"),
})
public class TestCluster extends AbstractJedisAction {
    private static JedisCluster jedis;
    
    static {
        jedis = new JedisCluster(Collections.singleton(new HostAndPort(REDIS_HOST, 7000)));
    }
    
    @Override
    protected String test(Jedis jedisNotUsed) throws Exception {
        for (int i = 0 ; i < 10; i++) {
            String string = String.valueOf(i);
            jedis.set(string, string);
        }
        
        for (int i = 0 ; i < 10; i++) {
            String string = String.valueOf(i);
            jedis.get(string);
        }
        
        
        printToOutput("Finished testing cluster");
        
        return SUCCESS;
    }
}

