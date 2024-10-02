package com.appoptics.test.controller;

import org.redisson.client.RedisClient;
import org.redisson.client.RedisClientConfig;
import org.redisson.client.RedisConnection;
import org.redisson.client.RedisException;
import org.redisson.client.codec.Codec;
import org.redisson.client.codec.LongCodec;
import org.redisson.client.codec.StringCodec;
import org.redisson.client.protocol.RedisCommand;
import org.redisson.client.protocol.RedisCommands;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import java.util.concurrent.ExecutionException;

@Controller
public class TestLowLevelAsync extends AbstractRedissonController {
    private RedisConnection connection;

    @GetMapping("/test-low-level-async")
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
        //asyncExec(RedisCommands.ASKING, KEY, VALUE);
        asyncExec2(RedisCommands.AUTH, true, "");


        //connection.bgrewriteaof();
        //connection.bgsave();
        asyncExec(RedisCommands.BITCOUNT, KEY);
        asyncExec(RedisCommands.BITCOUNT, KEY, 0, 1);
        asyncExec(RedisCommands.BITOP, "AND", KEY, KEY);

        asyncExec(RedisCommands.LPUSH, LIST_KEY, VALUE);
        asyncExec(RedisCommands.LPUSH_VOID, LIST_KEY, VALUE);
        asyncExec(RedisCommands.LPUSH_BOOLEAN, LIST_KEY, VALUE);

        asyncExec(RedisCommands.BLPOP, LIST_KEY, TIMEOUT);
        asyncExec(RedisCommands.BRPOP, LIST_KEY, TIMEOUT);
        asyncExec(RedisCommands.BRPOPLPUSH, LIST_KEY, LIST_KEY, TIMEOUT);

        asyncExec(RedisCommands.CLIENT_GETNAME);
        asyncExec(RedisCommands.CLIENT_LIST);
        asyncExec(RedisCommands.CLIENT_SETNAME, "test-client");

        asyncExec(RedisCommands.CONFIG_RESETSTAT);
        asyncExec(RedisCommands.CONFIG_SET, "timeout", asyncExec(RedisCommands.CONFIG_GET, "timeout").get(1));

        asyncExec(RedisCommands.DBSIZE);
        //asyncExec(RedisCommands.DEBUG, KEY);
        asyncExec(RedisCommands.SET, KEY, VALUE);
        asyncExec(RedisCommands.DECR, KEY);

        asyncExec(RedisCommands.DEL, KEY_2);

        String dump = (String) asyncExec(RedisCommands.DUMP, KEY);

        asyncExec(RedisCommands.DEL, KEY);

        //asyncExec(RedisCommands.RESTORE, KEY, EXPIRY, dump.getBytes());

        //asyncExec(RedisCommands.EVAL_STRING, "return { KEYS[1] }", 0, "key1");
        asyncExec(RedisCommands.EVAL_BOOLEAN, "return true", 0);


//        asyncExec(RedisCommands.EVAL_evalsha(stringSha, ScriptOutputType.VALUE);

        asyncExec(RedisCommands.EXISTS, KEY);
//        connection.expire(KEY, EXPIRY);
//        connection.expireat(KEY, new Date(System.currentTimeMillis() + EXPIRY));
//        connection.expireat(KEY, System.currentTimeMillis() + EXPIRY);

        //connection.flushall();
        //connection.flushdb();

        asyncExec(RedisCommands.GET, KEY);
        //connection.getAsync().get(KEY);

        asyncExec(RedisCommands.GETBIT, KEY, 0);
        //connection.getrange(KEY, 0, 1);
        asyncExec(RedisCommands.GETSET, KEY, VALUE);

        asyncExec(RedisCommands.HDEL, HASH_KEY, HASH_FIELD_KEY);
        asyncExec(RedisCommands.HEXISTS, HASH_KEY, HASH_FIELD_KEY);
        asyncExec(RedisCommands.HGET, HASH_KEY, HASH_FIELD_KEY);
        asyncExec(RedisCommands.HGETALL, HASH_KEY);
//        connection.hincrby(HASH_KEY, HASH_FIELD_KEY, 1);
//        connection.hincrbyfloat(HASH_KEY, HASH_FIELD_KEY, "1.0");
        asyncExec(RedisCommands.HKEYS, HASH_KEY);
        asyncExec(RedisCommands.HLEN, HASH_KEY);
        //connection.hmget(HASH_KEY, HASH_FIELD_KEY);
        asyncExec(RedisCommands.HMSET, HASH_KEY, HASH_FIELD_KEY, VALUE);
        asyncExec(RedisCommands.HSET, HASH_KEY, HASH_FIELD_KEY, VALUE);
        asyncExec(RedisCommands.HSETNX, HASH_KEY, HASH_FIELD_KEY, VALUE);
        asyncExec(RedisCommands.HVALS, HASH_KEY);

        asyncExec(RedisCommands.INCR, KEY);
        asyncExec(RedisCommands.INCRBY, KEY, 1);
        asyncExec(RedisCommands.INCRBYFLOAT, KEY, "1.0");

