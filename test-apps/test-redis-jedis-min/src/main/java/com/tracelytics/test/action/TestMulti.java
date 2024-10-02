package com.tracelytics.test.action;

import java.util.Collections;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.SortingParams;
import redis.clients.jedis.Transaction;

@SuppressWarnings("serial")
@org.apache.struts2.convention.annotation.Results({
    @org.apache.struts2.convention.annotation.Result(name="success", location="index.jsp"),
    @org.apache.struts2.convention.annotation.Result(name="error", location="index.jsp"),
})
public class TestMulti extends AbstractJedisAction {
    @Override
    protected String test(Jedis jedisO) throws Exception {
//        initialize();
        
        Transaction transaction = jedisO.multi();
        transaction.append(STRING_KEY, STRING_VALUE);
        
        
        //blpop
        fillList(transaction, LIST_STRING_KEY, 2); //so the pop will not be blocking due to empty list below
        
        //brpop
        fillList(transaction, LIST_STRING_KEY, 2); //so the pop will not be blocking due to empty list below
        
        transaction.dbSize();
        transaction.decr(STRING_KEY);
        transaction.decrBy(STRING_KEY, 2);
        
        
        transaction.exists(STRING_KEY);
        
        transaction.expire(STRING_KEY, EXPIRY);
        transaction.expireAt(STRING_KEY, System.currentTimeMillis() / 1000L + EXPIRY);
        
        transaction.get(STRING_KEY);
        transaction.get("BLAH");
        transaction.getSet(STRING_KEY, STRING_VALUE);
        
        transaction.hdel(HASH_STRING_KEY, HASH_FIELD_STRING_KEY);
        transaction.hexists(HASH_STRING_KEY, HASH_FIELD_STRING_KEY);
        transaction.hget(HASH_STRING_KEY, HASH_FIELD_STRING_KEY);
        transaction.hgetAll(HASH_STRING_KEY);
        transaction.hincrBy(HASH_STRING_KEY, HASH_FIELD_STRING_KEY, 1);
        transaction.hkeys(HASH_STRING_KEY);
        transaction.hlen(HASH_STRING_KEY);
        transaction.hmget(HASH_STRING_KEY, HASH_FIELD_STRING_KEY);
        transaction.hmset(HASH_STRING_KEY, Collections.singletonMap(HASH_FIELD_STRING_KEY, STRING_VALUE));
        transaction.hset(HASH_STRING_KEY, HASH_FIELD_STRING_KEY, STRING_VALUE);
        transaction.hsetnx(HASH_STRING_KEY, HASH_FIELD_STRING_KEY, STRING_VALUE);
        transaction.hvals(HASH_STRING_KEY);
        
        transaction.incr(STRING_KEY);
        transaction.incrBy(STRING_KEY, 1);
                
        transaction.keys(STRING_KEY);
        
        transaction.lindex(LIST_STRING_KEY, 0);
        transaction.llen(LIST_STRING_KEY);
        transaction.lpop(LIST_STRING_KEY);
        transaction.lpush(LIST_STRING_KEY, STRING_VALUE);
        transaction.lrange(LIST_STRING_KEY, 0, 1);
        transaction.lrem(LIST_STRING_KEY, 1, STRING_VALUE);
        transaction.lset(LIST_STRING_KEY, 0, STRING_VALUE);
        transaction.ltrim(LIST_STRING_KEY, 0, 1);
        
        transaction.mget(STRING_KEY);
        transaction.move(STRING_KEY, 1);
        transaction.mset(STRING_KEY, STRING_VALUE);
        transaction.msetnx(STRING_KEY, STRING_VALUE);
        
        transaction.ping();
        
        transaction.randomKey();
        transaction.rename(STRING_KEY, STRING_KEY_2);
        transaction.renamenx(STRING_KEY_2, STRING_KEY);
        transaction.rpop(LIST_STRING_KEY);
        transaction.rpoplpush(LIST_STRING_KEY, LIST_STRING_KEY);
        transaction.rpush(LIST_STRING_KEY, STRING_VALUE);
        
        transaction.sadd(SET_STRING_KEY, STRING_VALUE);
        transaction.scard(SET_STRING_KEY);
        
        transaction.sdiff(SET_STRING_KEY);
        transaction.sdiffstore(SET_STRING_KEY, SET_STRING_KEY);
        transaction.select(0);
        
        transaction.set(STRING_KEY, STRING_VALUE);
        transaction.setex(STRING_KEY, EXPIRY, STRING_VALUE);
        transaction.setnx(STRING_KEY, STRING_VALUE);
        
        transaction.sinter(SET_STRING_KEY);
        transaction.sinterstore(SET_STRING_KEY, SET_STRING_KEY);
        transaction.sismember(SET_STRING_KEY, STRING_VALUE);
        transaction.smembers(SET_STRING_KEY);
        transaction.smove(SET_STRING_KEY, SET_STRING_KEY, STRING_VALUE);
        transaction.sort(SET_STRING_KEY);
        transaction.sort(SET_STRING_KEY, new SortingParams());
        
        transaction.spop(SET_STRING_KEY);
        transaction.srandmember(SET_STRING_KEY);
        transaction.srem(SET_STRING_KEY, STRING_VALUE);
        
        transaction.substr(STRING_KEY, 0, 1);
        transaction.sunion(SET_STRING_KEY, SET_STRING_KEY);
        transaction.sunionstore(SET_STRING_KEY, SET_STRING_KEY, SET_STRING_KEY);
        
        transaction.ttl(STRING_KEY);
        transaction.type(STRING_KEY);
        
        transaction.exec();
        
        transaction = jedisO.multi();
        transaction.zadd(ZSET_STRING_KEY, 1, STRING_VALUE);
        transaction.zcard(ZSET_STRING_KEY);
        transaction.zincrby(ZSET_STRING_KEY, 1, STRING_VALUE);
        transaction.zrange(ZSET_STRING_KEY, 0, 1);
        transaction.zrangeWithScores(ZSET_STRING_KEY, 0, 1);
        transaction.zrank(ZSET_STRING_KEY, STRING_VALUE);
        transaction.zrem(ZSET_STRING_KEY, STRING_VALUE);
        transaction.zrevrangeWithScores(ZSET_STRING_KEY, 0, 1);
        transaction.zrevrank(ZSET_STRING_KEY, STRING_VALUE);
        
        transaction.zscore(ZSET_STRING_KEY, STRING_VALUE);
        
        transaction.del(STRING_KEY);
        transaction.del(STRING_KEY, STRING_KEY);
        
        transaction.flushAll();
        transaction.flushDB();
        
        //jedis.shutdown();
        
        transaction.exec();
        
        transaction = jedisO.multi();
        transaction.set(STRING_KEY, STRING_VALUE);
        transaction.discard();
        
        
        printToOutput("Finished calling multi with all the redis operations");
        
        return SUCCESS;
    }

    private void fillList(Transaction jedis, Object key, int count) {
        for (int i = 0 ; i < count; i++) {
            if (key instanceof String) {
                jedis.rpush((String)key, STRING_VALUE);
            }
        }
        
    }
}

