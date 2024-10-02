package com.tracelytics.test.action;

import java.util.Collections;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisSentinelPool;

@SuppressWarnings("serial")
@org.apache.struts2.convention.annotation.Results({
    @org.apache.struts2.convention.annotation.Result(name="success", location="index.jsp"),
    @org.apache.struts2.convention.annotation.Result(name="error", location="index.jsp"),
})
public class TestSentinel extends AbstractJedisAction {
    private static Jedis jedis;
    private static Jedis sentinelJedis; //has to use a different instance
    
    private static final int SENTINEL_PORT = 26379;
    
    static {
        JedisSentinelPool pool = new JedisSentinelPool("mymaster", Collections.singleton(REDIS_HOST + ":" + SENTINEL_PORT));
        jedis = pool.getResource();
        
        sentinelJedis = new Jedis(REDIS_HOST, SENTINEL_PORT);
        
    }
    
    @Override
    protected String test(Jedis jedisNotUsed) throws Exception {
        sentinelJedis.sentinelFailover("mymaster");
        sentinelJedis.sentinelGetMasterAddrByName("mymaster");
        sentinelJedis.sentinelMasters();
      //sentinelJedis.sentinelMonitor(masterName, ip, port, quorum);
//        sentinelJedis.sentinelRemove("not-exist");
        sentinelJedis.sentinelReset("*");
        sentinelJedis.sentinelSet("mymaster", Collections.singletonMap("down-after-milliseconds", "1000"));
        sentinelJedis.sentinelSlaves("mymaster");
        
        printToOutput("Finished testing sentinel");
        
        return SUCCESS;
    }
}

