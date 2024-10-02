package com.tracelytics.test.action;

import java.util.ArrayList;
import java.util.List;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisShardInfo;
import redis.clients.jedis.ShardedJedis;

@SuppressWarnings("serial")
@org.apache.struts2.convention.annotation.Results({
    @org.apache.struts2.convention.annotation.Result(name="success", location="index.jsp"),
    @org.apache.struts2.convention.annotation.Result(name="error", location="index.jsp"),
})
public class TestShard extends AbstractJedisAction {
    private static ShardedJedis jedis;
    
    static {
        List<JedisShardInfo> shards = new ArrayList<JedisShardInfo>();
        JedisShardInfo si = new JedisShardInfo(REDIS_HOST, 6379);
        shards.add(si);
        si = new JedisShardInfo(REDIS_HOST, 6380);
        shards.add(si);
        
        jedis = new ShardedJedis(shards);
    }
    
    @Override
    protected String test(Jedis jedisNotUsed) throws Exception {
        for (int i = 0 ; i < 10; i++) {
            String string = String.valueOf(i);
            jedis.set(string, string);
        }
        
        printToOutput("Finished testing shard with all the redis operations");
        
        return SUCCESS;
    }
}