        asyncExec(RedisCommands.INFO_ALL);
        asyncExec(RedisCommands.INFO_SERVER);

        asyncExec(RedisCommands.KEYS, "*");

        asyncExec(RedisCommands.LASTSAVE);
        asyncExec(RedisCommands.LINDEX, LIST_KEY, 0);
        asyncExec(RedisCommands.LINSERT_INT, LIST_KEY, "BEFORE", KEY, VALUE);
        asyncExec(RedisCommands.LLEN_INT, LIST_KEY);
        asyncExec(RedisCommands.LPOP, LIST_KEY);
        asyncExec(RedisCommands.LPUSH, LIST_KEY, VALUE);
        //connection.lpushx(LIST_KEY, VALUE);
        asyncExec(RedisCommands.LRANGE, LIST_KEY, 0, 1);
        asyncExec(RedisCommands.LREM_SINGLE, LIST_KEY, 0, VALUE);
        asyncExec(RedisCommands.LPUSH, LIST_KEY, VALUE);
        asyncExec(RedisCommands.LSET, LIST_KEY, 0, VALUE);
        asyncExec(RedisCommands.LTRIM, LIST_KEY, 0, 1);

        asyncExec(RedisCommands.MGET, KEY, KEY_2);
        //connection.migrate(host, port, key, db, timeout);
        asyncExec(RedisCommands.MOVE, KEY, 1);
        asyncExec(RedisCommands.MSET, KEY, VALUE);
        asyncExec(RedisCommands.MSETNX, KEY, VALUE);

//        connection.objectEncoding(KEY);
//        connection.objectIdletime(KEY);
//        connection.objectRefcount(KEY);

        asyncExec(RedisCommands.PERSIST, KEY);
        asyncExec(RedisCommands.PEXPIRE, KEY, EXPIRY);
        asyncExec(RedisCommands.PEXPIREAT, KEY, System.currentTimeMillis() + EXPIRY);
        asyncExec(RedisCommands.PFADD, LOG_KEY, VALUE);
        asyncExec(RedisCommands.PFCOUNT, LOG_KEY, LOG_KEY);
        asyncExec(RedisCommands.PFMERGE, LOG_KEY, LOG_KEY, LOG_KEY);
        asyncExec(RedisCommands.PING);
        asyncExec(RedisCommands.PSETEX, KEY, EXPIRY, VALUE);
        asyncExec(RedisCommands.PTTL, KEY);

        asyncExec(RedisCommands.RANDOM_KEY);
        asyncExec(RedisCommands.SET, KEY, VALUE);
        asyncExec(RedisCommands.RENAME, KEY, "new-key");
        asyncExec(RedisCommands.RENAMENX, "new-key", KEY);
        asyncExec(RedisCommands.RPOP, LIST_KEY);
        asyncExec(RedisCommands.RPOPLPUSH, LIST_KEY, LIST_KEY);
        asyncExec(RedisCommands.RPUSH, LIST_KEY, VALUE);
//        connection.rpushx(LIST_KEY, VALUE);

        asyncExec(RedisCommands.SADD, SET_KEY, VALUE, VALUE);
        asyncExec(RedisCommands.SAVE);
        asyncExec(RedisCommands.SCARD, SET_KEY);
        String stringSha = asyncExec(RedisCommands.SCRIPT_LOAD, "return {}");
        asyncExec(RedisCommands.SCRIPT_EXISTS, stringSha);
        asyncExec(RedisCommands.SCRIPT_FLUSH);
        asyncExec2(RedisCommands.SCRIPT_KILL, true);
        asyncExec(RedisCommands.SDIFF, SET_KEY, SET_KEY);
        asyncExec(RedisCommands.SDIFFSTORE, SET_KEY, SET_KEY, SET_KEY);
        asyncExec(RedisCommands.SELECT, 0);
        asyncExec(RedisCommands.SET, KEY, VALUE);
        asyncExec(RedisCommands.SETBIT, KEY, 0, 1);
//        connection.setex(KEY, EXPIRY, VALUE);
//        connection.setexnx(KEY, VALUE, EXPIRY);
        asyncExec(RedisCommands.SETNX, KEY, VALUE);
        //connection.setrange(KEY, 0, VALUE);
        //connection.setTimeout(EXPIRY, TimeUnit.SECONDS);

