package com.tracelytics.test.springboot;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;
import redis.clients.jedis.*;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Controller
public class TestMulti extends AbstractJedisController {
    @GetMapping("/test-multi")
    public ModelAndView test(Model model) {
        try (Jedis jedis = getJedis()) {
            Transaction transaction = jedis.multi();
            transaction.append(STRING_KEY, STRING_VALUE);
            transaction.append(BYTE_KEY, BYTE_VALUE);

            transaction.bitcount(BYTE_KEY);
            transaction.bitcount(STRING_KEY);
            transaction.bitcount(BYTE_KEY, 0, 1);
            transaction.bitcount(STRING_KEY, 0, 1);
            transaction.bitop(BitOP.AND, BYTE_KEY_2, BYTE_KEY, BYTE_KEY);
            transaction.bitop(BitOP.AND, STRING_KEY_2, STRING_KEY, STRING_KEY);
            transaction.bitpos(BYTE_KEY, true);
            transaction.bitpos(STRING_KEY, true);
            transaction.bitpos(BYTE_KEY, true, new BitPosParams(0));
            transaction.bitpos(STRING_KEY, true, new BitPosParams(0));

            //blpop
            fillList(transaction, LIST_BYTE_KEY, 1); //so the pop will not be blocking due to empty list below
            fillList(transaction, LIST_STRING_KEY, 2); //so the pop will not be blocking due to empty list below
            transaction.blpop(TIMEOUT, LIST_BYTE_KEY);
            transaction.blpop(TIMEOUT, LIST_STRING_KEY);
            transaction.blpop(TIMEOUT, LIST_STRING_KEY, LIST_STRING_KEY);

            //brpop
            fillList(transaction, LIST_BYTE_KEY, 1); //so the pop will not be blocking due to empty list below
            fillList(transaction, LIST_STRING_KEY, 2); //so the pop will not be blocking due to empty list below
            transaction.brpop(TIMEOUT, LIST_BYTE_KEY);
            transaction.brpop(TIMEOUT, LIST_STRING_KEY);
            transaction.brpop(TIMEOUT, LIST_STRING_KEY, LIST_STRING_KEY);

    //        transaction.configGet("*");
            transaction.configResetStat();

            transaction.dbSize();
            transaction.decr(BYTE_KEY);
            transaction.decr(STRING_KEY);
            transaction.decrBy(BYTE_KEY, 2);
            transaction.decrBy(STRING_KEY, 2);

            transaction.dump(BYTE_KEY);
            transaction.dump(STRING_KEY);

            transaction.echo(STRING_VALUE);
            transaction.echo(BYTE_VALUE);

    //        transaction.eval("return {}"); //java.util.ArrayList cannot be cast to [B
    //        transaction.eval("return {}", Collections.EMPTY_LIST, Collections.EMPTY_LIST);

            transaction.exists(BYTE_KEY);
            transaction.exists(STRING_KEY);

            transaction.expire(BYTE_KEY, EXPIRY);
            transaction.expire(STRING_KEY, EXPIRY);
            transaction.expireAt(BYTE_KEY, System.currentTimeMillis() / 1000L + EXPIRY);
            transaction.expireAt(STRING_KEY, System.currentTimeMillis() / 1000L + EXPIRY);

            transaction.get(BYTE_KEY);
            transaction.get(STRING_KEY);
            transaction.get("BLAH");
            transaction.getbit(BYTE_KEY, 0);
            transaction.getbit(STRING_KEY, 0);
    //        transaction.set("string-byte-key".getBytes(), "this is a string".getBytes()); //[B cannot be cast to java.lang.Long, probably a Jedis bug
    //        transaction.getrange("string-byte-key".getBytes(), 0, 1);
    //        transaction.set("string-string-key", "this is a string");
    //        transaction.getrange("string-string-key", 0, 1);
            transaction.getSet(BYTE_KEY, BYTE_VALUE);
            transaction.getSet(STRING_KEY, STRING_VALUE);

            transaction.hdel(HASH_BYTE_KEY, HASH_FIELD_BYTE_KEY);
            transaction.hdel(HASH_STRING_KEY, HASH_FIELD_STRING_KEY);
            transaction.hexists(HASH_BYTE_KEY, HASH_FIELD_BYTE_KEY);
            transaction.hexists(HASH_STRING_KEY, HASH_FIELD_STRING_KEY);
            transaction.hget(HASH_BYTE_KEY, HASH_FIELD_BYTE_KEY);
            transaction.hget(HASH_STRING_KEY, HASH_FIELD_STRING_KEY);
            transaction.hgetAll(HASH_BYTE_KEY);
            transaction.hgetAll(HASH_STRING_KEY);
            transaction.hincrBy(HASH_BYTE_KEY, HASH_FIELD_BYTE_KEY, 1);
            transaction.hincrBy(HASH_STRING_KEY, HASH_FIELD_STRING_KEY, 1);
            transaction.hincrByFloat(HASH_BYTE_KEY, HASH_FIELD_BYTE_KEY, 1);
            transaction.hincrByFloat(HASH_STRING_KEY, HASH_FIELD_STRING_KEY, 1);
            transaction.hkeys(HASH_BYTE_KEY);
            transaction.hkeys(HASH_STRING_KEY);
            transaction.hlen(HASH_BYTE_KEY);
            transaction.hlen(HASH_STRING_KEY);
            transaction.hmget(HASH_BYTE_KEY, HASH_FIELD_BYTE_KEY);
            transaction.hmget(HASH_STRING_KEY, HASH_FIELD_STRING_KEY);
            transaction.hmset(HASH_BYTE_KEY, Collections.singletonMap(HASH_FIELD_BYTE_KEY, BYTE_VALUE));
            transaction.hmset(HASH_STRING_KEY, Collections.singletonMap(HASH_FIELD_STRING_KEY, STRING_VALUE));
            transaction.hset(HASH_BYTE_KEY, HASH_FIELD_BYTE_KEY, BYTE_VALUE);
            transaction.hset(HASH_STRING_KEY, HASH_FIELD_STRING_KEY, STRING_VALUE);
            transaction.hsetnx(HASH_BYTE_KEY, HASH_FIELD_BYTE_KEY, BYTE_VALUE);
            transaction.hsetnx(HASH_STRING_KEY, HASH_FIELD_STRING_KEY, STRING_VALUE);
            transaction.hvals(HASH_BYTE_KEY);
            transaction.hvals(HASH_STRING_KEY);

            transaction.incr(BYTE_KEY);
            transaction.incr(STRING_KEY);
            transaction.incrBy(BYTE_KEY, 1);
            transaction.incrBy(STRING_KEY, 1);
            transaction.incrByFloat(BYTE_KEY, 1);
            transaction.incrByFloat(STRING_KEY, 1);
            transaction.info();

            transaction.keys(BYTE_KEY);
            transaction.keys(STRING_KEY);

            transaction.lastsave();
            transaction.lindex(LIST_BYTE_KEY, 0);
            transaction.lindex(LIST_STRING_KEY, 0);
            transaction.linsert(LIST_BYTE_KEY, ListPosition.BEFORE, BYTE_VALUE, BYTE_VALUE);
            transaction.linsert(LIST_STRING_KEY, ListPosition.BEFORE, STRING_VALUE, STRING_VALUE);
            transaction.llen(LIST_BYTE_KEY);
            transaction.llen(LIST_STRING_KEY);
            transaction.lpop(LIST_BYTE_KEY);
            transaction.lpop(LIST_STRING_KEY);
            transaction.lpush(LIST_BYTE_KEY, BYTE_VALUE);
            transaction.lpush(LIST_STRING_KEY, STRING_VALUE);
            transaction.lpushx(LIST_BYTE_KEY, BYTE_VALUE);
            transaction.lpushx(LIST_STRING_KEY, STRING_VALUE);
            transaction.lrange(LIST_BYTE_KEY, 0, 1);
            transaction.lrange(LIST_STRING_KEY, 0, 1);
            transaction.lrem(LIST_BYTE_KEY, 1, BYTE_VALUE);
            transaction.lrem(LIST_STRING_KEY, 1, STRING_VALUE);
            transaction.lset(LIST_BYTE_KEY, 0, BYTE_VALUE);
            transaction.lset(LIST_STRING_KEY, 0, STRING_VALUE);
            transaction.ltrim(LIST_BYTE_KEY, 0, 1);
            transaction.ltrim(LIST_STRING_KEY, 0, 1);

    //        jedis.monitor(new JedisMonitor() { public void onCommand(String command) {}});
    //        jedis.migrate(REDIS_HOST, REDIS_PORT, STRING_KEY, 0, Protocol.DEFAULT_TIMEOUT);
            transaction.mget(BYTE_KEY);
            transaction.mget(STRING_KEY);
            transaction.move(BYTE_KEY, 1);
            transaction.move(STRING_KEY, 1);
            transaction.mset(BYTE_KEY, BYTE_VALUE);
            transaction.mset(STRING_KEY, STRING_VALUE);
            transaction.msetnx(BYTE_KEY, BYTE_VALUE);
            transaction.msetnx(STRING_KEY, STRING_VALUE);

            transaction.objectEncoding(BYTE_KEY);
            transaction.objectEncoding(STRING_KEY);
            transaction.objectIdletime(BYTE_KEY);
            transaction.objectIdletime(STRING_KEY);
            transaction.objectRefcount(BYTE_KEY);
            transaction.objectRefcount(STRING_KEY);

            transaction.persist(BYTE_KEY);
            transaction.persist(STRING_KEY);
            transaction.pexpire(BYTE_KEY, EXPIRY);
            transaction.pexpire(STRING_KEY, EXPIRY);
            transaction.pexpire(BYTE_KEY, EXPIRY * 1000L);
            transaction.pexpire(STRING_KEY, EXPIRY * 1000L);
            transaction.pexpireAt(BYTE_KEY, System.currentTimeMillis() / 1000L + EXPIRY);
            transaction.pexpireAt(STRING_KEY, System.currentTimeMillis() / 1000L + EXPIRY);
            transaction.pfadd(LOG_BYTE_KEY, BYTE_VALUE);
            transaction.pfadd(LOG_STRING_KEY, STRING_VALUE);
            transaction.pfcount(LOG_BYTE_KEY);
            transaction.pfcount(LOG_BYTE_KEY, LOG_BYTE_KEY);
            transaction.pfcount(LOG_STRING_KEY);
            transaction.pfcount(LOG_STRING_KEY, LOG_STRING_KEY);
            transaction.pfmerge(LOG_BYTE_KEY, LOG_BYTE_KEY, LOG_BYTE_KEY);
            transaction.pfmerge(LOG_STRING_KEY, LOG_STRING_KEY, LOG_STRING_KEY);
            transaction.ping();
            transaction.psetex(BYTE_KEY, EXPIRY, BYTE_VALUE);
            transaction.psetex(STRING_KEY, EXPIRY, STRING_VALUE);


            //jedis.psubscribe(new DummyBinaryPubSub(), "*".getBytes()); //not instrumenting publisher/subscriber
    //        jedis.psubscribe(new DummyPubSub(), "*"); //not instrumenting publisher/subscriber
            transaction.pttl(BYTE_KEY);
            transaction.pttl(STRING_KEY);
    //        jedis.publish("test-channel".getBytes(), BYTE_VALUE); //not instrumenting publisher/subscriber
    //        jedis.publish("test-channel", STRING_VALUE); //not instrumenting publisher/subscriber
    //        jedis.pubsubChannels("*"); //not instrumenting publisher/subscriber
    //        jedis.pubsubNumPat(); //not instrumenting publisher/subscriber
    //        jedis.pubsubNumSub("test-channel"); //not instrumenting publisher/subscriber

            transaction.randomKey();
            transaction.rename(BYTE_KEY, BYTE_KEY_2);
            transaction.rename(STRING_KEY, STRING_KEY_2);
            transaction.renamenx(BYTE_KEY_2, BYTE_KEY);
            transaction.renamenx(STRING_KEY_2, STRING_KEY);
            transaction.rpop(LIST_BYTE_KEY);
            transaction.rpop(LIST_STRING_KEY);
            transaction.rpoplpush(LIST_BYTE_KEY, LIST_BYTE_KEY);
            transaction.rpoplpush(LIST_STRING_KEY, LIST_STRING_KEY);
            transaction.rpush(LIST_BYTE_KEY, BYTE_VALUE);
            transaction.rpush(LIST_STRING_KEY, STRING_VALUE);
            transaction.rpushx(LIST_BYTE_KEY, BYTE_VALUE);
            transaction.rpushx(LIST_STRING_KEY, STRING_VALUE);

            transaction.sadd(SET_BYTE_KEY, BYTE_VALUE);
            transaction.sadd(SET_STRING_KEY, STRING_VALUE);
            transaction.save();
            transaction.scard(SET_BYTE_KEY);
            transaction.scard(SET_STRING_KEY);

            transaction.sdiff(SET_BYTE_KEY);
            transaction.sdiff(SET_STRING_KEY);
            transaction.sdiffstore(SET_BYTE_KEY, SET_BYTE_KEY);
            transaction.sdiffstore(SET_STRING_KEY, SET_STRING_KEY);
            transaction.select(0);

            transaction.set(BYTE_KEY, BYTE_VALUE);
            transaction.set(STRING_KEY, STRING_VALUE);
            transaction.setbit(BYTE_KEY, 0, new byte[]{0});
            transaction.setbit(STRING_KEY, 0, true);
            transaction.setex(BYTE_KEY, EXPIRY, BYTE_VALUE);
            transaction.setex(STRING_KEY, EXPIRY, STRING_VALUE);
            transaction.setnx(BYTE_KEY, BYTE_VALUE);
            transaction.setnx(STRING_KEY, STRING_VALUE);
            transaction.setrange(BYTE_KEY, 0, BYTE_VALUE);
            transaction.setrange(STRING_KEY, 0, STRING_VALUE);

            transaction.sinter(SET_BYTE_KEY);
            transaction.sinter(SET_STRING_KEY);
            transaction.sinterstore(SET_BYTE_KEY, SET_BYTE_KEY);
            transaction.sinterstore(SET_STRING_KEY, SET_STRING_KEY);
            transaction.sismember(SET_BYTE_KEY, BYTE_VALUE);
            transaction.sismember(SET_STRING_KEY, STRING_VALUE);
            transaction.smembers(SET_BYTE_KEY);
            transaction.smembers(SET_STRING_KEY);
            transaction.smove(SET_BYTE_KEY, SET_BYTE_KEY, BYTE_VALUE);
            transaction.smove(SET_STRING_KEY, SET_STRING_KEY, STRING_VALUE);
            transaction.sort(SET_BYTE_KEY);
            transaction.sort(SET_STRING_KEY);
            transaction.sort(SET_BYTE_KEY, LIST_BYTE_KEY);
            transaction.sort(SET_STRING_KEY, LIST_STRING_KEY);
            transaction.sort(SET_BYTE_KEY, new SortingParams());
            transaction.sort(SET_STRING_KEY, new SortingParams());
            transaction.sort(SET_BYTE_KEY, new SortingParams(), LIST_BYTE_KEY);
            transaction.sort(SET_STRING_KEY, new SortingParams(), LIST_STRING_KEY);

            transaction.spop(SET_BYTE_KEY);
            transaction.spop(SET_STRING_KEY);
            transaction.srandmember(SET_BYTE_KEY);
            transaction.srandmember(SET_STRING_KEY);
            transaction.srandmember(SET_BYTE_KEY, 1);
            transaction.srandmember(SET_STRING_KEY, 1);
            transaction.srem(SET_BYTE_KEY, BYTE_VALUE);
            transaction.srem(SET_STRING_KEY, STRING_VALUE);

            transaction.strlen(BYTE_KEY);
            transaction.strlen(STRING_KEY);
            transaction.substr(BYTE_KEY, 0, 1);
            transaction.substr(STRING_KEY, 0, 1);
            transaction.sunion(SET_BYTE_KEY, SET_BYTE_KEY);
            transaction.sunion(SET_STRING_KEY, SET_STRING_KEY);
            transaction.sunionstore(SET_BYTE_KEY, SET_BYTE_KEY, SET_BYTE_KEY);
            transaction.sunionstore(SET_STRING_KEY, SET_STRING_KEY, SET_STRING_KEY);

            transaction.time();
            transaction.ttl(BYTE_KEY);
            transaction.ttl(STRING_KEY);
            transaction.type(BYTE_KEY);
            transaction.type(STRING_KEY);

            transaction.watch(BYTE_KEY);
            transaction.watch(STRING_KEY);

            transaction.exec();

            transaction = jedis.multi();
            transaction.zadd(ZSET_STRING_KEY, Collections.singletonMap(STRING_VALUE, (double) 1));
            transaction.zadd(ZSET_STRING_KEY, Collections.singletonMap(STRING_VALUE, (double) 1));
            transaction.zadd(ZSET_BYTE_KEY, 1, BYTE_VALUE);
            transaction.zadd(ZSET_STRING_KEY, 1, STRING_VALUE);
            transaction.zcard(ZSET_BYTE_KEY);
            transaction.zcard(ZSET_STRING_KEY);
            transaction.zcount(ZSET_STRING_KEY, STRING_VALUE, STRING_VALUE);
            transaction.zcount(ZSET_BYTE_KEY, 0, 1);
            transaction.zcount(ZSET_STRING_KEY, 0, 1);
            transaction.zincrby(ZSET_BYTE_KEY, 1, BYTE_VALUE);
            transaction.zincrby(ZSET_STRING_KEY, 1, STRING_VALUE);
            transaction.zinterstore(ZSET_BYTE_KEY, ZSET_BYTE_KEY, ZSET_BYTE_KEY);
            transaction.zinterstore(ZSET_STRING_KEY, ZSET_STRING_KEY, ZSET_STRING_KEY);
            transaction.zinterstore(ZSET_BYTE_KEY, new ZParams(), ZSET_BYTE_KEY, ZSET_BYTE_KEY);
            transaction.zinterstore(ZSET_STRING_KEY, new ZParams(), ZSET_STRING_KEY, ZSET_STRING_KEY);
            transaction.zlexcount(ZSET_BYTE_KEY, "-".getBytes(), "+".getBytes());
            transaction.zlexcount(ZSET_STRING_KEY, "-", "+");
            transaction.zrange(ZSET_BYTE_KEY, 0, 1);
            transaction.zrange(ZSET_STRING_KEY, 0, 1);
            transaction.zrangeByLex(ZSET_BYTE_KEY, "-".getBytes(), "+".getBytes());
            transaction.zrangeByLex(ZSET_STRING_KEY, "-", "+");
            transaction.zrangeByLex(ZSET_BYTE_KEY, "-".getBytes(), "+".getBytes(), 0, 1);
            transaction.zrangeByLex(ZSET_STRING_KEY, "-", "+", 0, 1);
            transaction.zrangeByScore(ZSET_BYTE_KEY, BYTE_VALUE, BYTE_VALUE);
            transaction.zrangeByScore(ZSET_BYTE_KEY, 0, 1);
            transaction.zrangeByScore(ZSET_STRING_KEY, STRING_VALUE, STRING_VALUE);
            transaction.zrangeByScore(ZSET_STRING_KEY, 0, 1);
            transaction.zrangeByScore(ZSET_BYTE_KEY, BYTE_VALUE, BYTE_VALUE, 0, 1);
            transaction.zrangeByScore(ZSET_BYTE_KEY, 0, 1, 0, 1);
            transaction.zrangeByScore(ZSET_STRING_KEY, STRING_VALUE, STRING_VALUE, 0, 1);
            transaction.zrangeByScore(ZSET_STRING_KEY, 0, 1, 0, 1);
            transaction.zrangeByScoreWithScores(ZSET_BYTE_KEY, BYTE_VALUE, BYTE_VALUE);
            transaction.zrangeByScoreWithScores(ZSET_BYTE_KEY, 0, 1);
            transaction.zrangeByScoreWithScores(ZSET_STRING_KEY, STRING_VALUE, STRING_VALUE);
            transaction.zrangeByScoreWithScores(ZSET_STRING_KEY, 0, 1);
            transaction.zrangeByScoreWithScores(ZSET_BYTE_KEY, BYTE_VALUE, BYTE_VALUE, 0, 1);
            transaction.zrangeByScoreWithScores(ZSET_BYTE_KEY, 0, 1, 0, 1);
            transaction.zrangeByScoreWithScores(ZSET_STRING_KEY, STRING_VALUE, STRING_VALUE, 0, 1);
            transaction.zrangeByScoreWithScores(ZSET_STRING_KEY, 0, 1, 0, 1);
            transaction.zrangeWithScores(ZSET_BYTE_KEY, 0, 1);
            transaction.zrangeWithScores(ZSET_STRING_KEY, 0, 1);
            transaction.zrank(ZSET_BYTE_KEY, BYTE_VALUE);
            transaction.zrank(ZSET_STRING_KEY, STRING_VALUE);
            transaction.zrem(ZSET_BYTE_KEY, BYTE_VALUE);
            transaction.zrem(ZSET_STRING_KEY, STRING_VALUE);
            transaction.zremrangeByLex(ZSET_BYTE_KEY, "-".getBytes(), "+".getBytes());
            transaction.zremrangeByLex(ZSET_STRING_KEY, "-", "+");
            transaction.zremrangeByRank(ZSET_BYTE_KEY, 0, 1);
            transaction.zremrangeByRank(ZSET_STRING_KEY, 0, 1);
            transaction.zremrangeByScore(ZSET_BYTE_KEY, BYTE_VALUE, BYTE_VALUE);
            transaction.zremrangeByScore(ZSET_BYTE_KEY, 0, 1);
            transaction.zremrangeByScore(ZSET_STRING_KEY, STRING_VALUE, STRING_VALUE);
            transaction.zremrangeByScore(ZSET_STRING_KEY, 0, 1);
            transaction.zrevrangeByScore(ZSET_BYTE_KEY, BYTE_VALUE, BYTE_VALUE);
            transaction.zrevrangeByScore(ZSET_BYTE_KEY, 0, 1);
            transaction.zrevrangeByScore(ZSET_STRING_KEY, STRING_VALUE, STRING_VALUE);
            transaction.zrevrangeByScore(ZSET_STRING_KEY, 0, 1);
            transaction.zrevrangeByScore(ZSET_BYTE_KEY, BYTE_VALUE, BYTE_VALUE, 0, 1);
            transaction.zrevrangeByScore(ZSET_BYTE_KEY, 0, 1, 0, 1);
            transaction.zrevrangeByScore(ZSET_STRING_KEY, STRING_VALUE, STRING_VALUE, 0, 1);
            transaction.zrevrangeByScore(ZSET_STRING_KEY, 0, 1, 0, 1);
            transaction.zrevrangeByScoreWithScores(ZSET_BYTE_KEY, BYTE_VALUE, BYTE_VALUE);
            transaction.zrevrangeByScoreWithScores(ZSET_BYTE_KEY, 0, 1);
            transaction.zrevrangeByScoreWithScores(ZSET_STRING_KEY, STRING_VALUE, STRING_VALUE);
            transaction.zrevrangeByScoreWithScores(ZSET_STRING_KEY, 0, 1);
            transaction.zrevrangeByScoreWithScores(ZSET_BYTE_KEY, BYTE_VALUE, BYTE_VALUE, 0, 1);
            transaction.zrevrangeByScoreWithScores(ZSET_BYTE_KEY, 0, 1, 0, 1);
            transaction.zrevrangeByScoreWithScores(ZSET_STRING_KEY, STRING_VALUE, STRING_VALUE, 0, 1);
            transaction.zrevrangeByScoreWithScores(ZSET_STRING_KEY, 0, 1, 0, 1);
            transaction.zrevrangeWithScores(ZSET_BYTE_KEY, 0, 1);
            transaction.zrevrangeWithScores(ZSET_STRING_KEY, 0, 1);
            transaction.zrevrank(ZSET_BYTE_KEY, BYTE_VALUE);
            transaction.zrevrank(ZSET_STRING_KEY, STRING_VALUE);

            transaction.zscore(ZSET_BYTE_KEY, BYTE_VALUE);
            transaction.zscore(ZSET_STRING_KEY, STRING_VALUE);

            transaction.zunionstore(SET_BYTE_KEY, SET_BYTE_KEY, SET_BYTE_KEY);
            transaction.zunionstore(SET_STRING_KEY, SET_STRING_KEY, SET_STRING_KEY);
            transaction.zunionstore(SET_BYTE_KEY, new ZParams(), SET_BYTE_KEY);
            transaction.zunionstore(SET_STRING_KEY, new ZParams(), SET_STRING_KEY, SET_STRING_KEY);

            //stream
            Map<String,String> map = new HashMap<>();
            map.put("f1", "v1");

            Response<StreamEntryID> streamEntryIDResponse = transaction.xadd(STREAM_KEY, null, map);
            transaction.exec();
            transaction.close();
            StreamEntryID streamEntryID = streamEntryIDResponse.get();
            transaction = jedis.multi();
            transaction.xack(STREAM_KEY, CONSUMER_GROUP, streamEntryID);
            transaction.xgroupCreate(STREAM_KEY, CONSUMER_GROUP, null, false);
            transaction.xclaim(STREAM_KEY, CONSUMER_GROUP, CONSUMER_GROUP, 500, 0 ,0, false, streamEntryID);
            transaction.xgroupSetID(STREAM_KEY, CONSUMER_GROUP, streamEntryID);
            transaction.xgroupDelConsumer(STREAM_KEY, CONSUMER_GROUP, "consumer");
            transaction.xgroupDestroy(STREAM_KEY, CONSUMER_GROUP);

            transaction.xlen(STREAM_KEY);
            transaction.xgroupCreate(STREAM_KEY, PENDING_GROUP, null, false);
            transaction.xpending(STREAM_KEY, PENDING_GROUP, null, null, 1,"consumer");
            transaction.xrange(STREAM_KEY, null, null, 1);

            Map.Entry<String, StreamEntryID> streamQuery = new AbstractMap.SimpleImmutableEntry<>(
                    STREAM_KEY, new StreamEntryID());
            transaction.xrevrange(STREAM_KEY, null, null, 1);
            transaction.xgroupCreate(STREAM_KEY, "xreadGroup-group", null, false);
            transaction.xtrim(STREAM_KEY, 100, false);

            transaction.xdel(STREAM_KEY, streamEntryID);

            transaction.del(BYTE_KEY);
            transaction.del(BYTE_KEY, BYTE_KEY);
            transaction.del(STRING_KEY);
            transaction.del(STRING_KEY, STRING_KEY);

            transaction.flushAll();
            transaction.flushDB();

            //jedis.shutdown();

            transaction.exec();

            transaction = jedis.multi();
            transaction.set(STRING_KEY, STRING_VALUE);
            transaction.migrate(host, port, STRING_KEY, 0, Protocol.DEFAULT_TIMEOUT);
            transaction.shutdown();
            transaction.discard();
        }
        printToOutput("Finished calling multi with all the redis operations");
        return getModelAndView("index");
    }

