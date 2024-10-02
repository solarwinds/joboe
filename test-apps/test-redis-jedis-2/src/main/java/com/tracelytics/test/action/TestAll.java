package com.tracelytics.test.action;

import java.util.Collections;

import redis.clients.jedis.BitOP;
import redis.clients.jedis.BitPosParams;
import redis.clients.jedis.Client;
import redis.clients.jedis.DebugParams;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.ScanParams;
import redis.clients.jedis.SortingParams;
import redis.clients.jedis.Transaction;
import redis.clients.jedis.ZParams;
import redis.clients.jedis.exceptions.JedisException;

@SuppressWarnings("serial")
@org.apache.struts2.convention.annotation.Results({
    @org.apache.struts2.convention.annotation.Result(name="success", location="index.jsp"),
    @org.apache.struts2.convention.annotation.Result(name="error", location="index.jsp"),
})
public class TestAll extends AbstractJedisAction {
    @Override
    protected String test(Jedis jedis) throws Exception {
        initialize();
        jedis.pttl(BYTE_KEY);
        
        jedis.append(STRING_KEY, STRING_VALUE);
        jedis.append(BYTE_KEY, BYTE_VALUE);
//        jedis.asking();
        try {
            jedis.auth("abc");
        } catch (JedisException e) {
            //expected
        }
        //jedis.bgsave();
        
        jedis.bitcount(BYTE_KEY);
        jedis.bitcount(STRING_KEY);
        jedis.bitcount(BYTE_KEY, 0, 1);
        jedis.bitcount(STRING_KEY, 0, 1);
        jedis.bitop(BitOP.AND, BYTE_KEY_2, BYTE_KEY, BYTE_KEY);
        jedis.bitop(BitOP.AND, STRING_KEY_2, STRING_KEY, STRING_KEY);
        jedis.bitpos(BYTE_KEY, true);
        jedis.bitpos(STRING_KEY, true);
        jedis.bitpos(BYTE_KEY, true, new BitPosParams(0));
        jedis.bitpos(STRING_KEY, true, new BitPosParams(0));        
        
        //blpop commented out as it throws exception when calling those methods, probably a bug from jedis
//        jedis.blpop(LIST_BYTE_KEY);  
        //jedis.blpop(LIST_BYTE_KEY, LIST_BYTE_KEY);
        //jedis.blpop(LIST_STRING_KEY);
//        jedis.blpop(LIST_STRING_KEY, LIST_STRING_KEY);
        
        fillList(jedis, LIST_BYTE_KEY, 1); //so the pop will not be blocking due to empty list below
        fillList(jedis, LIST_STRING_KEY, 2); //so the pop will not be blocking due to empty list below
        jedis.blpop(TIMEOUT, LIST_BYTE_KEY);
        jedis.blpop(TIMEOUT, LIST_STRING_KEY);
        jedis.blpop(TIMEOUT, LIST_STRING_KEY, LIST_STRING_KEY);
        
        //brpop commented out as it throws exception when calling those methods, probably a bug from jedis
//        jedis.brpop(LIST_BYTE_KEY);
//        jedis.brpop(LIST_BYTE_KEY, LIST_BYTE_KEY);
//        jedis.brpop(LIST_STRING_KEY);
//        jedis.brpop(LIST_STRING_KEY, LIST_STRING_KEY);
        fillList(jedis, LIST_BYTE_KEY, 1); //so the pop will not be blocking due to empty list below
        fillList(jedis, LIST_STRING_KEY, 2); //so the pop will not be blocking due to empty list below
        jedis.brpop(TIMEOUT, LIST_BYTE_KEY);
        jedis.brpop(TIMEOUT, LIST_STRING_KEY);
        jedis.brpop(TIMEOUT, LIST_STRING_KEY, LIST_STRING_KEY);
        
        jedis.clientGetname();
        
//        jedis.clientKill("1".getBytes());
//        jedis.clientKill("1");
        jedis.clientList();
        jedis.clientSetname("test-client".getBytes());
        jedis.clientSetname("test-client");
//        jedis.clusterAddSlots(1);
//        jedis.clusterCountKeysInSlot(1);
//        jedis.clusterDelSlots(1);
//        jedis.clusterFailover();
//        jedis.clusterFlushSlots();
//        jedis.clusterForget("1");
//        jedis.clusterGetKeysInSlot(1, 1);
//        jedis.clusterInfo();
//        jedis.clusterKeySlot("1");
//        jedis.clusterNodes();
//        jedis.clusterReplicate("1");
//        jedis.clusterReset(Reset.SOFT);
//        jedis.clusterSaveConfig();
//        jedis.clusterSetSlotImporting(1, "1");
//        jedis.clusterSetSlotMigrating(1, "1");
//        jedis.clusterSetSlotStable(1);
//        jedis.clusterSlaves("1");
//        jedis.clusterSlots();
        jedis.configGet("*".getBytes());
        jedis.configResetStat();
        jedis.configSet("tcp-keepalive".getBytes(), jedis.configGet("tcp-keepalive".getBytes()).get(1));
        jedis.configSet("tcp-keepalive", jedis.configGet("tcp-keepalive").get(1));
        
        jedis.dbSize();
        jedis.debug(DebugParams.RELOAD());
        jedis.decr(BYTE_KEY);
        jedis.decr(STRING_KEY);
        jedis.decrBy(BYTE_KEY, 2);
        jedis.decrBy(STRING_KEY, 2);
        
        jedis.dump(BYTE_KEY);
        jedis.dump(STRING_KEY);
        
        jedis.echo(STRING_VALUE);
        jedis.echo(BYTE_VALUE);
        
        jedis.eval("return {}".getBytes());
        jedis.eval("return {}");
        jedis.eval("return { KEYS[1] }".getBytes(), "1".getBytes(), BYTE_KEY);
        jedis.eval("return { KEYS[1] }".getBytes(), 1, BYTE_KEY);
        jedis.eval("return {}".getBytes(), Collections.EMPTY_LIST, Collections.EMPTY_LIST);
        jedis.eval("return {}", Collections.EMPTY_LIST, Collections.EMPTY_LIST);
        jedis.eval("return {}", 0);
        
        byte[] byteSha = jedis.scriptLoad("return {}".getBytes());
        String stringSha = jedis.scriptLoad("return {}");
//        jedis.evalsha(byteSha); 2.6.1 bug
        jedis.evalsha(stringSha);
        jedis.evalsha(byteSha, 0);
        jedis.evalsha(byteSha, Collections.EMPTY_LIST, Collections.EMPTY_LIST);
        jedis.evalsha(stringSha, 0);
        jedis.evalsha(stringSha, Collections.EMPTY_LIST, Collections.EMPTY_LIST);
        
        jedis.exists(BYTE_KEY);
        jedis.exists(STRING_KEY);
        
        jedis.expire(BYTE_KEY, EXPIRY);
        jedis.expire(STRING_KEY, EXPIRY);
        jedis.expireAt(BYTE_KEY, System.currentTimeMillis() / 1000L + EXPIRY);
        jedis.expireAt(STRING_KEY, System.currentTimeMillis() / 1000L + EXPIRY);
        
        jedis.get(BYTE_KEY);
        jedis.get(STRING_KEY);
        jedis.get("BLAH");
        jedis.getbit(BYTE_KEY, 0);
        jedis.getbit(STRING_KEY, 0);
        jedis.getrange(BYTE_KEY, 0, 1);
        jedis.getrange(STRING_KEY, 0, 1);
        jedis.getSet(BYTE_KEY, BYTE_VALUE);
        jedis.getSet(STRING_KEY, STRING_VALUE);
        
        jedis.hdel(HASH_BYTE_KEY, HASH_FIELD_BYTE_KEY);
        jedis.hdel(HASH_STRING_KEY, HASH_FIELD_STRING_KEY);
        jedis.hexists(HASH_BYTE_KEY, HASH_FIELD_BYTE_KEY);
        jedis.hexists(HASH_STRING_KEY, HASH_FIELD_STRING_KEY);
        jedis.hget(HASH_BYTE_KEY, HASH_FIELD_BYTE_KEY);
        jedis.hget(HASH_STRING_KEY, HASH_FIELD_STRING_KEY);
        jedis.hgetAll(HASH_BYTE_KEY);
        jedis.hgetAll(HASH_STRING_KEY);
        jedis.hincrBy(HASH_BYTE_KEY, HASH_FIELD_BYTE_KEY, 1);
        jedis.hincrBy(HASH_STRING_KEY, HASH_FIELD_STRING_KEY, 1);
        jedis.hincrByFloat(HASH_BYTE_KEY, HASH_FIELD_BYTE_KEY, 1);
        jedis.hincrByFloat(HASH_STRING_KEY, HASH_FIELD_STRING_KEY, 1);
        jedis.hkeys(HASH_BYTE_KEY);
        jedis.hkeys(HASH_STRING_KEY);
        jedis.hlen(HASH_BYTE_KEY);
        jedis.hlen(HASH_STRING_KEY);
        jedis.hmget(HASH_BYTE_KEY, HASH_FIELD_BYTE_KEY);
        jedis.hmget(HASH_STRING_KEY, HASH_FIELD_STRING_KEY);
        jedis.hmset(HASH_BYTE_KEY, Collections.singletonMap(HASH_FIELD_BYTE_KEY, BYTE_VALUE));
        jedis.hmset(HASH_STRING_KEY, Collections.singletonMap(HASH_FIELD_STRING_KEY, STRING_VALUE));
        jedis.hscan(HASH_BYTE_KEY, ScanParams.SCAN_POINTER_START_BINARY);
        jedis.hscan(HASH_STRING_KEY, ScanParams.SCAN_POINTER_START);
        jedis.hset(HASH_BYTE_KEY, HASH_FIELD_BYTE_KEY, BYTE_VALUE);
        jedis.hset(HASH_STRING_KEY, HASH_FIELD_STRING_KEY, STRING_VALUE);
        jedis.hsetnx(HASH_BYTE_KEY, HASH_FIELD_BYTE_KEY, BYTE_VALUE);
        jedis.hsetnx(HASH_STRING_KEY, HASH_FIELD_STRING_KEY, STRING_VALUE);
        jedis.hvals(HASH_BYTE_KEY);
        jedis.hvals(HASH_STRING_KEY);
        
        jedis.incr(BYTE_KEY);
        jedis.incr(STRING_KEY);
        jedis.incrBy(BYTE_KEY, 1);
        jedis.incrBy(STRING_KEY, 1);
        jedis.incrByFloat(BYTE_KEY, 1);
        jedis.incrByFloat(STRING_KEY, 1);
        jedis.info();
        jedis.info("server");
                
        jedis.keys(BYTE_KEY);
        jedis.keys(STRING_KEY);
        
        jedis.lastsave();
        jedis.lindex(LIST_BYTE_KEY, 0);
        jedis.lindex(LIST_STRING_KEY, 0);
        jedis.linsert(LIST_BYTE_KEY, Client.LIST_POSITION.BEFORE, BYTE_VALUE, BYTE_VALUE);
        jedis.linsert(LIST_STRING_KEY, Client.LIST_POSITION.BEFORE, STRING_VALUE, STRING_VALUE);
        jedis.llen(LIST_BYTE_KEY);
        jedis.llen(LIST_STRING_KEY);
        jedis.lpop(LIST_BYTE_KEY);
        jedis.lpop(LIST_STRING_KEY);
        jedis.lpush(LIST_BYTE_KEY, BYTE_VALUE);
        jedis.lpush(LIST_STRING_KEY, STRING_VALUE);
        jedis.lpushx(LIST_BYTE_KEY, BYTE_VALUE);
        jedis.lpushx(LIST_STRING_KEY, STRING_VALUE);
        jedis.lrange(LIST_BYTE_KEY, 0, 1);
        jedis.lrange(LIST_STRING_KEY, 0, 1);
        jedis.lrem(LIST_BYTE_KEY, 1, BYTE_VALUE);
        jedis.lrem(LIST_STRING_KEY, 1, STRING_VALUE);
        jedis.lset(LIST_BYTE_KEY, 0, BYTE_VALUE);
        jedis.lset(LIST_STRING_KEY, 0, STRING_VALUE);
        jedis.ltrim(LIST_BYTE_KEY, 0, 1);
        jedis.ltrim(LIST_STRING_KEY, 0, 1);
        
//        jedis.monitor(new JedisMonitor() { public void onCommand(String command) {}});
//        jedis.migrate(REDIS_HOST, REDIS_PORT, STRING_KEY, 0, Protocol.DEFAULT_TIMEOUT);
        jedis.mget(BYTE_KEY);
        jedis.mget(STRING_KEY);
        jedis.move(BYTE_KEY, 1);
        jedis.move(STRING_KEY, 1);
        jedis.mset(BYTE_KEY, BYTE_VALUE);
        jedis.mset(STRING_KEY, STRING_VALUE);
        jedis.msetnx(BYTE_KEY, BYTE_VALUE);
        jedis.msetnx(STRING_KEY, STRING_VALUE);
        
        
        Transaction transaction = jedis.multi();
        transaction.set(STRING_KEY, STRING_VALUE);
        transaction.exec();
        
        jedis.objectEncoding(BYTE_KEY);
        jedis.objectEncoding(STRING_KEY);
        jedis.objectIdletime(BYTE_KEY);
        jedis.objectIdletime(STRING_KEY);
        jedis.objectRefcount(BYTE_KEY);
        jedis.objectRefcount(STRING_KEY);
        
        jedis.persist(BYTE_KEY);
        jedis.persist(STRING_KEY);
        jedis.pexpire(BYTE_KEY, EXPIRY);
        jedis.pexpire(STRING_KEY, EXPIRY);
        jedis.pexpire(BYTE_KEY, EXPIRY * 1000L);
        jedis.pexpire(STRING_KEY, EXPIRY * 1000L);
        jedis.pexpireAt(BYTE_KEY, System.currentTimeMillis() / 1000L + EXPIRY);
        jedis.pexpireAt(STRING_KEY, System.currentTimeMillis() / 1000L + EXPIRY);
        jedis.pfadd(LOG_BYTE_KEY, BYTE_VALUE);
        jedis.pfadd(LOG_STRING_KEY, STRING_VALUE);
        jedis.pfcount(LOG_BYTE_KEY);
        jedis.pfcount(LOG_BYTE_KEY, LOG_BYTE_KEY);
        jedis.pfcount(LOG_STRING_KEY);
        jedis.pfcount(LOG_STRING_KEY, LOG_STRING_KEY);
        jedis.pfmerge(LOG_BYTE_KEY, LOG_BYTE_KEY, LOG_BYTE_KEY);
        jedis.pfmerge(LOG_STRING_KEY, LOG_STRING_KEY, LOG_STRING_KEY);
        jedis.ping();
        
        jedis.psetex(BYTE_KEY, EXPIRY, BYTE_VALUE);
        jedis.psetex(STRING_KEY, EXPIRY, STRING_VALUE);
        
        
        //jedis.psubscribe(new DummyBinaryPubSub(), "*".getBytes());
//        jedis.psubscribe(new DummyPubSub(), "*");
        jedis.pttl(BYTE_KEY);
        jedis.pttl(STRING_KEY);
//        jedis.publish("test-channel".getBytes(), BYTE_VALUE);
//        jedis.publish("test-channel", STRING_VALUE);
//        jedis.pubsubChannels("*");
//        jedis.pubsubNumPat();
//        jedis.pubsubNumSub("test-channel");
        
        jedis.randomBinaryKey();
        jedis.randomKey();
        jedis.set(BYTE_KEY, BYTE_VALUE);
        jedis.set(STRING_KEY, STRING_VALUE);
        jedis.rename(BYTE_KEY, BYTE_KEY_2);
        jedis.rename(STRING_KEY, STRING_KEY_2);
        jedis.renamenx(BYTE_KEY_2, BYTE_KEY);
        jedis.renamenx(STRING_KEY_2, STRING_KEY);
        jedis.resetState();
        jedis.restore(BYTE_KEY_2, EXPIRY, jedis.dump(BYTE_KEY));
        jedis.restore(STRING_KEY_2, EXPIRY, jedis.dump(STRING_KEY));
        jedis.rpop(LIST_BYTE_KEY);
        jedis.rpop(LIST_STRING_KEY);
        jedis.rpoplpush(LIST_BYTE_KEY, LIST_BYTE_KEY);
        jedis.rpoplpush(LIST_STRING_KEY, LIST_STRING_KEY);
        jedis.rpush(LIST_BYTE_KEY, BYTE_VALUE);
        jedis.rpush(LIST_STRING_KEY, STRING_VALUE);
        jedis.rpushx(LIST_BYTE_KEY, BYTE_VALUE);
        jedis.rpushx(LIST_STRING_KEY, STRING_VALUE);
        
        jedis.sadd(SET_BYTE_KEY, BYTE_VALUE);
        jedis.sadd(SET_STRING_KEY, STRING_VALUE);
        jedis.save();
        jedis.scan(ScanParams.SCAN_POINTER_START_BINARY);
        jedis.scan(0);
        jedis.scan(ScanParams.SCAN_POINTER_START);
        jedis.scan(ScanParams.SCAN_POINTER_START_BINARY, new ScanParams());
        jedis.scan(0, new ScanParams());
        jedis.scan(ScanParams.SCAN_POINTER_START, new ScanParams());
        jedis.scard(SET_BYTE_KEY);
        jedis.scard(SET_STRING_KEY);
        jedis.scriptExists(byteSha);
        jedis.scriptExists(stringSha);
        jedis.scriptExists(stringSha, stringSha);
        jedis.scriptFlush();
//        jedis.scriptKill();
        jedis.sdiff(SET_BYTE_KEY);
        jedis.sdiff(SET_STRING_KEY);
        jedis.sdiffstore(SET_BYTE_KEY, SET_BYTE_KEY);
        jedis.sdiffstore(SET_STRING_KEY, SET_STRING_KEY);
        jedis.select(0);
        
//        jedis.sentinelFailover("test-master");
//        jedis.sentinelGetMasterAddrByName("test-master");
//        jedis.sentinelMasters();
        //jedis.sentinelMonitor(masterName, ip, port, quorum);
//        jedis.sentinelRemove("test-master");
//        jedis.sentinelReset("*");
//        jedis.sentinelSet("test-master", Collections.<String, String>emptyMap());
//        jedis.sentinelSlaves("test-master");
        
        jedis.set(BYTE_KEY, BYTE_VALUE);
        jedis.set(STRING_KEY, STRING_VALUE);
        jedis.set(BYTE_KEY, BYTE_VALUE, "NX".getBytes());
        jedis.set(STRING_KEY, STRING_VALUE, "NX");
        jedis.set(BYTE_KEY, BYTE_VALUE, "NX".getBytes(), "EX".getBytes(), EXPIRY);
        jedis.set(STRING_KEY, STRING_VALUE, "NX", "EX", EXPIRY);
        jedis.set(BYTE_KEY, BYTE_VALUE, "NX".getBytes(), "EX".getBytes(), (long)EXPIRY);
        jedis.set(STRING_KEY, STRING_VALUE, "NX", "EX", (long)EXPIRY);
        jedis.setbit(BYTE_KEY, 0, true);
        jedis.setbit(BYTE_KEY, 0, "0".getBytes());
        jedis.setbit(STRING_KEY, 0, true);
        jedis.setbit(STRING_KEY, 0, STRING_VALUE);
        jedis.setex(BYTE_KEY, EXPIRY, BYTE_VALUE);
        jedis.setex(STRING_KEY, EXPIRY, STRING_VALUE);
        jedis.setnx(BYTE_KEY, BYTE_VALUE);
        jedis.setnx(STRING_KEY, STRING_VALUE);
        jedis.setrange(BYTE_KEY, 0, BYTE_VALUE);
        jedis.setrange(STRING_KEY, 0, STRING_VALUE);
        
        jedis.sinter(SET_BYTE_KEY);
        jedis.sinter(SET_STRING_KEY);
        jedis.sinterstore(SET_BYTE_KEY, SET_BYTE_KEY);
        jedis.sinterstore(SET_STRING_KEY, SET_STRING_KEY);
        jedis.sismember(SET_BYTE_KEY, BYTE_VALUE);
        jedis.sismember(SET_STRING_KEY, STRING_VALUE);
        jedis.slaveof(REDIS_HOST, REDIS_PORT);
        jedis.slaveofNoOne();
        jedis.slowlogGet();
        jedis.slowlogGet(1);
        jedis.slowlogGetBinary();
        jedis.slowlogGetBinary(1);
        jedis.slowlogLen();
        jedis.slowlogReset();
        jedis.smembers(SET_BYTE_KEY);
        jedis.smembers(SET_STRING_KEY);
        jedis.smove(SET_BYTE_KEY, SET_BYTE_KEY, BYTE_VALUE);
        jedis.smove(SET_STRING_KEY, SET_STRING_KEY, STRING_VALUE);
        jedis.sort(SET_BYTE_KEY);
        jedis.sort(SET_STRING_KEY);
        jedis.sort(SET_BYTE_KEY, LIST_BYTE_KEY);
        jedis.sort(SET_STRING_KEY, LIST_STRING_KEY);
        jedis.sort(SET_BYTE_KEY, new SortingParams());
        jedis.sort(SET_STRING_KEY, new SortingParams());
        jedis.sort(SET_BYTE_KEY, new SortingParams(), LIST_BYTE_KEY);
        jedis.sort(SET_STRING_KEY, new SortingParams(), LIST_STRING_KEY);
        
        jedis.spop(SET_BYTE_KEY);
        jedis.spop(SET_STRING_KEY);
        jedis.srandmember(SET_BYTE_KEY);
        jedis.srandmember(SET_STRING_KEY);
        jedis.srandmember(SET_BYTE_KEY, 1);
        jedis.srandmember(SET_STRING_KEY, 1);
        jedis.srem(SET_BYTE_KEY, BYTE_VALUE);
        jedis.srem(SET_STRING_KEY, STRING_VALUE);
        
        jedis.sscan(SET_BYTE_KEY, ScanParams.SCAN_POINTER_START_BINARY);
        jedis.sscan(SET_STRING_KEY, 0);
        jedis.sscan(SET_STRING_KEY, ScanParams.SCAN_POINTER_START);
        jedis.sscan(SET_BYTE_KEY, ScanParams.SCAN_POINTER_START_BINARY, new ScanParams());
        jedis.sscan(SET_STRING_KEY, 0, new ScanParams());
        jedis.sscan(SET_STRING_KEY, ScanParams.SCAN_POINTER_START, new ScanParams());
                
        jedis.strlen(BYTE_KEY);
        jedis.strlen(STRING_KEY);
        jedis.substr(BYTE_KEY, 0, 1);
        jedis.substr(STRING_KEY, 0, 1);
        jedis.sunion(SET_BYTE_KEY, SET_BYTE_KEY);
        jedis.sunion(SET_STRING_KEY, SET_STRING_KEY);
        jedis.sunionstore(SET_BYTE_KEY, SET_BYTE_KEY, SET_BYTE_KEY);
        jedis.sunionstore(SET_STRING_KEY, SET_STRING_KEY, SET_STRING_KEY);
//        jedis.sync();
        
        jedis.time();
        jedis.ttl(BYTE_KEY);
        jedis.ttl(STRING_KEY);
        jedis.type(BYTE_KEY);
        jedis.type(STRING_KEY);
        
        jedis.watch(BYTE_KEY);
        jedis.watch(STRING_KEY);
        jedis.unwatch();
        
        jedis.zadd(ZSET_BYTE_KEY, Collections.singletonMap(BYTE_VALUE, (double)1));
        jedis.zadd(ZSET_STRING_KEY, Collections.singletonMap(STRING_VALUE, (double)1));
        jedis.zadd(ZSET_BYTE_KEY, 1, BYTE_VALUE);
        jedis.zadd(ZSET_STRING_KEY, 1, STRING_VALUE);
        jedis.zcard(ZSET_BYTE_KEY);
        jedis.zcard(ZSET_STRING_KEY);
        jedis.zcount(ZSET_BYTE_KEY, BYTE_VALUE, BYTE_VALUE);
        jedis.zcount(ZSET_STRING_KEY, STRING_VALUE, STRING_VALUE);
        jedis.zcount(ZSET_BYTE_KEY, 0, 1);
        jedis.zcount(ZSET_STRING_KEY, 0, 1);
        jedis.zincrby(ZSET_BYTE_KEY, 1, BYTE_VALUE);
        jedis.zincrby(ZSET_STRING_KEY, 1, STRING_VALUE);
        jedis.zinterstore(ZSET_BYTE_KEY, ZSET_BYTE_KEY, ZSET_BYTE_KEY);
        jedis.zinterstore(ZSET_STRING_KEY, ZSET_STRING_KEY, ZSET_STRING_KEY);
        jedis.zinterstore(ZSET_BYTE_KEY, new ZParams(), ZSET_BYTE_KEY, ZSET_BYTE_KEY);
        jedis.zinterstore(ZSET_STRING_KEY, new ZParams(), ZSET_STRING_KEY, ZSET_STRING_KEY);
        jedis.zlexcount(ZSET_BYTE_KEY, "-".getBytes(), "+".getBytes());
        jedis.zlexcount(ZSET_STRING_KEY, "-", "+");
        jedis.zrange(ZSET_BYTE_KEY, 0, 1);
        jedis.zrange(ZSET_STRING_KEY, 0, 1);
        jedis.zrangeByLex(ZSET_BYTE_KEY, "-".getBytes(), "+".getBytes());
        jedis.zrangeByLex(ZSET_STRING_KEY, "-", "+");
        jedis.zrangeByLex(ZSET_BYTE_KEY, "-".getBytes(), "+".getBytes(), 0, 1);
        jedis.zrangeByLex(ZSET_STRING_KEY, "-", "+", 0, 1);
        jedis.zrangeByScore(ZSET_BYTE_KEY, BYTE_VALUE, BYTE_VALUE);
        jedis.zrangeByScore(ZSET_BYTE_KEY, 0, 1);
        jedis.zrangeByScore(ZSET_STRING_KEY, STRING_VALUE, STRING_VALUE);
        jedis.zrangeByScore(ZSET_STRING_KEY, 0, 1);
        jedis.zrangeByScore(ZSET_BYTE_KEY, BYTE_VALUE, BYTE_VALUE, 0, 1);
        jedis.zrangeByScore(ZSET_BYTE_KEY, 0, 1, 0, 1);
        jedis.zrangeByScore(ZSET_STRING_KEY, STRING_VALUE, STRING_VALUE, 0, 1);
        jedis.zrangeByScore(ZSET_STRING_KEY, 0, 1, 0, 1);
        jedis.zrangeByScoreWithScores(ZSET_BYTE_KEY, BYTE_VALUE, BYTE_VALUE);
        jedis.zrangeByScoreWithScores(ZSET_BYTE_KEY, 0, 1);
        jedis.zrangeByScoreWithScores(ZSET_STRING_KEY, STRING_VALUE, STRING_VALUE);
        jedis.zrangeByScoreWithScores(ZSET_STRING_KEY, 0, 1);
        jedis.zrangeByScoreWithScores(ZSET_BYTE_KEY, BYTE_VALUE, BYTE_VALUE, 0, 1);
        jedis.zrangeByScoreWithScores(ZSET_BYTE_KEY, 0, 1, 0, 1);
        jedis.zrangeByScoreWithScores(ZSET_STRING_KEY, STRING_VALUE, STRING_VALUE, 0, 1);
        jedis.zrangeByScoreWithScores(ZSET_STRING_KEY, 0, 1, 0, 1);
        jedis.zrangeWithScores(ZSET_BYTE_KEY, 0, 1);
        jedis.zrangeWithScores(ZSET_STRING_KEY, 0, 1);
        jedis.zrank(ZSET_BYTE_KEY, BYTE_VALUE);
        jedis.zrank(ZSET_STRING_KEY, STRING_VALUE);
        jedis.zrem(ZSET_BYTE_KEY, BYTE_VALUE);
        jedis.zrem(ZSET_STRING_KEY, STRING_VALUE);
        jedis.zremrangeByLex(ZSET_BYTE_KEY, "-".getBytes(), "+".getBytes());
        jedis.zremrangeByLex(ZSET_STRING_KEY, "-", "+");
        jedis.zremrangeByRank(ZSET_BYTE_KEY, 0, 1);
        jedis.zremrangeByRank(ZSET_STRING_KEY, 0, 1);
        jedis.zremrangeByScore(ZSET_BYTE_KEY, BYTE_VALUE, BYTE_VALUE);
        jedis.zremrangeByScore(ZSET_BYTE_KEY, 0, 1);
        jedis.zremrangeByScore(ZSET_STRING_KEY, STRING_VALUE, STRING_VALUE);
        jedis.zremrangeByScore(ZSET_STRING_KEY, 0, 1);
        jedis.zrevrangeByScore(ZSET_BYTE_KEY, BYTE_VALUE, BYTE_VALUE);
        jedis.zrevrangeByScore(ZSET_BYTE_KEY, 0, 1);
        jedis.zrevrangeByScore(ZSET_STRING_KEY, STRING_VALUE, STRING_VALUE);
        jedis.zrevrangeByScore(ZSET_STRING_KEY, 0, 1);
        jedis.zrevrangeByScore(ZSET_BYTE_KEY, BYTE_VALUE, BYTE_VALUE, 0, 1);
        jedis.zrevrangeByScore(ZSET_BYTE_KEY, 0, 1, 0, 1);
        jedis.zrevrangeByScore(ZSET_STRING_KEY, STRING_VALUE, STRING_VALUE, 0, 1);
        jedis.zrevrangeByScore(ZSET_STRING_KEY, 0, 1, 0, 1);
        jedis.zrevrangeByScoreWithScores(ZSET_BYTE_KEY, BYTE_VALUE, BYTE_VALUE);
        jedis.zrevrangeByScoreWithScores(ZSET_BYTE_KEY, 0, 1);
        jedis.zrevrangeByScoreWithScores(ZSET_STRING_KEY, STRING_VALUE, STRING_VALUE);
        jedis.zrevrangeByScoreWithScores(ZSET_STRING_KEY, 0, 1);
        jedis.zrevrangeByScoreWithScores(ZSET_BYTE_KEY, BYTE_VALUE, BYTE_VALUE, 0, 1);
        jedis.zrevrangeByScoreWithScores(ZSET_BYTE_KEY, 0, 1, 0, 1);
        jedis.zrevrangeByScoreWithScores(ZSET_STRING_KEY, STRING_VALUE, STRING_VALUE, 0, 1);
        jedis.zrevrangeByScoreWithScores(ZSET_STRING_KEY, 0, 1, 0, 1);
        jedis.zrevrangeWithScores(ZSET_BYTE_KEY, 0, 1);
        jedis.zrevrangeWithScores(ZSET_STRING_KEY, 0, 1);
        jedis.zrevrank(ZSET_BYTE_KEY, BYTE_VALUE);
        jedis.zrevrank(ZSET_STRING_KEY, STRING_VALUE);
        jedis.zscan(ZSET_BYTE_KEY, ScanParams.SCAN_POINTER_START_BINARY);
        jedis.zscan(ZSET_STRING_KEY, 0);
        jedis.zscan(ZSET_STRING_KEY, ScanParams.SCAN_POINTER_START);
        jedis.zscan(ZSET_BYTE_KEY, ScanParams.SCAN_POINTER_START_BINARY, new ScanParams());
        jedis.zscan(ZSET_STRING_KEY, 0, new ScanParams());
        jedis.zscan(ZSET_STRING_KEY, ScanParams.SCAN_POINTER_START, new ScanParams());
        
        jedis.zscore(ZSET_BYTE_KEY, BYTE_VALUE);
        jedis.zscore(ZSET_STRING_KEY, STRING_VALUE);
        
        jedis.zunionstore(SET_BYTE_KEY, SET_BYTE_KEY, SET_BYTE_KEY);
        jedis.zunionstore(SET_STRING_KEY, SET_STRING_KEY, SET_STRING_KEY);
        jedis.zunionstore(SET_BYTE_KEY, new ZParams(), SET_BYTE_KEY);
        jedis.zunionstore(SET_STRING_KEY, new ZParams(), SET_STRING_KEY, SET_STRING_KEY);
        
        jedis.del(BYTE_KEY);
        jedis.del(BYTE_KEY, BYTE_KEY);
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
            if (key instanceof String) {
                jedis.rpush((String)key, STRING_VALUE);
            } else if (key instanceof byte[]) {
                jedis.rpush((byte[])key, BYTE_VALUE);
            }
        }
        
    }
    
   

}

