package com.tracelytics.test.action;

import java.util.Collections;

import redis.clients.jedis.BitOP;
import redis.clients.jedis.BitPosParams;
import redis.clients.jedis.Client;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;
import redis.clients.jedis.SortingParams;
import redis.clients.jedis.ZParams;

@SuppressWarnings("serial")
@org.apache.struts2.convention.annotation.Results({
    @org.apache.struts2.convention.annotation.Result(name="success", location="index.jsp"),
    @org.apache.struts2.convention.annotation.Result(name="error", location="index.jsp"),
})
public class TestPipeline extends AbstractJedisAction {
    @Override
    protected String test(Jedis jedis) throws Exception {
        initialize();
        
        Pipeline pipeline = jedis.pipelined();
        pipeline.append(STRING_KEY, STRING_VALUE);
        pipeline.append(BYTE_KEY, BYTE_VALUE);
        
        pipeline.bitcount(BYTE_KEY);
        pipeline.bitcount(STRING_KEY);
        pipeline.bitcount(BYTE_KEY, 0, 1);
        pipeline.bitcount(STRING_KEY, 0, 1);
        pipeline.bitop(BitOP.AND, BYTE_KEY_2, BYTE_KEY, BYTE_KEY);
        pipeline.bitop(BitOP.AND, STRING_KEY_2, STRING_KEY, STRING_KEY);
        pipeline.bitpos(BYTE_KEY, true);
        pipeline.bitpos(STRING_KEY, true);
        pipeline.bitpos(BYTE_KEY, true, new BitPosParams(0));
        pipeline.bitpos(STRING_KEY, true, new BitPosParams(0));        
        
        //blpop
        fillList(pipeline, LIST_BYTE_KEY, 1); //so the pop will not be blocking due to empty list below
        fillList(pipeline, LIST_STRING_KEY, 2); //so the pop will not be blocking due to empty list below
        pipeline.blpop(TIMEOUT, LIST_BYTE_KEY);
        pipeline.blpop(TIMEOUT, LIST_STRING_KEY);
        pipeline.blpop(TIMEOUT, LIST_STRING_KEY, LIST_STRING_KEY);
        
        //brpop
        fillList(pipeline, LIST_BYTE_KEY, 1); //so the pop will not be blocking due to empty list below
        fillList(pipeline, LIST_STRING_KEY, 2); //so the pop will not be blocking due to empty list below
        pipeline.brpop(TIMEOUT, LIST_BYTE_KEY);
        pipeline.brpop(TIMEOUT, LIST_STRING_KEY);
        pipeline.brpop(TIMEOUT, LIST_STRING_KEY, LIST_STRING_KEY);

//        transaction.configGet("*");
        pipeline.configResetStat();
        
        pipeline.dbSize();
        pipeline.decr(BYTE_KEY);
        pipeline.decr(STRING_KEY);
        pipeline.decrBy(BYTE_KEY, 2);
        pipeline.decrBy(STRING_KEY, 2);
        
        pipeline.dump(BYTE_KEY);
        pipeline.dump(STRING_KEY);
        
        pipeline.echo(STRING_VALUE);
        pipeline.echo(BYTE_VALUE);
        
//        transaction.eval("return {}");
//        transaction.eval("return {}", Collections.EMPTY_LIST, Collections.EMPTY_LIST);
        
        pipeline.exists(BYTE_KEY);
        pipeline.exists(STRING_KEY);
        
        pipeline.expire(BYTE_KEY, EXPIRY);
        pipeline.expire(STRING_KEY, EXPIRY);
        pipeline.expireAt(BYTE_KEY, System.currentTimeMillis() / 1000L + EXPIRY);
        pipeline.expireAt(STRING_KEY, System.currentTimeMillis() / 1000L + EXPIRY);
        
        pipeline.get(BYTE_KEY);
        Response<String> response = pipeline.get(STRING_KEY);
        pipeline.get("BLAH");
        pipeline.getbit(BYTE_KEY, 0);
        pipeline.getbit(STRING_KEY, 0);
//        transaction.getrange(BYTE_KEY, 0, 1);
//        transaction.getrange(STRING_KEY, 0, 1);
        pipeline.getSet(BYTE_KEY, BYTE_VALUE);
        pipeline.getSet(STRING_KEY, STRING_VALUE);
        
        pipeline.hdel(HASH_BYTE_KEY, HASH_FIELD_BYTE_KEY);
        pipeline.hdel(HASH_STRING_KEY, HASH_FIELD_STRING_KEY);
        pipeline.hexists(HASH_BYTE_KEY, HASH_FIELD_BYTE_KEY);
        pipeline.hexists(HASH_STRING_KEY, HASH_FIELD_STRING_KEY);
        pipeline.hget(HASH_BYTE_KEY, HASH_FIELD_BYTE_KEY);
        pipeline.hget(HASH_STRING_KEY, HASH_FIELD_STRING_KEY);
        pipeline.hgetAll(HASH_BYTE_KEY);
        pipeline.hgetAll(HASH_STRING_KEY);
        pipeline.hincrBy(HASH_BYTE_KEY, HASH_FIELD_BYTE_KEY, 1);
        pipeline.hincrBy(HASH_STRING_KEY, HASH_FIELD_STRING_KEY, 1);
        pipeline.hincrByFloat(HASH_BYTE_KEY, HASH_FIELD_BYTE_KEY, 1);
        pipeline.hincrByFloat(HASH_STRING_KEY, HASH_FIELD_STRING_KEY, 1);
        pipeline.hkeys(HASH_BYTE_KEY);
        pipeline.hkeys(HASH_STRING_KEY);
        pipeline.hlen(HASH_BYTE_KEY);
        pipeline.hlen(HASH_STRING_KEY);
        pipeline.hmget(HASH_BYTE_KEY, HASH_FIELD_BYTE_KEY);
        pipeline.hmget(HASH_STRING_KEY, HASH_FIELD_STRING_KEY);
        pipeline.hmset(HASH_BYTE_KEY, Collections.singletonMap(HASH_FIELD_BYTE_KEY, BYTE_VALUE));
        pipeline.hmset(HASH_STRING_KEY, Collections.singletonMap(HASH_FIELD_STRING_KEY, STRING_VALUE));
        pipeline.hset(HASH_BYTE_KEY, HASH_FIELD_BYTE_KEY, BYTE_VALUE);
        pipeline.hset(HASH_STRING_KEY, HASH_FIELD_STRING_KEY, STRING_VALUE);
        pipeline.hsetnx(HASH_BYTE_KEY, HASH_FIELD_BYTE_KEY, BYTE_VALUE);
        pipeline.hsetnx(HASH_STRING_KEY, HASH_FIELD_STRING_KEY, STRING_VALUE);
        pipeline.hvals(HASH_BYTE_KEY);
        pipeline.hvals(HASH_STRING_KEY);
        
        pipeline.incr(BYTE_KEY);
        pipeline.incr(STRING_KEY);
        pipeline.incrBy(BYTE_KEY, 1);
        pipeline.incrBy(STRING_KEY, 1);
        pipeline.incrByFloat(BYTE_KEY, 1);
        pipeline.incrByFloat(STRING_KEY, 1);
        pipeline.info();
                
        pipeline.keys(BYTE_KEY);
        pipeline.keys(STRING_KEY);
        
        pipeline.lastsave();
        pipeline.lindex(LIST_BYTE_KEY, 0);
        pipeline.lindex(LIST_STRING_KEY, 0);
        pipeline.linsert(LIST_BYTE_KEY, Client.LIST_POSITION.BEFORE, BYTE_VALUE, BYTE_VALUE);
        pipeline.linsert(LIST_STRING_KEY, Client.LIST_POSITION.BEFORE, STRING_VALUE, STRING_VALUE);
        pipeline.llen(LIST_BYTE_KEY);
        pipeline.llen(LIST_STRING_KEY);
        pipeline.lpop(LIST_BYTE_KEY);
        pipeline.lpop(LIST_STRING_KEY);
        pipeline.lpush(LIST_BYTE_KEY, BYTE_VALUE);
        pipeline.lpush(LIST_STRING_KEY, STRING_VALUE);
        pipeline.lpushx(LIST_BYTE_KEY, BYTE_VALUE);
        pipeline.lpushx(LIST_STRING_KEY, STRING_VALUE);
        pipeline.lrange(LIST_BYTE_KEY, 0, 1);
        pipeline.lrange(LIST_STRING_KEY, 0, 1);
        pipeline.lrem(LIST_BYTE_KEY, 1, BYTE_VALUE);
        pipeline.lrem(LIST_STRING_KEY, 1, STRING_VALUE);
        pipeline.lset(LIST_BYTE_KEY, 0, BYTE_VALUE);
        pipeline.lset(LIST_STRING_KEY, 0, STRING_VALUE);
        pipeline.ltrim(LIST_BYTE_KEY, 0, 1);
        pipeline.ltrim(LIST_STRING_KEY, 0, 1);
        
//        jedis.monitor(new JedisMonitor() { public void onCommand(String command) {}});
//        jedis.migrate(REDIS_HOST, REDIS_PORT, STRING_KEY, 0, Protocol.DEFAULT_TIMEOUT);
        pipeline.mget(BYTE_KEY);
        pipeline.mget(STRING_KEY);
        pipeline.move(BYTE_KEY, 1);
        pipeline.move(STRING_KEY, 1);
        pipeline.mset(BYTE_KEY, BYTE_VALUE);
        pipeline.mset(STRING_KEY, STRING_VALUE);
        pipeline.msetnx(BYTE_KEY, BYTE_VALUE);
        pipeline.msetnx(STRING_KEY, STRING_VALUE);
        
        pipeline.objectEncoding(BYTE_KEY);
        pipeline.objectEncoding(STRING_KEY);
        pipeline.objectIdletime(BYTE_KEY);
        pipeline.objectIdletime(STRING_KEY);
        pipeline.objectRefcount(BYTE_KEY);
        pipeline.objectRefcount(STRING_KEY);
        
        pipeline.persist(BYTE_KEY);
        pipeline.persist(STRING_KEY);
        pipeline.pexpire(BYTE_KEY, EXPIRY);
        pipeline.pexpire(STRING_KEY, EXPIRY);
        pipeline.pexpire(BYTE_KEY, EXPIRY * 1000L);
        pipeline.pexpire(STRING_KEY, EXPIRY * 1000L);
        pipeline.pexpireAt(BYTE_KEY, System.currentTimeMillis() / 1000L + EXPIRY);
        pipeline.pexpireAt(STRING_KEY, System.currentTimeMillis() / 1000L + EXPIRY);
        pipeline.pfadd(LOG_BYTE_KEY, BYTE_VALUE);
        pipeline.pfadd(LOG_STRING_KEY, STRING_VALUE);
        pipeline.pfcount(LOG_BYTE_KEY);
        pipeline.pfcount(LOG_BYTE_KEY, LOG_BYTE_KEY);
        pipeline.pfcount(LOG_STRING_KEY);
        pipeline.pfcount(LOG_STRING_KEY, LOG_STRING_KEY);
        pipeline.pfmerge(LOG_BYTE_KEY, LOG_BYTE_KEY, LOG_BYTE_KEY);
        pipeline.pfmerge(LOG_STRING_KEY, LOG_STRING_KEY, LOG_STRING_KEY);
        pipeline.ping();
//        jedis.pipelined().set(STRING_KEY, STRING_VALUE);
        pipeline.psetex(BYTE_KEY, EXPIRY, BYTE_VALUE);
        pipeline.psetex(STRING_KEY, EXPIRY, STRING_VALUE);
        
        //jedis.psubscribe(new DummyBinaryPubSub(), "*".getBytes());
//        jedis.psubscribe(new DummyPubSub(), "*");
        pipeline.pttl(BYTE_KEY);
        pipeline.pttl(STRING_KEY);
//        jedis.publish("test-channel".getBytes(), BYTE_VALUE);
//        jedis.publish("test-channel", STRING_VALUE);
//        jedis.pubsubChannels("*");
//        jedis.pubsubNumPat();
//        jedis.pubsubNumSub("test-channel");
        
        pipeline.randomKey();
        pipeline.rename(BYTE_KEY, BYTE_KEY_2);
        pipeline.rename(STRING_KEY, STRING_KEY_2);
        pipeline.renamenx(BYTE_KEY_2, BYTE_KEY);
        pipeline.renamenx(STRING_KEY_2, STRING_KEY);
        pipeline.rpop(LIST_BYTE_KEY);
        pipeline.rpop(LIST_STRING_KEY);
        pipeline.rpoplpush(LIST_BYTE_KEY, LIST_BYTE_KEY);
        pipeline.rpoplpush(LIST_STRING_KEY, LIST_STRING_KEY);
        pipeline.rpush(LIST_BYTE_KEY, BYTE_VALUE);
        pipeline.rpush(LIST_STRING_KEY, STRING_VALUE);
        pipeline.rpushx(LIST_BYTE_KEY, BYTE_VALUE);
        pipeline.rpushx(LIST_STRING_KEY, STRING_VALUE);
        
        pipeline.sadd(SET_BYTE_KEY, BYTE_VALUE);
        pipeline.sadd(SET_STRING_KEY, STRING_VALUE);
        pipeline.save();
        pipeline.scard(SET_BYTE_KEY);
        pipeline.scard(SET_STRING_KEY);
        
        pipeline.sdiff(SET_BYTE_KEY);
        pipeline.sdiff(SET_STRING_KEY);
        pipeline.sdiffstore(SET_BYTE_KEY, SET_BYTE_KEY);
        pipeline.sdiffstore(SET_STRING_KEY, SET_STRING_KEY);
        pipeline.select(0);
        
        pipeline.set(BYTE_KEY, BYTE_VALUE);
        pipeline.set(STRING_KEY, STRING_VALUE);
        pipeline.set(BYTE_KEY, BYTE_VALUE, "NX".getBytes());
        pipeline.set(STRING_KEY, STRING_VALUE, "NX");
        pipeline.set(BYTE_KEY, BYTE_VALUE, "NX".getBytes(), "EX".getBytes(), EXPIRY);
        pipeline.set(STRING_KEY, STRING_VALUE, "NX", "EX", EXPIRY);
        pipeline.setbit(BYTE_KEY, 0, new byte[] { 0 });
        pipeline.setbit(STRING_KEY, 0, true);
        pipeline.setex(BYTE_KEY, EXPIRY, BYTE_VALUE);
        pipeline.setex(STRING_KEY, EXPIRY, STRING_VALUE);
        pipeline.setnx(BYTE_KEY, BYTE_VALUE);
        pipeline.setnx(STRING_KEY, STRING_VALUE);
        pipeline.setrange(BYTE_KEY, 0, BYTE_VALUE);
        pipeline.setrange(STRING_KEY, 0, STRING_VALUE);
        
        pipeline.sinter(SET_BYTE_KEY);
        pipeline.sinter(SET_STRING_KEY);
        pipeline.sinterstore(SET_BYTE_KEY, SET_BYTE_KEY);
        pipeline.sinterstore(SET_STRING_KEY, SET_STRING_KEY);
        pipeline.sismember(SET_BYTE_KEY, BYTE_VALUE);
        pipeline.sismember(SET_STRING_KEY, STRING_VALUE);
        pipeline.smembers(SET_BYTE_KEY);
        pipeline.smembers(SET_STRING_KEY);
        pipeline.smove(SET_BYTE_KEY, SET_BYTE_KEY, BYTE_VALUE);
        pipeline.smove(SET_STRING_KEY, SET_STRING_KEY, STRING_VALUE);
        pipeline.sort(SET_BYTE_KEY);
        pipeline.sort(SET_STRING_KEY);
        pipeline.sort(SET_BYTE_KEY, LIST_BYTE_KEY);
        pipeline.sort(SET_STRING_KEY, LIST_STRING_KEY);
        pipeline.sort(SET_BYTE_KEY, new SortingParams());
        pipeline.sort(SET_STRING_KEY, new SortingParams());
        pipeline.sort(SET_BYTE_KEY, new SortingParams(), LIST_BYTE_KEY);
        pipeline.sort(SET_STRING_KEY, new SortingParams(), LIST_STRING_KEY);
        
        pipeline.spop(SET_BYTE_KEY);
        pipeline.spop(SET_STRING_KEY);
        pipeline.srandmember(SET_BYTE_KEY);
        pipeline.srandmember(SET_STRING_KEY);
        pipeline.srandmember(SET_BYTE_KEY, 1);
        pipeline.srandmember(SET_STRING_KEY, 1);
        pipeline.srem(SET_BYTE_KEY, BYTE_VALUE);
        pipeline.srem(SET_STRING_KEY, STRING_VALUE);
        
        pipeline.strlen(BYTE_KEY);
        pipeline.strlen(STRING_KEY);
        pipeline.substr(BYTE_KEY, 0, 1);
        pipeline.substr(STRING_KEY, 0, 1);
        pipeline.sunion(SET_BYTE_KEY, SET_BYTE_KEY);
        pipeline.sunion(SET_STRING_KEY, SET_STRING_KEY);
        pipeline.sunionstore(SET_BYTE_KEY, SET_BYTE_KEY, SET_BYTE_KEY);
        pipeline.sunionstore(SET_STRING_KEY, SET_STRING_KEY, SET_STRING_KEY);
        
        pipeline.time();
        pipeline.ttl(BYTE_KEY);
        pipeline.ttl(STRING_KEY);
        pipeline.type(BYTE_KEY);
        pipeline.type(STRING_KEY);
        
        pipeline.watch(BYTE_KEY);
        pipeline.watch(STRING_KEY);
        
        pipeline.zadd(ZSET_STRING_KEY, Collections.singletonMap(STRING_VALUE, (double)1));
        pipeline.zadd(ZSET_STRING_KEY, Collections.singletonMap(STRING_VALUE, (double)1));
        pipeline.zadd(ZSET_BYTE_KEY, 1, BYTE_VALUE);
        pipeline.zadd(ZSET_STRING_KEY, 1, STRING_VALUE);
        pipeline.zcard(ZSET_BYTE_KEY);
        pipeline.zcard(ZSET_STRING_KEY);
        pipeline.zcount(ZSET_STRING_KEY, STRING_VALUE, STRING_VALUE);
        pipeline.zcount(ZSET_BYTE_KEY, 0, 1);
        pipeline.zcount(ZSET_STRING_KEY, 0, 1);
        pipeline.zincrby(ZSET_BYTE_KEY, 1, BYTE_VALUE);
        pipeline.zincrby(ZSET_STRING_KEY, 1, STRING_VALUE);
        pipeline.zinterstore(ZSET_BYTE_KEY, ZSET_BYTE_KEY, ZSET_BYTE_KEY);
        pipeline.zinterstore(ZSET_STRING_KEY, ZSET_STRING_KEY, ZSET_STRING_KEY);
        pipeline.zinterstore(ZSET_BYTE_KEY, new ZParams(), ZSET_BYTE_KEY, ZSET_BYTE_KEY);
        pipeline.zinterstore(ZSET_STRING_KEY, new ZParams(), ZSET_STRING_KEY, ZSET_STRING_KEY);
        pipeline.zlexcount(ZSET_BYTE_KEY, "-".getBytes(), "+".getBytes());
        pipeline.zlexcount(ZSET_STRING_KEY, "-", "+");
        pipeline.zrange(ZSET_BYTE_KEY, 0, 1);
        pipeline.zrange(ZSET_STRING_KEY, 0, 1);
        pipeline.zrangeByLex(ZSET_BYTE_KEY, "-".getBytes(), "+".getBytes());
        pipeline.zrangeByLex(ZSET_STRING_KEY, "-", "+");
        pipeline.zrangeByLex(ZSET_BYTE_KEY, "-".getBytes(), "+".getBytes(), 0, 1);
        pipeline.zrangeByLex(ZSET_STRING_KEY, "-", "+", 0, 1);
        pipeline.zrangeByScore(ZSET_BYTE_KEY, BYTE_VALUE, BYTE_VALUE);
        pipeline.zrangeByScore(ZSET_BYTE_KEY, 0, 1);
        pipeline.zrangeByScore(ZSET_STRING_KEY, STRING_VALUE, STRING_VALUE);
        pipeline.zrangeByScore(ZSET_STRING_KEY, 0, 1);
        pipeline.zrangeByScore(ZSET_BYTE_KEY, BYTE_VALUE, BYTE_VALUE, 0, 1);
        pipeline.zrangeByScore(ZSET_BYTE_KEY, 0, 1, 0, 1);
        pipeline.zrangeByScore(ZSET_STRING_KEY, STRING_VALUE, STRING_VALUE, 0, 1);
        pipeline.zrangeByScore(ZSET_STRING_KEY, 0, 1, 0, 1);
        pipeline.zrangeByScoreWithScores(ZSET_BYTE_KEY, BYTE_VALUE, BYTE_VALUE);
        pipeline.zrangeByScoreWithScores(ZSET_BYTE_KEY, 0, 1);
        pipeline.zrangeByScoreWithScores(ZSET_STRING_KEY, STRING_VALUE, STRING_VALUE);
        pipeline.zrangeByScoreWithScores(ZSET_STRING_KEY, 0, 1);
        pipeline.zrangeByScoreWithScores(ZSET_BYTE_KEY, BYTE_VALUE, BYTE_VALUE, 0, 1);
        pipeline.zrangeByScoreWithScores(ZSET_BYTE_KEY, 0, 1, 0, 1);
        pipeline.zrangeByScoreWithScores(ZSET_STRING_KEY, STRING_VALUE, STRING_VALUE, 0, 1);
        pipeline.zrangeByScoreWithScores(ZSET_STRING_KEY, 0, 1, 0, 1);
        pipeline.zrangeWithScores(ZSET_BYTE_KEY, 0, 1);
        pipeline.zrangeWithScores(ZSET_STRING_KEY, 0, 1);
        pipeline.zrank(ZSET_BYTE_KEY, BYTE_VALUE);
        pipeline.zrank(ZSET_STRING_KEY, STRING_VALUE);
        pipeline.zrem(ZSET_BYTE_KEY, BYTE_VALUE);
        pipeline.zrem(ZSET_STRING_KEY, STRING_VALUE);
        pipeline.zremrangeByLex(ZSET_BYTE_KEY, "-".getBytes(), "+".getBytes());
        pipeline.zremrangeByLex(ZSET_STRING_KEY, "-", "+");
        pipeline.zremrangeByRank(ZSET_BYTE_KEY, 0, 1);
        pipeline.zremrangeByRank(ZSET_STRING_KEY, 0, 1);
        pipeline.zremrangeByScore(ZSET_BYTE_KEY, BYTE_VALUE, BYTE_VALUE);
        pipeline.zremrangeByScore(ZSET_BYTE_KEY, 0, 1);
        pipeline.zremrangeByScore(ZSET_STRING_KEY, STRING_VALUE, STRING_VALUE);
        pipeline.zremrangeByScore(ZSET_STRING_KEY, 0, 1);
        pipeline.zrevrangeByScore(ZSET_BYTE_KEY, BYTE_VALUE, BYTE_VALUE);
        pipeline.zrevrangeByScore(ZSET_BYTE_KEY, 0, 1);
        pipeline.zrevrangeByScore(ZSET_STRING_KEY, STRING_VALUE, STRING_VALUE);
        pipeline.zrevrangeByScore(ZSET_STRING_KEY, 0, 1);
        pipeline.zrevrangeByScore(ZSET_BYTE_KEY, BYTE_VALUE, BYTE_VALUE, 0, 1);
        pipeline.zrevrangeByScore(ZSET_BYTE_KEY, 0, 1, 0, 1);
        pipeline.zrevrangeByScore(ZSET_STRING_KEY, STRING_VALUE, STRING_VALUE, 0, 1);
        pipeline.zrevrangeByScore(ZSET_STRING_KEY, 0, 1, 0, 1);
        pipeline.zrevrangeByScoreWithScores(ZSET_BYTE_KEY, BYTE_VALUE, BYTE_VALUE);
        pipeline.zrevrangeByScoreWithScores(ZSET_BYTE_KEY, 0, 1);
        pipeline.zrevrangeByScoreWithScores(ZSET_STRING_KEY, STRING_VALUE, STRING_VALUE);
        pipeline.zrevrangeByScoreWithScores(ZSET_STRING_KEY, 0, 1);
        pipeline.zrevrangeByScoreWithScores(ZSET_BYTE_KEY, BYTE_VALUE, BYTE_VALUE, 0, 1);
        pipeline.zrevrangeByScoreWithScores(ZSET_BYTE_KEY, 0, 1, 0, 1);
        pipeline.zrevrangeByScoreWithScores(ZSET_STRING_KEY, STRING_VALUE, STRING_VALUE, 0, 1);
        pipeline.zrevrangeByScoreWithScores(ZSET_STRING_KEY, 0, 1, 0, 1);
        pipeline.zrevrangeWithScores(ZSET_BYTE_KEY, 0, 1);
        pipeline.zrevrangeWithScores(ZSET_STRING_KEY, 0, 1);
        pipeline.zrevrank(ZSET_BYTE_KEY, BYTE_VALUE);
        pipeline.zrevrank(ZSET_STRING_KEY, STRING_VALUE);
        
        pipeline.zscore(ZSET_BYTE_KEY, BYTE_VALUE);
        pipeline.zscore(ZSET_STRING_KEY, STRING_VALUE);
        
        pipeline.zunionstore(SET_BYTE_KEY, SET_BYTE_KEY, SET_BYTE_KEY);
        pipeline.zunionstore(SET_STRING_KEY, SET_STRING_KEY, SET_STRING_KEY);
        pipeline.zunionstore(SET_BYTE_KEY, new ZParams(), SET_BYTE_KEY);
        pipeline.zunionstore(SET_STRING_KEY, new ZParams(), SET_STRING_KEY, SET_STRING_KEY);
        
        pipeline.del(BYTE_KEY);
        pipeline.del(BYTE_KEY, BYTE_KEY);
        pipeline.del(STRING_KEY);
        pipeline.del(STRING_KEY, STRING_KEY);
        
        pipeline.flushAll();
        pipeline.flushDB();
        
        pipeline.sync();
        
        pipeline = jedis.pipelined();
        pipeline.get(STRING_KEY);
        pipeline.get(BYTE_KEY);
        pipeline.syncAndReturnAll();
        
        printToOutput("Finished calling pipeline with all the redis operations");
        
        return SUCCESS;
    }

    private void fillList(Pipeline jedis, Object key, int count) {
        for (int i = 0 ; i < count; i++) {
            if (key instanceof String) {
                jedis.rpush((String)key, STRING_VALUE);
            } else if (key instanceof byte[]) {
                jedis.rpush((byte[])key, BYTE_VALUE);
            }
        }
    }
}

