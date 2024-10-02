package com.tracelytics.instrumentation.cache.ehcache;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.MethodMatcher;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.Event;

/**
 * Instruments the net.sf.ehcache.Ehcache class which provides methods to access the cache. Most of the basic operations should be captured here. 
 * 
 * Take note that we do not currently capture search/query string as it is not available
 * @author pluk
 *
 */
public class EhcacheInstrumentation extends ClassInstrumentation {
    private static final String CLASS_NAME = EhcacheInstrumentation.class.getName();
    private static final String LAYER_NAME = "ehcache";
    
    private static ThreadLocal<Integer> depthThreadLocal = new ThreadLocal<Integer>() {
        protected Integer initialValue() {
            return 0;
        }
    };
            
            
    // List of method matchers that declare method with signature patterns that we want to instrument
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<OpType>> methodMatchers = Arrays.asList(
        new MethodMatcher<OpType>("get", new String[]{ "java.lang.Object" }, "net.sf.ehcache.Element", OpType.INST_OP_KEY_RETURN),
        new MethodMatcher<OpType>("getAll", new String[]{ "java.util.Collection" }, "java.util.Map", OpType.INST_OP_KEY_RETURN),
        new MethodMatcher<OpType>("getAllWithLoader", new String[]{ "java.util.Collection", "java.lang.Object" }, "java.util.Map", OpType.INST_OP_KEY_RETURN),
        new MethodMatcher<OpType>("getKeys", new String[]{ }, "java.util.List", OpType.INST_OP_RETURN),
        new MethodMatcher<OpType>("getKeysNoDuplicateCheck", new String[]{ }, "java.util.List", OpType.INST_OP_RETURN),
        new MethodMatcher<OpType>("getKeysWithExpiryCheck", new String[]{ }, "java.util.List", OpType.INST_OP_RETURN),
        new MethodMatcher<OpType>("getQuiet", new String[]{ "java.lang.Object" }, "net.sf.ehcache.Element", OpType.INST_OP_KEY_RETURN),
        new MethodMatcher<OpType>("getWithLoader", new String[]{ "java.lang.Object" }, "net.sf.ehcache.Element", OpType.INST_OP_KEY_RETURN),
        
        new MethodMatcher<OpType>("load", new String[]{ "java.lang.Object" }, "void", OpType.INST_OP_KEY),
        new MethodMatcher<OpType>("loadAll", new String[]{ "java.util.Collection", "java.lang.Object" }, "void", OpType.INST_OP_KEY),
        new MethodMatcher<OpType>("put", new String[]{ "net.sf.ehcache.Element" }, "void", OpType.INST_OP_KEY),
        new MethodMatcher<OpType>("putAll", new String[]{ "java.util.Collection" }, "void", OpType.INST_OP_KEY),
        new MethodMatcher<OpType>("putIfAbsent", new String[]{ "net.sf.ehcache.Element" }, "net.sf.ehcache.Element", OpType.INST_OP_KEY_RETURN),
        new MethodMatcher<OpType>("putQuiet", new String[]{ "net.sf.ehcache.Element" }, "void", OpType.INST_OP_KEY),
        new MethodMatcher<OpType>("putWithWriter", new String[]{ "net.sf.ehcache.Element" }, "void", OpType.INST_OP_KEY),
        
        new MethodMatcher<OpType>("remove", new String[]{ "java.lang.Object" }, "boolean", OpType.INST_OP_KEY_RETURN),
        new MethodMatcher<OpType>("removeAll", new String[]{ }, "void", OpType.INST_OP, true),
        new MethodMatcher<OpType>("removeAll", new String[]{ "boolean" }, "void", OpType.INST_OP),
        new MethodMatcher<OpType>("removeAll", new String[]{ "java.util.Collection" }, "void", OpType.INST_OP_KEY),
        new MethodMatcher<OpType>("removeElement", new String[]{ "net.sf.ehcache.Element"}, "boolean", OpType.INST_OP_KEY_RETURN),
        new MethodMatcher<OpType>("removeQuiet", new String[]{ "java.lang.Object" }, "boolean", OpType.INST_OP_KEY_RETURN),
        new MethodMatcher<OpType>("removeWithWriter", new String[]{ "java.lang.Object" }, "boolean", OpType.INST_OP_KEY_RETURN),
        
        new MethodMatcher<OpType>("replace", new String[]{ "net.sf.ehcache.Element"}, "net.sf.ehcache.Element", OpType.INST_OP_KEY_RETURN),
        new MethodMatcher<OpType>("replace", new String[]{ "net.sf.ehcache.Element", "net.sf.ehcache.Element"}, "boolean", OpType.INST_OP_KEY_RETURN),
        
        new MethodMatcher<OpType>("executeQuery", new String[] {"net.sf.ehcache.store.StoreQuery"}, "net.sf.ehcache.search.Results", OpType.INST_OP_RETURN, true),
        
        new MethodMatcher<OpType>("acquireReadLockOnKey", new String[]{ "java.lang.Object" }, "void", OpType.INST_OP_KEY),
        new MethodMatcher<OpType>("acquireWriteLockOnKey", new String[]{ "java.lang.Object" }, "void", OpType.INST_OP_KEY),
        new MethodMatcher<OpType>("releaseReadLockOnKey", new String[]{ "java.lang.Object" }, "void", OpType.INST_OP_KEY),
        new MethodMatcher<OpType>("releaseWriteLockOnKey", new String[]{ "java.lang.Object" }, "void", OpType.INST_OP_KEY),
        new MethodMatcher<OpType>("tryReadLockOnKey", new String[]{ "java.lang.Object" }, "boolean", OpType.INST_OP_KEY_RETURN),
        new MethodMatcher<OpType>("tryWriteLockOnKey", new String[]{ "java.lang.Object" }, "boolean", OpType.INST_OP_KEY_RETURN)
    );

    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
            throws Exception {
        Map<CtMethod, OpType> matchingMethods = findMatchingMethods(cc, methodMatchers);
        
        for (Entry<CtMethod, OpType> matchingMethodEntry : matchingMethods.entrySet()) {
            CtMethod method = matchingMethodEntry.getKey();
            OpType opType = matchingMethodEntry.getValue();
            
            String keyParamToken = opType.hasKey ? "($w)$1" : "null"; 
            
            //layer entry code, if the key parameter is type net.sf.ehcache.Element, we would get its object key instead
            insertBefore(method, 
                         CLASS_NAME + ".layerEntry(getName(), \"" + method.getName() + "\", " + keyParamToken + ");");
            
            insertAfter(method, CLASS_NAME + ".layerExit(" + opType.traceReturn + ", " +  (opType.traceReturn ? "($w)$_" : "null") + ");", true);
        }
        
        return true;
    }

