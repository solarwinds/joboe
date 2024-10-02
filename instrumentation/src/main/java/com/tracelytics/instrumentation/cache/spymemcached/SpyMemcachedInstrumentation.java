package com.tracelytics.instrumentation.cache.spymemcached;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.tracelytics.ext.javassist.CannotCompileException;
import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtField;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.ext.javassist.NotFoundException;
import com.tracelytics.ext.javassist.expr.ExprEditor;
import com.tracelytics.ext.javassist.expr.MethodCall;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.MethodMatcher;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.Event;
import com.tracelytics.joboe.OboeException;

/**
 * Instrumentation for Spy memcached client. Spy memcached offers mostly asynchronous operations. Therefore a single memcached operation usually would generate
 * 2 extends. For example client.set("key", "value"), would first generate an extend that represents the start and end of the client.set() method, which itself
 * only represents the time it takes to prepare and submit the asynchronous operations. The actual submitted asynchronous operation will be traced in another extend.
 * The second extend will be connected back to the first extend so we can generate the graph properly on front-end.
 *
 * @author Patson Luk
 *
 */
public class SpyMemcachedInstrumentation extends ClassInstrumentation {
    private static final String CLASS_NAME = SpyMemcachedInstrumentation.class.getName();
    private static final String LAYER_NAME = "spymemcached";
    
    // Several common Instrumented method OpTypes
    private static final OpType
            INST_OP = new OpType(),           // Generic op method: just logs the op (method)
            INST_OP_KEY = new OpType(0);       // Op with key: first param is key, returns value is ignored
            
            
    // List of method matchers that declare method with signature patterns that we want to instrument
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<OpType>> opMethods = Arrays.asList(
        new MethodMatcher<OpType>("add", new String[]{ "java.lang.String", "int" }, "java.util.concurrent.Future", new OpType(0, 1)),
        new MethodMatcher<OpType>("append", new String[]{ "java.lang.String" }, "java.util.concurrent.Future", INST_OP_KEY),
        new MethodMatcher<OpType>("append", new String[]{ "long", "java.lang.String" }, "java.util.concurrent.Future", new OpType(1)),
        new MethodMatcher<OpType>("asyncCAS", new String[]{ "java.lang.String", "long", "int" }, "java.util.concurrent.Future", new OpType(0, 2)),
        new MethodMatcher<OpType>("asyncCAS", new String[]{ "java.lang.String" }, "java.util.concurrent.Future", INST_OP_KEY),
        new MethodMatcher<OpType>("asyncDecr", new String[]{ "java.lang.String" }, "java.util.concurrent.Future", INST_OP_KEY),
        new MethodMatcher<OpType>("asyncGet", new String[]{ "java.lang.String" }, "java.util.concurrent.Future", INST_OP_KEY),
        new MethodMatcher<OpType>("asyncGetAndLock", new String[]{ "java.lang.String", "int" }, "java.util.concurrent.Future", new OpType(0, 1)),
        new MethodMatcher<OpType>("asyncGetAndTouch", new String[]{ "java.lang.String", "int" }, "java.util.concurrent.Future", new OpType(0, 1)),
        new MethodMatcher<OpType>("asyncGetBulk", new String[] { "net.spy.memcached.transcoders.Transcoder", "java.lang.String[]" }, "java.util.concurrent.Future", new OpType(1)),
        new MethodMatcher<OpType>("asyncGetBulk", new String[] { }, "java.util.concurrent.Future", INST_OP_KEY),
        new MethodMatcher<OpType>("asyncGets", new String[]{ "java.lang.String" }, "java.util.concurrent.Future", INST_OP_KEY),
        new MethodMatcher<OpType>("asyncIncr", new String[]{ "java.lang.String" }, "java.util.concurrent.Future", INST_OP_KEY),
        new MethodMatcher<OpType>("cas", new String[]{ "java.lang.String", "long", "int" }, "net.spy.memcached.CASResponse", new OpType(0, 2)),
        new MethodMatcher<OpType>("cas", new String[]{ "java.lang.String" }, "net.spy.memcached.CASResponse", INST_OP_KEY),
        new MethodMatcher<OpType>("decr", new String[]{ "java.lang.String" , "int", "long", "int"}, "long", new OpType(0, 3)),
        new MethodMatcher<OpType>("decr", new String[]{ "java.lang.String" , "long", "long", "int"}, "long", new OpType(0, 3)),
        new MethodMatcher<OpType>("decr", new String[]{ "java.lang.String" }, "long", INST_OP_KEY),
        new MethodMatcher<OpType>("delete", new String[]{ "java.lang.String" }, "java.util.concurrent.Future", INST_OP_KEY),
        new MethodMatcher<OpType>("flush", new String[0], "java.util.concurrent.Future", INST_OP),
        new MethodMatcher<OpType>("get", new String[]{ "java.lang.String" }, "java.lang.Object", new OpType(0, null, true)),
        new MethodMatcher<OpType>("getAndTouch", new String[]{ "java.lang.String" }, "net.spy.memcached.CASValue", new OpType(0, 1, true)),
        new MethodMatcher<OpType>("getAndLock", new String[]{ "java.lang.String" }, "net.spy.memcached.CASValue", new OpType(0, 1, true)),
        new MethodMatcher<OpType>("getBulk", new String[] { "net.spy.memcached.transcoders.Transcoder", "java.lang.String[]" }, "java.util.Map", new OpType(1, null, true)),
        new MethodMatcher<OpType>("getBulk", new String[] { }, "java.util.Map", new OpType(0, null, true)),
        new MethodMatcher<OpType>("gets", new String[]{ "java.lang.String" }, "net.spy.memcached.CASValue", new OpType(0, null, true)),
        new MethodMatcher<OpType>("incr", new String[]{ "java.lang.String" , "int", "long", "int"}, "long", new OpType(0, 3)),
        new MethodMatcher<OpType>("incr", new String[]{ "java.lang.String" , "long", "long", "int"}, "long", new OpType(0, 3)),
        new MethodMatcher<OpType>("incr", new String[]{ "java.lang.String" }, "long", INST_OP_KEY),
        new MethodMatcher<OpType>("prepend", new String[]{ "java.lang.String" }, "java.util.concurrent.Future", INST_OP_KEY),
        new MethodMatcher<OpType>("prepend", new String[]{ "long", "java.lang.String" }, "java.util.concurrent.Future", new OpType(1)),
        new MethodMatcher<OpType>("replace", new String[]{ "java.lang.String", "int" }, "java.util.concurrent.Future", new OpType(0, 1)),
        new MethodMatcher<OpType>("set", new String[]{ "java.lang.String", "int" }, "java.util.concurrent.Future", new OpType(0, 1)),
        new MethodMatcher<OpType>("touch", new String[]{ "java.lang.String", "int" }, "java.util.concurrent.Future", new OpType(0, 1))
    );

    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
            throws Exception {
        applyOpInstrumentation(cc, opMethods);
        
        return true;
    }

