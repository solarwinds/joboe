package com.tracelytics.instrumentation.nosql.redis.jedis;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.MethodMatcher;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.Event;

/**
 * Captures lower level operation sent to Redis server, report KV such as KVOp and RemoteHost. Take note that RemoteHost has to be captured here in lower level 
 * instead of the Jedis object; In shard setup, Jedis object contains the full list of target servers, only until it reaches this lower level will the actual 
 * target server be selected. 
 * 
 * @author Patson Luk
 *
 */
public class RedisJedisConnectionInstrumentation extends ClassInstrumentation {
    private static final String CLASS_NAME = RedisJedisConnectionInstrumentation.class.getName();
    private static final String LAYER_NAME = "redis-jedis";
    private enum OpType { COMMAND_OBJECT, COMMAND_STRING }
    
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<OpType>> methodMatchers = Arrays.asList(
        new MethodMatcher<OpType>("sendCommand", new String[]{ "redis.clients.jedis.Protocol$Command" }, "redis.clients.jedis.Connection", OpType.COMMAND_OBJECT, true), //newer version
        new MethodMatcher<OpType>("sendCommand", new String[]{ "redis.clients.jedis.Protocol$Command", "byte[][]" }, "redis.clients.jedis.Connection", OpType.COMMAND_OBJECT, true), //newer version
        new MethodMatcher<OpType>("sendCommand", new String[]{ "java.lang.String" }, "redis.clients.jedis.Connection", OpType.COMMAND_STRING) //older version
    );

    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
            throws Exception {
        Map<CtMethod, OpType> matchingMethods = findMatchingMethods(cc, methodMatchers);
        
        for (Entry<CtMethod, OpType> matchingMethodEntry : matchingMethods.entrySet()) {
            CtMethod method = matchingMethodEntry.getKey();
            OpType opType = matchingMethodEntry.getValue();
            
            if (opType == OpType.COMMAND_OBJECT) {
                insertBefore(method, CLASS_NAME + ".reportCommand($1 != null ? $1.name() : null, getHost(), getPort());");
            } else if (opType == OpType.COMMAND_STRING) {
                insertBefore(method, CLASS_NAME + ".reportCommand($1, getHost(), getPort());");
            }
        }
        
        return true;
    }
    
    /**
     * Reports an info event for the current operation with KVOp and RemoteHost
     * @param command
     * @param host
     * @param port
     */
    public static void reportCommand(String command, String host, int port) {
        if (command != null) {
            Event event = Context.createEvent();
            event.addInfo("Label", "info",
                          "Layer", LAYER_NAME,
                          "KVOp", command.toLowerCase());
            
            if (host != null) {
                event.addInfo("RemoteHost", host + ":" + port);
            }
            event.report();
        }
    }
}