        //connection.shutdown(false);
        asyncExec(RedisCommands.SINTER, SET_KEY, SET_KEY);
        asyncExec(RedisCommands.SINTERSTORE, SET_KEY, SET_KEY, SET_KEY);
        asyncExec(RedisCommands.SISMEMBER, SET_KEY, KEY);
        //connection.slaveof(host, Integer.valueOf(port));
        //connection.slaveofNoOne();
//        connection.slowlogGet();
//        connection.slowlogGet(0);
//        connection.slowlogLen();
//        connection.slowlogReset();
        asyncExec(RedisCommands.SMEMBERS, SET_KEY);
        asyncExec(RedisCommands.SMOVE, SET_KEY, SET_KEY, KEY);
        asyncExec(RedisCommands.SORT_SET, SET_KEY);
        asyncExec(RedisCommands.SORT_LIST, LIST_KEY);
//        connection.sort(SET_KEY, SortArgs.Builder.asc());
//        connection.sortStore(SET_KEY, SortArgs.Builder.asc(), SET_KEY);
        asyncExec(RedisCommands.SPOP, SET_KEY);
        asyncExec(RedisCommands.SRANDMEMBER, SET_KEY, 1);
        asyncExec(RedisCommands.SRANDMEMBER_SINGLE, SET_KEY);
        asyncExec(RedisCommands.SREM_SINGLE, SET_KEY, KEY);
        asyncExec(RedisCommands.SADD, "num-set", "1");
//        connection.sscan("num-set", 1);
        asyncExec(RedisCommands.STRLEN, KEY);
        asyncExec(RedisCommands.SUNION, SET_KEY, SET_KEY);
        asyncExec(RedisCommands.SUNIONSTORE, SET_KEY, SET_KEY, SET_KEY);
        //connection.sync();

        asyncExec(LongCodec.INSTANCE, RedisCommands.TIME);
        //connection.ttl(KEY);
        asyncExec(RedisCommands.TYPE, KEY);

        asyncExec(RedisCommands.ZADD, ZSET_KEY, 1, "2");
        asyncExec(RedisCommands.ZCARD, ZSET_KEY);
        asyncExec(RedisCommands.ZCOUNT, ZSET_KEY, VALUE, VALUE);
        asyncExec(RedisCommands.ZCOUNT, ZSET_KEY, 0, 1);
        asyncExec(RedisCommands.ZINCRBY, ZSET_KEY, 1, VALUE);
        asyncExec(RedisCommands.ZINTERSTORE_INT, "dest", 2, ZSET_KEY, ZSET_KEY);
        asyncExec(RedisCommands.ZRANGE, ZSET_KEY, 0, 1);
        asyncExec(RedisCommands.ZRANGE_SINGLE, ZSET_KEY, 0, 0);
        asyncExec(RedisCommands.ZRANGEBYSCORE, ZSET_KEY, VALUE, VALUE);
//        connection.zrangebyscoreWithScores(ZSET_KEY, VALUE, VALUE);
//        connection.zrangebyscoreWithScores(ZSET_KEY, 0, 1);
//        connection.zrangebyscoreWithScores(ZSET_KEY, VALUE, VALUE, 0, 1);
//        connection.zrangebyscoreWithScores(ZSET_KEY, 0, 1, 0, 1);
//        connection.zrangeWithScores(ZSET_KEY, 0, 1);
        asyncExec(RedisCommands.ZRANK, ZSET_KEY, VALUE);
        asyncExec(RedisCommands.ZREM, ZSET_KEY, VALUE);
        asyncExec(RedisCommands.ZREMRANGEBYRANK, ZSET_KEY, 0, 1);
        asyncExec(RedisCommands.ZREMRANGEBYSCORE, ZSET_KEY, VALUE, VALUE);
        asyncExec(RedisCommands.ZREVRANGEBYSCORE, ZSET_KEY, VALUE, VALUE);
//        connection.zrevrangebyscoreWithScores(ZSET_KEY, VALUE, VALUE);
//        connection.zrevrangebyscoreWithScores(ZSET_KEY, 0, 1);
//        connection.zrevrangebyscoreWithScores(ZSET_KEY, VALUE, VALUE, 0, 1);
//        connection.zrevrangebyscoreWithScores(ZSET_KEY, 0, 1, 0, 1);
//        connection.zrevrangeWithScores(ZSET_KEY, 0, 1);
        asyncExec(RedisCommands.ZREVRANK, ZSET_KEY, VALUE);
//        connection.zscan(ZSET_KEY, 0);

        asyncExec(RedisCommands.ZSCORE, ZSET_KEY, VALUE);

        asyncExec(RedisCommands.ZUNIONSTORE_INT, "dest", 2, SET_KEY, SET_KEY);

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
        printToOutput("Finished testing on async connection");
        return getModelAndView("index");
    }

    private <T> T asyncExec(RedisCommand<T> command, Object... params) throws ExecutionException, InterruptedException {
        return asyncExec(StringCodec.INSTANCE, command, params);
    }

    private <T> T asyncExec(Codec codec, RedisCommand<T> command, Object... params) throws ExecutionException, InterruptedException {
        return asyncExec2(codec, command, false, params);
    }

    private <T> T asyncExec2(RedisCommand<T> command, boolean ignoreException, Object... params) throws ExecutionException, InterruptedException {
        return asyncExec2(StringCodec.INSTANCE, command, ignoreException, params);
    }

    private <T> T asyncExec2(Codec codec, RedisCommand<T> command, boolean ignoreException, Object... params) throws ExecutionException, InterruptedException {
        try {
            return (T) connection.async(codec, command, params).get();
        } catch (Exception e) {
            if (!ignoreException) {
                throw e;
            } else {
                return null;
            }
        }
    }
}