    private void applyOpInstrumentation(CtClass cc, List<MethodMatcher<OpType>> methodMatchers) throws CannotCompileException, NotFoundException {
        /**
         * Stores the "depth" of the calls in order to avoid multiple layers of duplicated calls caused by method calling its overloading version. For example if
         * get(String) calls get(String, Transcoder), we would only want to trace the start and end of get(String), not both the methods. This field is used
         * to figure out which method call is the "outer most" one in order to report entry and exit events
         *   
         */
        cc.addField(CtField.make("private ThreadLocal tvDepth = new ThreadLocal();", cc));
        
        Map<CtMethod, OpType> matchingMethods = findMatchingMethods(cc, methodMatchers);
        
        for (Entry<CtMethod, OpType> matchingMethodEntry : matchingMethods.entrySet()) {
            CtMethod method = matchingMethodEntry.getKey();
            OpType opType = matchingMethodEntry.getValue();
            
            String keyParamToken = opType.keyParamIndex != null ?  "$" + (opType.keyParamIndex + 1) : "null"; 
            String expirationParamToken = opType.expirationParamIndex != null ?  "Integer.valueOf($" + (opType.expirationParamIndex + 1) + ")" : "null";
            
            insertBefore(method, CLASS_NAME + ".layerEntry(tvDepth, \"" + method.getName() + "\", " + keyParamToken + ", " + expirationParamToken + ");");
            
            insertAfter(method, CLASS_NAME + ".layerExit(tvDepth, " + opType.traceReturn + ", " +  (opType.traceReturn ? "$_ instanceof net.spy.memcached.CASValue ? ((net.spy.memcached.CASValue)$_).getValue() : $_" : "null") + ");", true, false);
            
        }
        
    }

    public static void layerEntry(ThreadLocal<Integer> tvDepth, String op, Object key, Integer expiration) {
        if (shouldTraceEntry(tvDepth)) {
            Event event = Context.createEvent();
            event.addInfo("Layer", LAYER_NAME,
                          "Label", "entry",
                          "KVOp", op);
            
            if (key != null) {
                if (key instanceof String) {
                    event.addInfo("KVKey", key);
                } else if (key instanceof Collection) {
                    event.addInfo("KVKeyCount", ((Collection<?>)key).size());
                } else if (key instanceof String[]) {
                    event.addInfo("KVKeyCount", ((String[])key).length);
                }
            }
            
            if (expiration != null) {
                event.addInfo("TTL", expiration);
            }

            event.report();
        }
    }

    public static void layerExit(ThreadLocal<Integer> depth, boolean traceReturn, Object returnValue) {
        if (shouldTraceExit(depth)) {
            Event event = Context.createEvent();
            event.addInfo("Layer", LAYER_NAME,
                          "Label", "exit");
            
            if (traceReturn) {
                if (returnValue instanceof Map) {
                    event.addInfo("KVHitCount", ((Map<?, ?>)returnValue).size());
                } else {
                    event.addInfo("KVHit", returnValue != null);
                }
            }
    
            event.report();
        }
    }
    
    private static boolean shouldTraceEntry(ThreadLocal<Integer> depth) {
        if (depth.get() == null) {
            depth.set(1);
            return true;
        } else {
            depth.set(depth.get() + 1);
            return false;
        }
    }
    
    private static boolean shouldTraceExit(ThreadLocal<Integer> depth) {
        if (depth.get() == null) {
            return false;
        } else if (depth.get() > 1){
            depth.set(depth.get() - 1);
            return false;
        } else {
            depth.remove();
            return true;
        }
    }
    
    private static class OpType {
        private Integer keyParamIndex;
        private Integer expirationParamIndex;
        private boolean traceReturn;
        
        public OpType() {
            this(null);
        }
        public OpType(Integer keyParamIndex) {
            this(keyParamIndex, null);
        }
        
        public OpType(Integer keyParamIndex, Integer expirationParamIndex) {
            this(keyParamIndex, expirationParamIndex, false);
        }
        
        public OpType(Integer keyParamIndex, Integer expirationParamIndex, boolean traceReturn) {
            this.keyParamIndex = keyParamIndex;
            this.expirationParamIndex = expirationParamIndex;
            this.traceReturn = traceReturn;
        }
        
        
    }
}