    private void fillList(Transaction jedis, Object key, int count) {
        for (int i = 0 ; i < count; i++) {
            if (key instanceof String) {
                jedis.rpush((String)key, STRING_VALUE);
            } else if (key instanceof byte[]) {
                jedis.rpush((byte[])key, BYTE_VALUE);
            }
        }

    }

    private class DummyBinaryPubSub extends BinaryJedisPubSub {

        @Override
        public void onMessage(byte[] channel, byte[] message) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onPMessage(byte[] pattern, byte[] channel, byte[] message) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onSubscribe(byte[] channel, int subscribedChannels) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onUnsubscribe(byte[] channel, int subscribedChannels) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onPUnsubscribe(byte[] pattern, int subscribedChannels) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onPSubscribe(byte[] pattern, int subscribedChannels) {
            // TODO Auto-generated method stub

        }

    }

    private class DummyPubSub extends JedisPubSub {

        @Override
        public void onMessage(String channel, String message) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onPMessage(String pattern, String channel, String message) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onSubscribe(String channel, int subscribedChannels) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onUnsubscribe(String channel, int subscribedChannels) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onPUnsubscribe(String pattern, int subscribedChannels) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onPSubscribe(String pattern, int subscribedChannels) {
            // TODO Auto-generated method stub

        }

    }
}