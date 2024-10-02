package com.appoptics.test.controller;

import org.redisson.api.*;
import org.redisson.api.listener.MessageListener;
import org.redisson.api.map.MapLoader;
import org.redisson.api.mapreduce.RCollectionMapper;
import org.redisson.api.mapreduce.RCollector;
import org.redisson.api.mapreduce.RMapper;
import org.redisson.api.mapreduce.RReducer;
import org.redisson.client.RedisClient;
import org.redisson.client.RedisClientConfig;
import org.redisson.client.RedisConnection;
import org.redisson.client.RedisException;
import org.redisson.client.codec.Codec;
import org.redisson.client.codec.LongCodec;
import org.redisson.client.codec.StringCodec;
import org.redisson.client.protocol.RedisCommand;
import org.redisson.client.protocol.RedisCommands;
import org.redisson.codec.KryoCodec;
import org.redisson.config.Config;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Controller
public class TestLowLevel extends AbstractRedissonController {
    private RedisConnection connection;

    @GetMapping("/test-low-level")
    public ModelAndView test(Model model) throws InterruptedException, ExecutionException {
        RedisClientConfig config = new RedisClientConfig();
        config.setAddress(host, port);
        RedisClient client = RedisClient.create(config);

        connection = client.connect();

//        connection.set(KEY, VALUE);
//        connection.get(KEY);
//
//        connection.del("random-list");
//        connection.lpush("random-list", LIST);
//        connection.sortStore("random-list", SortArgs.Builder.asc(), "new-list");
        //syncExec(RedisCommands.ASKING, KEY, VALUE);
        syncExec2(RedisCommands.AUTH, true, "");


        //connection.bgrewriteaof();
        //connection.bgsave();
        syncExec(RedisCommands.BITCOUNT, KEY);
        syncExec(RedisCommands.BITCOUNT, KEY, 0, 1);
        syncExec(RedisCommands.BITOP, "AND", KEY, KEY);

        syncExec(RedisCommands.LPUSH, LIST_KEY, VALUE);
        syncExec(RedisCommands.LPUSH_VOID, LIST_KEY, VALUE);
        syncExec(RedisCommands.LPUSH_BOOLEAN, LIST_KEY, VALUE);

        syncExec(RedisCommands.BLPOP, LIST_KEY, TIMEOUT);
        syncExec(RedisCommands.BRPOP, LIST_KEY, TIMEOUT);
        syncExec(RedisCommands.BRPOPLPUSH, LIST_KEY, LIST_KEY, TIMEOUT);

        syncExec(RedisCommands.CLIENT_GETNAME);
        syncExec(RedisCommands.CLIENT_LIST);
        syncExec(RedisCommands.CLIENT_SETNAME, "test-client");

        syncExec(RedisCommands.CONFIG_RESETSTAT);
        syncExec(RedisCommands.CONFIG_SET, "timeout", syncExec(RedisCommands.CONFIG_GET, "timeout").get(1));

        syncExec(RedisCommands.DBSIZE);
        //syncExec(RedisCommands.DEBUG, KEY);
        syncExec(RedisCommands.SET, KEY, VALUE);
        syncExec(RedisCommands.DECR, KEY);

        syncExec(RedisCommands.DEL, KEY_2);

        String dump = (String) syncExec(RedisCommands.DUMP, KEY);

        syncExec(RedisCommands.DEL, KEY);

        //syncExec(RedisCommands.RESTORE, KEY, EXPIRY, dump.getBytes());

        //syncExec(RedisCommands.EVAL_STRING, "return { KEYS[1] }", 0, "key1");
        syncExec(RedisCommands.EVAL_BOOLEAN, "return true", 0);


//        syncExec(RedisCommands.EVAL_evalsha(stringSha, ScriptOutputType.VALUE);

        syncExec(RedisCommands.EXISTS, KEY);
//        connection.expire(KEY, EXPIRY);
//        connection.expireat(KEY, new Date(System.currentTimeMillis() + EXPIRY));
//        connection.expireat(KEY, System.currentTimeMillis() + EXPIRY);

        //connection.flushall();
        //connection.flushdb();

        syncExec(RedisCommands.GET, KEY);
        //connection.getAsync().get(KEY);

        syncExec(RedisCommands.GETBIT, KEY, 0);
        //connection.getrange(KEY, 0, 1);
        syncExec(RedisCommands.GETSET, KEY, VALUE);

        syncExec(RedisCommands.HDEL, HASH_KEY, HASH_FIELD_KEY);
        syncExec(RedisCommands.HEXISTS, HASH_KEY, HASH_FIELD_KEY);
        syncExec(RedisCommands.HGET, HASH_KEY, HASH_FIELD_KEY);
        syncExec(RedisCommands.HGETALL, HASH_KEY);
//        connection.hincrby(HASH_KEY, HASH_FIELD_KEY, 1);
//        connection.hincrbyfloat(HASH_KEY, HASH_FIELD_KEY, "1.0");
        syncExec(RedisCommands.HKEYS, HASH_KEY);
        syncExec(RedisCommands.HLEN, HASH_KEY);
        //connection.hmget(HASH_KEY, HASH_FIELD_KEY);
        syncExec(RedisCommands.HMSET, HASH_KEY, HASH_FIELD_KEY, VALUE);
        syncExec(RedisCommands.HSET, HASH_KEY, HASH_FIELD_KEY, VALUE);
        syncExec(RedisCommands.HSETNX, HASH_KEY, HASH_FIELD_KEY, VALUE);
        syncExec(RedisCommands.HVALS, HASH_KEY);

        syncExec(RedisCommands.INCR, KEY);
        syncExec(RedisCommands.INCRBY, KEY, 1);
        syncExec(RedisCommands.INCRBYFLOAT, KEY, "1.0");

        syncExec(RedisCommands.INFO_ALL);
        syncExec(RedisCommands.INFO_SERVER);

        syncExec(RedisCommands.KEYS, "*");

        syncExec(RedisCommands.LASTSAVE);
        syncExec(RedisCommands.LINDEX, LIST_KEY, 0);
        syncExec(RedisCommands.LINSERT_INT, LIST_KEY, "BEFORE", KEY, VALUE);
        syncExec(RedisCommands.LLEN_INT, LIST_KEY);
        syncExec(RedisCommands.LPOP, LIST_KEY);
        syncExec(RedisCommands.LPUSH, LIST_KEY, VALUE);
        //connection.lpushx(LIST_KEY, VALUE);
        syncExec(RedisCommands.LRANGE, LIST_KEY, 0, 1);
        syncExec(RedisCommands.LREM_SINGLE, LIST_KEY, 0, VALUE);
        syncExec(RedisCommands.LPUSH, LIST_KEY, VALUE);
        syncExec(RedisCommands.LSET, LIST_KEY, 0, VALUE);
        syncExec(RedisCommands.LTRIM, LIST_KEY, 0, 1);

        syncExec(RedisCommands.MGET, KEY, KEY_2);
        //connection.migrate(host, port, key, db, timeout);
        syncExec(RedisCommands.MOVE, KEY, 1);
        syncExec(RedisCommands.MSET, KEY, VALUE);
        syncExec(RedisCommands.MSETNX, KEY, VALUE);

//        connection.objectEncoding(KEY);
//        connection.objectIdletime(KEY);
//        connection.objectRefcount(KEY);

        syncExec(RedisCommands.PERSIST, KEY);
        syncExec(RedisCommands.PEXPIRE, KEY, EXPIRY);
        syncExec(RedisCommands.PEXPIREAT, KEY, System.currentTimeMillis() + EXPIRY);
        syncExec(RedisCommands.PFADD, LOG_KEY, VALUE);
        syncExec(RedisCommands.PFCOUNT, LOG_KEY, LOG_KEY);
        syncExec(RedisCommands.PFMERGE, LOG_KEY, LOG_KEY, LOG_KEY);
        syncExec(RedisCommands.PING);
        syncExec(RedisCommands.PSETEX, KEY, EXPIRY, VALUE);
        syncExec(RedisCommands.PTTL, KEY);

        syncExec(RedisCommands.RANDOM_KEY);
        syncExec(RedisCommands.SET, KEY, VALUE);
        syncExec(RedisCommands.RENAME, KEY, "new-key");
        syncExec(RedisCommands.RENAMENX, "new-key", KEY);
        syncExec(RedisCommands.RPOP, LIST_KEY);
        syncExec(RedisCommands.RPOPLPUSH, LIST_KEY, LIST_KEY);
        syncExec(RedisCommands.RPUSH, LIST_KEY, VALUE);
//        connection.rpushx(LIST_KEY, VALUE);

        syncExec(RedisCommands.SADD, SET_KEY, VALUE, VALUE);
        syncExec(RedisCommands.SAVE);
        syncExec(RedisCommands.SCARD, SET_KEY);
        String stringSha = syncExec(RedisCommands.SCRIPT_LOAD, "return {}");
        syncExec(RedisCommands.SCRIPT_EXISTS, stringSha);
        syncExec(RedisCommands.SCRIPT_FLUSH);
        syncExec2(RedisCommands.SCRIPT_KILL, true);
        syncExec(RedisCommands.SDIFF, SET_KEY, SET_KEY);
        syncExec(RedisCommands.SDIFFSTORE, SET_KEY, SET_KEY, SET_KEY);
        syncExec(RedisCommands.SELECT, 0);
        syncExec(RedisCommands.SET, KEY, VALUE);
        syncExec(RedisCommands.SETBIT, KEY, 0, 1);
//        connection.setex(KEY, EXPIRY, VALUE);
//        connection.setexnx(KEY, VALUE, EXPIRY);
        syncExec(RedisCommands.SETNX, KEY, VALUE);
        //connection.setrange(KEY, 0, VALUE);
        //connection.setTimeout(EXPIRY, TimeUnit.SECONDS);

        //connection.shutdown(false);
        syncExec(RedisCommands.SINTER, SET_KEY, SET_KEY);
        syncExec(RedisCommands.SINTERSTORE, SET_KEY, SET_KEY, SET_KEY);
        syncExec(RedisCommands.SISMEMBER, SET_KEY, KEY);
        //connection.slaveof(host, Integer.valueOf(port));
        //connection.slaveofNoOne();
//        connection.slowlogGet();
//        connection.slowlogGet(0);
//        connection.slowlogLen();
//        connection.slowlogReset();
        syncExec(RedisCommands.SMEMBERS, SET_KEY);
        syncExec(RedisCommands.SMOVE, SET_KEY, SET_KEY, KEY);
        syncExec(RedisCommands.SORT_SET, SET_KEY);
        syncExec(RedisCommands.SORT_LIST, LIST_KEY);
//        connection.sort(SET_KEY, SortArgs.Builder.asc());
//        connection.sortStore(SET_KEY, SortArgs.Builder.asc(), SET_KEY);
        syncExec(RedisCommands.SPOP, SET_KEY);
        syncExec(RedisCommands.SRANDMEMBER, SET_KEY, 1);
        syncExec(RedisCommands.SRANDMEMBER_SINGLE, SET_KEY);
        syncExec(RedisCommands.SREM_SINGLE, SET_KEY, KEY);
        syncExec(RedisCommands.SADD, "num-set", "1");
//        connection.sscan("num-set", 1);
        syncExec(RedisCommands.STRLEN, KEY);
        syncExec(RedisCommands.SUNION, SET_KEY, SET_KEY);
        syncExec(RedisCommands.SUNIONSTORE, SET_KEY, SET_KEY, SET_KEY);
        //connection.sync();

        syncExec(LongCodec.INSTANCE, RedisCommands.TIME);
        //connection.ttl(KEY);
        syncExec(RedisCommands.TYPE, KEY);

        syncExec(RedisCommands.ZADD, ZSET_KEY, 1, "2");
        syncExec(RedisCommands.ZCARD, ZSET_KEY);
        syncExec(RedisCommands.ZCOUNT, ZSET_KEY, VALUE, VALUE);
        syncExec(RedisCommands.ZCOUNT, ZSET_KEY, 0, 1);
        syncExec(RedisCommands.ZINCRBY, ZSET_KEY, 1, VALUE);
        syncExec(RedisCommands.ZINTERSTORE_INT, "dest", 2, ZSET_KEY, ZSET_KEY);
        syncExec(RedisCommands.ZRANGE, ZSET_KEY, 0, 1);
        syncExec(RedisCommands.ZRANGE_SINGLE, ZSET_KEY, 0, 0);
        syncExec(RedisCommands.ZRANGEBYSCORE, ZSET_KEY, VALUE, VALUE);
//        connection.zrangebyscoreWithScores(ZSET_KEY, VALUE, VALUE);
//        connection.zrangebyscoreWithScores(ZSET_KEY, 0, 1);
//        connection.zrangebyscoreWithScores(ZSET_KEY, VALUE, VALUE, 0, 1);
//        connection.zrangebyscoreWithScores(ZSET_KEY, 0, 1, 0, 1);
//        connection.zrangeWithScores(ZSET_KEY, 0, 1);
        syncExec(RedisCommands.ZRANK, ZSET_KEY, VALUE);
        syncExec(RedisCommands.ZREM, ZSET_KEY, VALUE);
        syncExec(RedisCommands.ZREMRANGEBYRANK, ZSET_KEY, 0, 1);
        syncExec(RedisCommands.ZREMRANGEBYSCORE, ZSET_KEY, VALUE, VALUE);
        syncExec(RedisCommands.ZREVRANGEBYSCORE, ZSET_KEY, VALUE, VALUE);
//        connection.zrevrangebyscoreWithScores(ZSET_KEY, VALUE, VALUE);
//        connection.zrevrangebyscoreWithScores(ZSET_KEY, 0, 1);
//        connection.zrevrangebyscoreWithScores(ZSET_KEY, VALUE, VALUE, 0, 1);
//        connection.zrevrangebyscoreWithScores(ZSET_KEY, 0, 1, 0, 1);
//        connection.zrevrangeWithScores(ZSET_KEY, 0, 1);
        syncExec(RedisCommands.ZREVRANK, ZSET_KEY, VALUE);
//        connection.zscan(ZSET_KEY, 0);

        syncExec(RedisCommands.ZSCORE, ZSET_KEY, VALUE);

        syncExec(RedisCommands.ZUNIONSTORE_INT, "dest", 2, SET_KEY, SET_KEY);

        connection.closeAsync();

//
//        RedisConnection<Object, Object> kryoConnection = client.connect(new RedisCodecWrapper(new KryoCodec()));
//        kryoConnection.set(KEY, VALUE);
//        kryoConnection.set(1, 1);
//        kryoConnection.set('a', 'a');
//        kryoConnection.set(1.0, 1.0);
//        kryoConnection.close();
//
//        client.shutdown();
//
        printToOutput("Finished testing on sync connection");
        return getModelAndView("index");
    }

    private <T> T syncExec(RedisCommand<T> command, Object... params) {
        return syncExec(StringCodec.INSTANCE, command, params);
    }

    private <T> T syncExec(Codec codec, RedisCommand<T> command, Object... params) {
        return syncExec2(codec, command, false, params);
    }

    private <T> T syncExec2(RedisCommand<T> command, boolean ignoreException, Object... params) {
        return syncExec2(StringCodec.INSTANCE, command, ignoreException, params);
    }

    private <T> T syncExec2(Codec codec, RedisCommand<T> command, boolean ignoreException, Object... params) {
        try {
            return connection.sync(codec, command, params);
        } catch (RedisException e) {
            if (!ignoreException) {
                throw e;
            } else {
                return null;
            }
        }
    }
}