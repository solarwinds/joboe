package com.tracelytics.test.action;

import java.util.Collections;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisConnection;
import com.lambdaworks.redis.RedisException;
import com.lambdaworks.redis.ScriptOutputType;
import com.lambdaworks.redis.SortArgs;
import com.lambdaworks.redis.ZStoreArgs;

@SuppressWarnings("serial")
@org.apache.struts2.convention.annotation.Results({
    @org.apache.struts2.convention.annotation.Result(name="success", location="index.jsp"),
    @org.apache.struts2.convention.annotation.Result(name="error", location="index.jsp"),
})
public class TestSync extends AbstractRedissonAction {
    @Override
    protected String test() throws Exception {
        RedisClient client = new RedisClient(REDIS_HOST);
        RedisConnection<String, String> connection = client.connect();
        
//        connection.set(KEY, VALUE);
//        connection.get(KEY);
//               
//        connection.del("random-list");
//        connection.lpush("random-list", LIST);
//        connection.sortStore("random-list", SortArgs.Builder.asc(), "new-list");
        
        connection.append(KEY, VALUE);
        try {
            connection.auth("");
        } catch (RedisException e) {
            //expected;
        }
        
        //connection.bgrewriteaof();
        //connection.bgsave();
        
        connection.bitcount(KEY);
        connection.bitcount(KEY, 0, 1);
        connection.bitopAnd(KEY_2, KEY, KEY);
        connection.bitopNot(KEY_2, KEY);
        connection.bitopOr(KEY_2, KEY, KEY);
        connection.bitopXor(KEY_2, KEY, KEY);
        
        connection.lpush(LIST_KEY, VALUE);
        connection.blpop(TIMEOUT, LIST_KEY);
        connection.lpush(LIST_KEY, VALUE);
        connection.brpop(TIMEOUT, LIST_KEY);
        
        connection.lpush(LIST_KEY, VALUE);
        connection.brpoplpush(TIMEOUT, LIST_KEY, LIST_KEY);
        
        connection.clientGetname();
        //connection.clientKill(addr);
        connection.clientList();
        connection.clientSetname("test-client");
        
        connection.configResetstat();
        connection.configSet("timeout", connection.configGet("timeout").get(1));
        
        connection.dbsize();
        connection.debugObject(KEY);
        connection.set(KEY, VALUE);
        connection.decr(KEY);
        connection.decrby(KEY, 1);
        connection.del(KEY_2);
        
        connection.restore(KEY_2, EXPIRY, connection.dump(KEY));
        
        connection.echo("hi");
        
        connection.eval("return { KEYS[1] }", ScriptOutputType.VALUE, "1");

        String stringSha = connection.scriptLoad("return {}");
        connection.evalsha(stringSha, ScriptOutputType.VALUE);
        
        connection.exists(KEY);
        connection.expire(KEY, EXPIRY);
        connection.expireat(KEY, new Date(System.currentTimeMillis() + EXPIRY));
        connection.expireat(KEY, System.currentTimeMillis() + EXPIRY);
        
        //connection.flushall();
        //connection.flushdb();
        
        connection.get(KEY);
        //connection.getAsync().get(KEY);
        
        connection.getbit(KEY, 0);
        connection.getrange(KEY, 0, 1);
        connection.getset(KEY, VALUE);
        
        connection.hdel(HASH_KEY, HASH_FIELD_KEY);
        connection.hexists(HASH_KEY, HASH_FIELD_KEY);
        connection.hget(HASH_KEY, HASH_FIELD_KEY);
        connection.hgetall(HASH_KEY);
        connection.hincrby(HASH_KEY, HASH_FIELD_KEY, 1);
        connection.hincrbyfloat(HASH_KEY, HASH_FIELD_KEY, 1.0);
        connection.hkeys(HASH_KEY);
        connection.hlen(HASH_KEY);
        connection.hmget(HASH_KEY, HASH_FIELD_KEY);
        connection.hmset(HASH_KEY, Collections.singletonMap(HASH_FIELD_KEY, VALUE));
        connection.hset(HASH_KEY, HASH_FIELD_KEY, VALUE);
        connection.hsetnx(HASH_KEY, HASH_FIELD_KEY, VALUE);
        connection.hvals(HASH_KEY);
        
        connection.incr(KEY);
        connection.incrby(KEY, 1);
        connection.incrbyfloat(KEY, 1.0);
        
        connection.info();
        connection.info("server");
        
        connection.keys("*");
        
        connection.lastsave();
        connection.lindex(LIST_KEY, 0);
        connection.linsert(LIST_KEY, false, KEY, VALUE);
        connection.llen(LIST_KEY);
        connection.lpop(LIST_KEY);
        connection.lpush(LIST_KEY, VALUE);
        connection.lpushx(LIST_KEY, VALUE);
        connection.lrange(LIST_KEY, 0, 1);
        connection.lrem(LIST_KEY, 0, VALUE);
        connection.lpush(LIST_KEY, VALUE);
        connection.lset(LIST_KEY, 0, VALUE);
        connection.ltrim(LIST_KEY, 0, 1);
        
        connection.mget(KEY, KEY_2);
        //connection.migrate(host, port, key, db, timeout);
        connection.move(KEY, 1);
        connection.mset(Collections.singletonMap(KEY, VALUE));
        connection.msetnx(Collections.singletonMap(KEY, VALUE));
        
        connection.objectEncoding(KEY);
        connection.objectIdletime(KEY);
        connection.objectRefcount(KEY);
        
        connection.persist(KEY);
        connection.pexpire(KEY, EXPIRY);
        connection.pexpireat(KEY, new Date(System.currentTimeMillis() + EXPIRY));
        connection.pexpireat(KEY, System.currentTimeMillis() + EXPIRY);
        connection.ping();
        connection.pttl(KEY);
        
        connection.randomkey();
        connection.set(KEY_2, VALUE);
        connection.rename(KEY_2, "new-key");
        connection.renamenx("new-key", KEY_2);
        connection.rpop(LIST_KEY);
        connection.rpoplpush(LIST_KEY, LIST_KEY);
        connection.rpush(LIST_KEY, VALUE);
        connection.rpushx(LIST_KEY, VALUE);
        
        connection.sadd(SET_KEY, VALUE, VALUE);
        connection.save();
        connection.scard(SET_KEY);
        connection.scriptExists(stringSha);
        connection.scriptFlush();
        try {
            connection.scriptKill();
        } catch (RedisException e) {
            //expected;
        }
        connection.scriptLoad("return {}");
        connection.sdiff(SET_KEY, SET_KEY);
        connection.sdiffstore(SET_KEY, SET_KEY, SET_KEY);
        connection.select(0);
        connection.set(KEY, VALUE);
        connection.setbit(KEY, 0, 1);
        connection.setex(KEY, EXPIRY, VALUE);
        connection.setnx(KEY, VALUE);
        connection.setrange(KEY, 0, VALUE);
        connection.setTimeout(EXPIRY, TimeUnit.SECONDS);
        
        //connection.shutdown(false);
        connection.sinter(SET_KEY, SET_KEY);
        connection.sinterstore(SET_KEY, SET_KEY, SET_KEY);
        connection.sismember(SET_KEY, KEY);
        connection.slaveof(REDIS_HOST, Integer.valueOf(REDIS_PORT));
        connection.slaveofNoOne();
        connection.slowlogGet();
        connection.slowlogGet(0);
        connection.slowlogLen();
        connection.slowlogReset();
        connection.smembers(SET_KEY);
        connection.smove(SET_KEY, SET_KEY, KEY);
        connection.sort(SET_KEY);
        connection.sort(SET_KEY, SortArgs.Builder.asc());
        connection.sortStore(SET_KEY, SortArgs.Builder.asc(), SET_KEY);
        connection.spop(SET_KEY);
        connection.srandmember(SET_KEY);
        connection.srandmember(SET_KEY, 1);
        connection.srem(SET_KEY, KEY);
        connection.sadd("num-set", "1");
//        connection.sscan("num-set", 1);
        connection.strlen(KEY);
        connection.sunion(SET_KEY, SET_KEY);
        connection.sunionstore(SET_KEY, SET_KEY, SET_KEY);
        //connection.sync();
        
        connection.ttl(KEY);
        connection.type(KEY);
        
        connection.zadd(ZSET_KEY, 1, "2");
        connection.zcard(ZSET_KEY);
        connection.zcount(ZSET_KEY, VALUE, VALUE);
        connection.zcount(ZSET_KEY, 0, 1);
        connection.zincrby(ZSET_KEY, 1, VALUE);
        connection.zinterstore(ZSET_KEY, ZSET_KEY, ZSET_KEY);
        connection.zrange(ZSET_KEY, 0, 1);
        connection.zrangebyscore(ZSET_KEY, VALUE, VALUE);
        connection.zrangebyscore(ZSET_KEY, 0, 1);
        connection.zrangebyscore(ZSET_KEY, VALUE, VALUE, 0, 1);
        connection.zrangebyscore(ZSET_KEY, 0, 1, 0, 1);
        connection.zrangebyscoreWithScores(ZSET_KEY, VALUE, VALUE);
        connection.zrangebyscoreWithScores(ZSET_KEY, 0, 1);
        connection.zrangebyscoreWithScores(ZSET_KEY, VALUE, VALUE, 0, 1);
        connection.zrangebyscoreWithScores(ZSET_KEY, 0, 1, 0, 1);
        connection.zrangeWithScores(ZSET_KEY, 0, 1);
        connection.zrank(ZSET_KEY, VALUE);
        connection.zrem(ZSET_KEY, VALUE);
        connection.zremrangebyrank(ZSET_KEY, 0, 1);
        connection.zremrangebyscore(ZSET_KEY, VALUE, VALUE);
        connection.zremrangebyscore(ZSET_KEY, 0, 1);
        connection.zremrangebyscore(ZSET_KEY, VALUE, VALUE);
        connection.zremrangebyscore(ZSET_KEY, 0, 1);
        connection.zrevrangebyscoreWithScores(ZSET_KEY, VALUE, VALUE);
        connection.zrevrangebyscoreWithScores(ZSET_KEY, 0, 1);
        connection.zrevrangebyscoreWithScores(ZSET_KEY, VALUE, VALUE, 0, 1);
        connection.zrevrangebyscoreWithScores(ZSET_KEY, 0, 1, 0, 1);
        connection.zrevrangeWithScores(ZSET_KEY, 0, 1);
        connection.zrevrank(ZSET_KEY, VALUE);
//        connection.zscan(ZSET_KEY, 0);
        
        connection.zscore(ZSET_KEY, VALUE);
        
        connection.zunionstore(SET_KEY, SET_KEY, SET_KEY);
        connection.zunionstore(SET_KEY, ZStoreArgs.Builder.max(), SET_KEY, SET_KEY);
        
        connection.close();
        
        client.shutdown();
                                
        printToOutput("Finished testing on synchronous connection");
        return SUCCESS;
    }

}