    /**
     * Creates layer entry event for ehcache operation
     * @param cacheName name of the ehcache
     * @param op    ehcache operation name
     * @param key   key used for this operation, we will include it in the KV in forms acceptable to bson event
     */
    public static void layerEntry(String cacheName, String op, Object key) {
        if (shouldStartExtent()) {
            Event event = Context.createEvent();
            event.addInfo("Layer", LAYER_NAME,
                          "Label", "entry",
                          "KVOp", op);
            
            if (cacheName != null) {
                event.addInfo("CacheName", cacheName);
            }
            
            if (key != null) {
                if (key instanceof EhcacheElement) {
                    key = ((EhcacheElement)key).getObjectKey(); //if it is an EhcacheElement, then extract its object key for the "KVKey"/"KVKeyCount" logic below
                }
                
                if (key instanceof String) {
                    event.addInfo("KVKey", key);
                } else if (key instanceof Number || key instanceof Character || key instanceof UUID || key instanceof HibernateCacheKey) {
                    event.addInfo("KVKey", key.toString());
                } else if (key instanceof Collection) {
                    event.addInfo("KVKeyCount", ((Collection<?>)key).size());
                } else if (key.getClass().isArray()) {
                    event.addInfo("KVKeyCount", ((Object[])key).length);
                } else { //report the class name and identity hash if as a catch all
                    event.addInfo("KVKey", key.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(key)));
                }
            }
            
            event.report();
        }
    }

    /**
     * Creates an exit event for the ehcache operation
     * @param traceReturn   whether to include the return value in the event
     * @param returnValue   the value of the operation result
     */
    public static void layerExit(boolean traceReturn, Object returnValue) {
        if (shouldEndExtent()) {
            Event event = Context.createEvent();
            event.addInfo("Layer", LAYER_NAME,
                          "Label", "exit");
            
            if (traceReturn) {
                if (returnValue instanceof EhcacheElement) {
                    EhcacheElement returnElement = (EhcacheElement) returnValue;
                    event.addInfo("KVCreationTime", new Date(returnElement.getCreationTime()));
                    event.addInfo("KVAccumlativeHitCount", returnElement.getHitCount());
                    event.addInfo("KVLastAccessTime", new Date(returnElement.getLastAccessTime()));
                    event.addInfo("KVLastUpdateTime", new Date(returnElement.getLastUpdateTime()));
                    event.addInfo("KVVersion", returnElement.getVersion());
                    event.addInfo("KVHit", true);
                } else if (returnValue instanceof Map) {
                    int hitCount = 0;
                    for (Object value : ((Map<?, ?>)returnValue).values()) {
                        if (value != null) { //null value indicates not found in ehcache
                            hitCount ++;
                        }
                    }
                    event.addInfo("KVHitCount", hitCount);
                } else if (returnValue instanceof EhcacheSearchResults) {
                    event.addInfo("KVHitCount", ((EhcacheSearchResults)returnValue).size());
                } else if (returnValue instanceof Collection<?>) {
                    event.addInfo("KvHitCount", ((Collection<?>)returnValue).size());
                } else if (returnValue instanceof Boolean) {
                    event.addInfo("KvHit", returnValue);
                } else {
                    event.addInfo("KVHit", returnValue != null);
                }
            }
    
            event.report();
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
    
    private enum OpType {
        INST_OP(false, false), INST_OP_KEY(true, false), INST_OP_KEY_RETURN(true, true), INST_OP_RETURN(false, true);
        
        private boolean hasKey;
        private boolean traceReturn;

        OpType(boolean hasKey, boolean traceReturn) {
            this.hasKey = hasKey;
            this.traceReturn = traceReturn;
        }
    }
}