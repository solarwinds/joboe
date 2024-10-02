package com.tracelytics.test.action;

import java.util.Collections;

import redis.clients.jedis.Client;
import redis.clients.jedis.DebugParams;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisException;
import redis.clients.jedis.SortingParams;
import redis.clients.jedis.Transaction;
import redis.clients.jedis.ZParams;

@SuppressWarnings("serial")
@org.apache.struts2.convention.annotation.Results({
    @org.apache.struts2.convention.annotation.Result(name="success", location="index.jsp"),
    @org.apache.struts2.convention.annotation.Result(name="error", location="index.jsp"),
})
public class TestAll extends AbstractJedisAction {
    @Override
    protected String test(Jedis jedis) throws Exception {
        initialize();
        
        jedis.append(STRING_KEY, STRING_VALUE);
        //jedis.asking(); //NOT SUPPORTED
        
        try {
            jedis.auth("abc");
        } catch (JedisException e) {
            //expected
        }
        //jedis.bgsave();
        
        //blpop
        fillList(jedis, LIST_STRING_KEY, 2); //so the pop will not be blocking due to empty list below
        jedis.blpop(TIMEOUT, LIST_STRING_KEY);
        jedis.blpop(TIMEOUT, LIST_STRING_KEY, LIST_STRING_KEY);
        
        //brpop
        fillList(jedis, LIST_STRING_KEY, 2); //so the pop will not be blocking due to empty list below
        jedis.brpop(TIMEOUT, LIST_STRING_KEY);
        jedis.brpop(TIMEOUT, LIST_STRING_KEY, LIST_STRING_KEY);

        jedis.configGet("*");
        jedis.configSet("tcp-keepalive", jedis.configGet("tcp-keepalive").get(1));
        
        jedis.dbSize();
        
        jedis.disconnect();
        jedis.connect();
        
        jedis.debug(DebugParams.RELOAD());
        jedis.decr(STRING_KEY);
        jedis.decrBy(STRING_KEY, 2);
        
        jedis.echo(STRING_VALUE);
        
        jedis.exists(STRING_KEY);
        
        jedis.expire(STRING_KEY, EXPIRY);
        jedis.expireAt(STRING_KEY, System.currentTimeMillis() / 1000L + EXPIRY);
        
        jedis.get(STRING_KEY);
        jedis.get("BLAH");
        jedis.getSet(STRING_KEY, STRING_VALUE);
        
        jedis.hdel(HASH_STRING_KEY, HASH_FIELD_STRING_KEY);
        jedis.hexists(HASH_STRING_KEY, HASH_FIELD_STRING_KEY);
        jedis.hget(HASH_STRING_KEY, HASH_FIELD_STRING_KEY);
        jedis.hgetAll(HASH_STRING_KEY);
        jedis.hincrBy(HASH_STRING_KEY, HASH_FIELD_STRING_KEY, 1);
        jedis.hkeys(HASH_STRING_KEY);
        jedis.hlen(HASH_STRING_KEY);
        jedis.hmget(HASH_STRING_KEY, HASH_FIELD_STRING_KEY);
        jedis.hmset(HASH_STRING_KEY, Collections.singletonMap(HASH_FIELD_STRING_KEY, STRING_VALUE));
        jedis.hset(HASH_STRING_KEY, HASH_FIELD_STRING_KEY, STRING_VALUE);
        jedis.hsetnx(HASH_STRING_KEY, HASH_FIELD_STRING_KEY, STRING_VALUE);
        jedis.hvals(HASH_STRING_KEY);
        
        jedis.incr(STRING_KEY);
        jedis.incrBy(STRING_KEY, 1);
        jedis.info();
                
        jedis.keys(STRING_KEY);
        
        jedis.lastsave();
        jedis.lindex(LIST_STRING_KEY, 0);
        jedis.linsert(LIST_STRING_KEY, Client.LIST_POSITION.BEFORE, STRING_VALUE, STRING_VALUE);
        jedis.llen(LIST_STRING_KEY);
        jedis.lpop(LIST_STRING_KEY);
        jedis.lpush(LIST_STRING_KEY, STRING_VALUE);
        jedis.lpushx(LIST_STRING_KEY, STRING_VALUE);
        jedis.lrange(LIST_STRING_KEY, 0, 1);
        jedis.lrem(LIST_STRING_KEY, 1, STRING_VALUE);
        jedis.lset(LIST_STRING_KEY, 0, STRING_VALUE);
        jedis.ltrim(LIST_STRING_KEY, 0, 1);
        
//        jedis.monitor(new JedisMonitor() { public void onCommand(String command) {}});
        jedis.mget(STRING_KEY);
        jedis.move(STRING_KEY, 1);
        jedis.mset(STRING_KEY, STRING_VALUE);
        jedis.msetnx(STRING_KEY, STRING_VALUE);
        
        
        Transaction transaction = jedis.multi();
        transaction.set(STRING_KEY, STRING_VALUE);
        transaction.exec();
        
        jedis.persist(STRING_KEY);
        jedis.ping();
        
        jedis.randomKey();
        jedis.rename(STRING_KEY, STRING_KEY_2);
        jedis.renamenx(STRING_KEY_2, STRING_KEY);
        jedis.rpop(LIST_STRING_KEY);
        jedis.rpoplpush(LIST_STRING_KEY, LIST_STRING_KEY);
        jedis.rpush(LIST_STRING_KEY, STRING_VALUE);
        jedis.rpushx(LIST_STRING_KEY, STRING_VALUE);
        
        jedis.sadd(SET_STRING_KEY, STRING_VALUE);
        jedis.save();
        jedis.scard(SET_STRING_KEY);
        jedis.sdiff(SET_STRING_KEY);
        jedis.sdiffstore(SET_STRING_KEY, SET_STRING_KEY);
        jedis.select(0);
        
        jedis.set(STRING_KEY, STRING_VALUE);
        jedis.setex(STRING_KEY, EXPIRY, STRING_VALUE);
        jedis.setnx(STRING_KEY, STRING_VALUE);
        
        jedis.sinter(SET_STRING_KEY);
        jedis.sinterstore(SET_STRING_KEY, SET_STRING_KEY);
        jedis.sismember(SET_STRING_KEY, STRING_VALUE);
        jedis.slaveof(REDIS_HOST, REDIS_PORT);
        jedis.slaveofNoOne();
        jedis.smembers(SET_STRING_KEY);
        jedis.smove(SET_STRING_KEY, SET_STRING_KEY, STRING_VALUE);
        jedis.sort(SET_STRING_KEY);
        jedis.sort(SET_STRING_KEY, LIST_STRING_KEY);
        jedis.sort(SET_STRING_KEY, new SortingParams());
        jedis.sort(SET_STRING_KEY, new SortingParams(), LIST_STRING_KEY);
        
        jedis.spop(SET_STRING_KEY);
        jedis.srandmember(SET_STRING_KEY);
        jedis.srem(SET_STRING_KEY, STRING_VALUE);
        
        jedis.strlen(STRING_KEY);
        jedis.substr(STRING_KEY, 0, 1);
        jedis.sunion(SET_STRING_KEY, SET_STRING_KEY);
        jedis.sunionstore(SET_STRING_KEY, SET_STRING_KEY, SET_STRING_KEY);
//        jedis.sync();
        
        jedis.ttl(STRING_KEY);
        jedis.type(STRING_KEY);
        
        jedis.watch(STRING_KEY);
        jedis.unwatch();
        
        jedis.zadd(ZSET_STRING_KEY, 1, STRING_VALUE);
        jedis.zcard(ZSET_STRING_KEY);
        jedis.zcount(ZSET_STRING_KEY, 0, 1);
        jedis.zincrby(ZSET_STRING_KEY, 1, STRING_VALUE);
        jedis.zinterstore(ZSET_STRING_KEY, ZSET_STRING_KEY, ZSET_STRING_KEY);
        jedis.zinterstore(ZSET_STRING_KEY, new ZParams(), ZSET_STRING_KEY, ZSET_STRING_KEY);
        jedis.zrange(ZSET_STRING_KEY, 0, 1);
        jedis.zrangeByScore(ZSET_STRING_KEY, 0, 1);
        jedis.zrangeByScore(ZSET_STRING_KEY, 0, 1, 0, 1);
        jedis.zrangeByScoreWithScores(ZSET_STRING_KEY, 0, 1);
        jedis.zrangeByScoreWithScores(ZSET_STRING_KEY, 0, 1, 0, 1);
        jedis.zrangeWithScores(ZSET_STRING_KEY, 0, 1);
        jedis.zrank(ZSET_STRING_KEY, STRING_VALUE);
        jedis.zrem(ZSET_STRING_KEY, STRING_VALUE);
        jedis.zremrangeByRank(ZSET_STRING_KEY, 0, 1);
        jedis.zremrangeByScore(ZSET_STRING_KEY, 0, 1);
        jedis.zrevrangeWithScores(ZSET_STRING_KEY, 0, 1);
        
        jedis.zadd(ZSET_STRING_KEY, 1, STRING_VALUE);
        jedis.zrevrank(ZSET_STRING_KEY, STRING_VALUE); //jedis bug, not found value triggers exception
        jedis.zscore(ZSET_STRING_KEY, STRING_VALUE);
        jedis.zunionstore(SET_STRING_KEY, SET_STRING_KEY, SET_STRING_KEY);
        jedis.zunionstore(SET_STRING_KEY, new ZParams(), SET_STRING_KEY, SET_STRING_KEY);
        
        jedis.del(STRING_KEY);
        jedis.del(STRING_KEY, STRING_KEY);
        
        jedis.flushAll();
        jedis.flushDB();
        
        //jedis.shutdown();
        
        printToOutput("Finished calling all the redis operations");
        
        return SUCCESS;
    }

    private void fillList(Jedis jedis, Object key, int count) {
        for (int i = 0 ; i < count; i++) {
            jedis.rpush((String)key, STRING_VALUE);
        }
    }
}

