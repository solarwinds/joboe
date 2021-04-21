package com.tracelytics.instrumentation.nosql.redis.jedis;

import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.ext.javassist.NotFoundException;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.MethodMatcher;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.Event;

/**
 * Instruments the higher level calls to the Jedis library - {@code Jedis}, {@code BinaryJedis}, {@code Pipeline}, {@code Transaction}. 
 * 
 * They all share a very similar sets of operations and each of the operation is to be captured as an individual extent
 * 
 * Take note that for the lower level information such as "KVOp" (actual Redis Command) and "RemoteHost" are captured by {@link RedisJedisConnectionInstrumentation} instead
 * 
 * @author Patson Luk
 *
 */
public class RedisJedisInstrumentation extends ClassInstrumentation {
    private static final String CLASS_NAME = RedisJedisInstrumentation.class.getName();
    private static final String LAYER_NAME = "redis-jedis";
    private static final int MAX_KEY_LENGTH = 100; //max length allowed for reporting the Redis operation param key/script

    private static ThreadLocal<Integer> depthThreadLocal = new ThreadLocal<Integer>() {
        protected Integer initialValue() {
            return 0;
        }
    };
    
    private enum OpType { GET , EVAL, AUTH, MULTI, EXEC, DISCARD, SYNC, POP_TIMEOUT, BITOP, STREAM }
    // List of method matchers that declare method with signature patterns that we want to instrument
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<OpType>> methodMatchers = Arrays.asList(
        //JedisCommands                                                                              
        new MethodMatcher<OpType>("get" , new String[] {}, "java.lang.Object", OpType.GET),
        new MethodMatcher<OpType>("set" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("get" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("exists" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("persist" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("type" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("expire" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("expireAt" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("ttl" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("setbit" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("getbit" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("setrange" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("getrange" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("getSet" , new String[] {}, "java.lang.Object", OpType.GET),
        new MethodMatcher<OpType>("setnx" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("setex" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("decrBy" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("decr" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("incrBy" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("incr" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("append" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("substr" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("hset" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("hget" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("hsetnx" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("hmset" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("hmget" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("hincrBy" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("hexists" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("hdel" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("hlen" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("hkeys" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("hvals" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("hgetAll" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("rpush" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("lpush" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("llen" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("lrange" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("ltrim" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("lindex" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("lset" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("lrem" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("lpop" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("rpop" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("sadd" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("smembers" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("srem" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("spop" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("scard" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("sismember" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("srandmember" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("strlen" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("zadd" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("zrange" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("zrem" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("zincrby" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("zrank" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("zrevrank" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("zrevrange" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("zrangeWithScores" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("zrevrangeWithScores" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("zcard" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("zscore" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("sort" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("zcount" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("zrangeByScore" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("zrevrangeByScore" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("zrangeByScoreWithScores" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("zrevrangeByScoreWithScores" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("zremrangeByRank" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("zremrangeByScore" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("zlexcount" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("zrangeByLex" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("zremrangeByLex" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("linsert" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("lpushx" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("rpushx" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("blpop" , new String[] { "int", "java.lang.Object"}, "java.lang.Object", OpType.POP_TIMEOUT), 
        new MethodMatcher<OpType>("blpop" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("brpop" , new String[] { "int", "java.lang.Object"}, "java.lang.Object", OpType.POP_TIMEOUT),
        new MethodMatcher<OpType>("brpop" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("del" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("echo" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("move" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("bitcount" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("hscan" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("sscan" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("zscan" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("pfadd" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("pfcount" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("pfcount" , new String[] {}, "long"), //3 inconsistent signatures that use primitive long as return value

        //MultiKeyCommands
        new MethodMatcher<OpType>("del" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("blpop" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("brpop" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("keys" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("mget" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("mset" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("msetnx" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("rename" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("renamenx" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("rpoplpush" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("sdiff" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("sdiffstore" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("sinter" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("sinterstore" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("smove" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("sort" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("sunion" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("sunionstore" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("watch" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("unwatch" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("zinterstore" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("zunionstore" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("brpoplpush" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("publish" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("randomKey" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("bitop" , new String[] { "redis.clients.jedis.BitOP", "java.lang.Object" }, "java.lang.Object", OpType.BITOP),
        new MethodMatcher<OpType>("scan" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("pfmerge" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("pfcount" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("randomBinaryKey" , new String[] {}, "java.lang.Object"),
        

        //AdvancedJedisCommands
        new MethodMatcher<OpType>("configGet" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("configSet" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("slowlogReset" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("slowlogLen" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("slowlogGet" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("objectRefcount" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("objectEncoding" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("objectIdletime" , new String[] {}, "java.lang.Object"),

        //ScriptingCommands
        new MethodMatcher<OpType>("eval" , new String[] {}, "java.lang.Object", OpType.EVAL),
        new MethodMatcher<OpType>("evalsha" , new String[] {}, "java.lang.Object", OpType.EVAL),
        new MethodMatcher<OpType>("scriptExists" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("scriptLoad" , new String[] {}, "java.lang.Object"),

        //BasicCommands
        new MethodMatcher<OpType>("ping" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("quit" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("flushDB" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("dbSize" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("select" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("flushAll" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("auth" , new String[] {}, "java.lang.Object", OpType.AUTH),
        new MethodMatcher<OpType>("save" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("bgsave" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("bgrewriteaof" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("lastsave" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("shutdown" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("info" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("slaveof" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("slaveofNoOne" , new String[] {}, "java.lang.Object"),
        //new MethodMatcher<OpType>("getDB" , new String[] {}, "java.lang.Object"), //short and does not issue command to server
        new MethodMatcher<OpType>("debug" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("configResetStat" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("waitReplicas" , new String[] {}, "java.lang.Object"),

        //ClusterCommands
        new MethodMatcher<OpType>("clusterNodes" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("clusterMeet" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("clusterAddSlots" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("clusterDelSlots" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("clusterInfo" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("clusterGetKeysInSlot" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("clusterSetSlotNode" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("clusterSetSlotMigrating" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("clusterSetSlotImporting" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("clusterSetSlotStable" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("clusterForget" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("clusterFlushSlots" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("clusterKeySlot" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("clusterCountKeysInSlot" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("clusterSaveConfig" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("clusterReplicate" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("clusterSlaves" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("clusterFailover" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("clusterSlots" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("clusterReset" , new String[] {}, "java.lang.Object"),
        
        //BinaryJedis
        new MethodMatcher<OpType>("hincrByFloat" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("incrByFloat" , new String[] {}, "java.lang.Object"),
        
        //MultiKeyBinaryCommands
        new MethodMatcher<OpType>("randomBinaryKey" , new String[] {}, "java.lang.Object"),

        //BinaryScriptingCommands
        new MethodMatcher<OpType>("scriptFlush" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("scriptKill" , new String[] {}, "java.lang.Object"),
        
        //AdvancedBinaryJedisCommands
        new MethodMatcher<OpType>("slowlogGetBinary" , new String[] {}, "java.lang.Object"),
        
        //not included in any interface
        new MethodMatcher<OpType>("bitpos" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("pexpire" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("pexpireAt" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("pttl" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("psetex" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("clientKill" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("clientGetname" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("clientList" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("clientSetname" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("dump" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("restore" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("multi" , new String[] {}, "java.lang.Object", OpType.MULTI),
        new MethodMatcher<OpType>("time" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("migrate" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("sentinelMasters" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("sentinelGetMasterAddrByName" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("sentinelReset" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("sentinelSlaves" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("sentinelFailover" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("sentinelMonitor" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("sentinelRemove" , new String[] {}, "java.lang.Object"),
        new MethodMatcher<OpType>("sentinelSet" , new String[] {}, "java.lang.Object"),
        
        //Transaction
        new MethodMatcher<OpType>("exec" , new String[] {}, "java.util.List", OpType.EXEC),
        new MethodMatcher<OpType>("execGetResponse" , new String[] {}, "java.util.List", OpType.EXEC),
        new MethodMatcher<OpType>("discard" , new String[] {}, "java.lang.Object", OpType.DISCARD), //newer version
        new MethodMatcher<OpType>("discard" , new String[] {}, "void", OpType.DISCARD), //older version
        
        //Pipeline
        new MethodMatcher<OpType>("sync" , new String[] {}, "void", OpType.SYNC),        
        new MethodMatcher<OpType>("syncAndReturnAll" , new String[] {}, "java.util.List", OpType.SYNC),

        //Stream
        new MethodMatcher<OpType>("xadd" , new String[] {}, "java.lang.Object", OpType.STREAM),
        new MethodMatcher<OpType>("xlen" , new String[] {}, "java.lang.Object", OpType.STREAM),
        new MethodMatcher<OpType>("xrange" , new String[] {}, "java.lang.Object", OpType.STREAM),
        new MethodMatcher<OpType>("xrevrange" , new String[] {}, "java.lang.Object", OpType.STREAM),
        new MethodMatcher<OpType>("xread" , new String[] {}, "java.lang.Object", OpType.STREAM),
        new MethodMatcher<OpType>("xack" , new String[] {}, "long", OpType.STREAM),
        new MethodMatcher<OpType>("xack" , new String[] {}, "java.lang.Object", OpType.STREAM),
        new MethodMatcher<OpType>("xgroupCreate" , new String[] {}, "java.lang.Object", OpType.STREAM),
        new MethodMatcher<OpType>("xgroupSetID" , new String[] {}, "java.lang.Object", OpType.STREAM),
        new MethodMatcher<OpType>("xgroupDestroy" , new String[] {}, "long", OpType.STREAM),
        new MethodMatcher<OpType>("xgroupDestroy" , new String[] {}, "java.lang.Object", OpType.STREAM),
        new MethodMatcher<OpType>("xgroupDelConsumer" , new String[] {}, "java.lang.Object", OpType.STREAM),
        new MethodMatcher<OpType>("xinfoConsumers" , new String[] {}, "java.lang.Object", OpType.STREAM),
        new MethodMatcher<OpType>("xdel" , new String[] {}, "long", OpType.STREAM),
        new MethodMatcher<OpType>("xdel" , new String[] {}, "java.lang.Object", OpType.STREAM),
        new MethodMatcher<OpType>("xtrim" , new String[] {}, "long", OpType.STREAM),
        new MethodMatcher<OpType>("xtrim" , new String[] {}, "java.lang.Object", OpType.STREAM),
        new MethodMatcher<OpType>("xreadGroup" , new String[] {}, "java.lang.Object", OpType.STREAM),
        new MethodMatcher<OpType>("xpending" , new String[] {}, "java.lang.Object", OpType.STREAM),
        new MethodMatcher<OpType>("xclaim" , new String[] {}, "java.lang.Object", OpType.STREAM),
        new MethodMatcher<OpType>("xinfoStream" , new String[] {}, "java.lang.Object", OpType.STREAM),
        new MethodMatcher<OpType>("xinfoGroup" , new String[] {}, "java.lang.Object", OpType.STREAM)
    );

    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
            throws Exception {
        tagInterfaces(cc);
        
        for (Entry<CtMethod, OpType> methodEntry : findMatchingMethods(cc, methodMatchers).entrySet()) {
            CtMethod method = methodEntry.getKey();
            OpType type = methodEntry.getValue();
            
            if (type == OpType.AUTH) {
                insertBefore(method, CLASS_NAME + ".layerEntry(\"" + method.getName() + "\", null, null, this);"); //do not show the password!
            } else if (type == OpType.EVAL){
                insertBefore(method, CLASS_NAME + ".layerEntry(\"" + method.getName() + "\", null, $1, this);");
            } else { //capture as normal KVKey
                String keyToken;
                if (type == OpType.POP_TIMEOUT || type == OpType.BITOP) { //the key is in 2nd argument
                    keyToken = "($w)$2";
                } else if (method.getParameterTypes().length >= 1) {
                    keyToken = "($w)$1";
                } else {
                    keyToken = "null";
                }
                
                insertBefore(method, CLASS_NAME + ".layerEntry(\"" + method.getName() + "\", " + keyToken + ", null, this);");
            }
            
            
            if (type == OpType.GET) { //instrument return
                insertAfter(method, CLASS_NAME + ".layerExit(true, ($w)$_);", true);
            } else {
                insertAfter(method, CLASS_NAME + ".layerExit(false, null);", true);
            }
        }
        

        
        return true;
    }

    /**
     * Tag the interface class {@link RedisJedisTransaction} or {@link RedisJedisPipeline} to the Jedis {@code Transaction} or {@code Pipeline} such that
     * we can identify them using {@code instanceof} later on in injected code
     *  
     * @param cc    class to be identified
     */
    private void tagInterfaces(CtClass cc) {
        try {
            CtClass transactionClass = classPool.get("redis.clients.jedis.Transaction");
            if (cc.subtypeOf(transactionClass)) {
                tagInterface(cc, RedisJedisTransaction.class.getName());
            }
        } catch (NotFoundException e) {
            logger.warn("Cannot load the expected redis.clients.jedis.Transaction class, isTransaction flag will not be set in Jedis");
        }
        
        try {
            CtClass pipelineClass = classPool.get("redis.clients.jedis.Pipeline");
            if (cc.subtypeOf(pipelineClass)) {
                tagInterface(cc, RedisJedisPipeline.class.getName());
            }
        } catch (NotFoundException e) {
            logger.debug("Cannot load redis.clients.jedis.Pipline class, probably running in an older version of Jedis"); //OK, older version does not have pipeline
        }
    }
    
    

    public static void layerEntry(String methodName, Object key, Object script, Object jedisObject) {
        if (shouldStartExtent()) {
            Event event = Context.createEvent();
            event.addInfo("Layer", LAYER_NAME,
                          "Label", "entry",
                          "MethodName", methodName);
            
            String formattedKey = toStringKey(key); //format the param key, it could be byte[] or String
            if (formattedKey != null) {
                event.addInfo("KVKey", formattedKey);
            }
            
            String formattedScript = toStringKey(script);
            if (formattedScript != null) {
                event.addInfo("Script", formattedScript);
            }
            
            if (jedisObject instanceof RedisJedisTransaction) {
                event.addInfo("IsTransaction", true);
            } else if (jedisObject instanceof RedisJedisPipeline) {
                event.addInfo("IsPipelined", true);                
            }
            
            event.report();
        }
    }
        

    public static void layerExit(boolean traceReturn, Object returnValue) {
        if (shouldEndExtent()) {
            Event event = Context.createEvent();
            event.addInfo("Layer", LAYER_NAME,
                          "Label", "exit");
            if (traceReturn) {
                event.addInfo("KVHit", returnValue != null);
            }            
            event.report();
        }
    }
    
    
    /**
     * Formats the input param key to String values
     * @param   sourceKey input param key
     * @return  formatted key in String
     */
    private static String toStringKey(Object sourceKey) {
        if (sourceKey instanceof String) {
            return formatString((String) sourceKey);
        } else if (sourceKey instanceof String[]) { 
            String[] sourceArray = (String[]) sourceKey;
            if (sourceArray.length == 1) { //only report single value based on spec https://github.com/tracelytics/launchpad/wiki/Redis-client-spec
                return formatString(sourceArray[0]);
            }
        } else if (sourceKey instanceof byte[]) {
           return formatByteArray((byte[]) sourceKey);
        } else if (sourceKey instanceof byte[][]) {
            byte[][] sourceArray = (byte[][]) sourceKey;
            if (sourceArray.length == 1) { //only report single value based on spec https://github.com/tracelytics/launchpad/wiki/Redis-client-spec
                return formatByteArray(sourceArray[0]);
            }
        }
        
        return null;
    }
    
    private static String formatString(String sourceString) {
        if (sourceString.length() > MAX_KEY_LENGTH) {
            return sourceString.substring(0, MAX_KEY_LENGTH) + "...(" + (sourceString.length() - MAX_KEY_LENGTH) + " characters truncated)" ;
        } else {
            return sourceString;
        }
    }
    
    private static String formatByteArray(byte[] sourceByteArray) {
        if (sourceByteArray.length > MAX_KEY_LENGTH) {
            byte[] truncatedByte = new byte[MAX_KEY_LENGTH];
            System.arraycopy(sourceByteArray, 0, truncatedByte, 0, MAX_KEY_LENGTH); //make a truncated copy first to reduce memory usage
            return new String(truncatedByte) + "...(" + (sourceByteArray.length - MAX_KEY_LENGTH) + " byte truncated)";
        } else {
            return new String(sourceByteArray);
        }
    }
    
    /**
     * Checks whether the current instrumentation should start a new extent. If there is already an active extent, then do not start one
     * @return
     */
    protected static boolean shouldStartExtent() {
        int currentDepth = depthThreadLocal.get();
        depthThreadLocal.set(currentDepth + 1);

        return currentDepth == 0;
    }

    /**
     * Checks whether the current instrumentation should end the current extent. If this is the active extent being traced, then ends it
     * @return
     */
    protected static boolean shouldEndExtent() {
        int currentDepth = depthThreadLocal.get();
        depthThreadLocal.set(currentDepth - 1);

        return currentDepth == 1;
    }
}