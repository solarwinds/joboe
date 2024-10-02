package com.appoptics.instrumentation.nosql.mongo2;

import com.tracelytics.instrumentation.ClassInstrumentation;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Base class for Mongodb Instrumentation
 *
 * @author Patson Luk
 *
 */
public abstract class Mongo2BaseInstrumentation extends ClassInstrumentation {
    protected static final String LAYER_NAME = "mongoDB";
    protected static final String FLAVOR = "mongodb";

    //From method name in java to the QueryOp values defined in https://github.com/tracelytics/launchpad/wiki/mongodb-client-spec
    private static final Map<String, String> SPECIAL_METHOD_NAME_TO_QUERY_OP;

    static {
        Map<String, String> methodNameToQueryOp = new HashMap<String, String>();

        methodNameToQueryOp.put("createCollection", "create_collection");
        methodNameToQueryOp.put("createIndex", "create_index");
        methodNameToQueryOp.put("drop", "drop_collection");
        methodNameToQueryOp.put("dropDatabase", "drop");
        methodNameToQueryOp.put("dropIndex", "drop_index");
        methodNameToQueryOp.put("dropIndexes", "drop_indexes");
        methodNameToQueryOp.put("ensureIndex", "ensure_index");
        methodNameToQueryOp.put("findAndModify", "find_and_modify");
        methodNameToQueryOp.put("findOne", "find_one");
        methodNameToQueryOp.put("getCount", "count");
        methodNameToQueryOp.put("getIndexInfo", "index_information");
        methodNameToQueryOp.put("getStats", "stats");
        methodNameToQueryOp.put("mapReduce", "map_reduce");
        methodNameToQueryOp.put("inlineMapReduce", "inline_map_reduce");
        methodNameToQueryOp.put("parallelScan", "parallel_scan");

        SPECIAL_METHOD_NAME_TO_QUERY_OP = Collections.unmodifiableMap(methodNameToQueryOp);
    }

    private static ThreadLocal<Integer> depthThreadLocal = new ThreadLocal<Integer>() {
        protected Integer initialValue() {
            return 0;
        }
    };

    /**
     * Checks whether the current instrumentation should start a new extent. If there is already an active MongoDB extent, then do not start one
     * @return
     */
    protected static boolean shouldStartExtent() {
        int currentDepth = depthThreadLocal.get();
        depthThreadLocal.set(currentDepth + 1);

        if (currentDepth == 0) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Checks whether the current instrumentation should end the current extent. If this is the active MongoDB extent being traced, then ends it
     * @return
     */
    protected static boolean shouldEndExtent() {
        int currentDepth = depthThreadLocal.get();
        depthThreadLocal.set(currentDepth - 1);

        if (currentDepth == 1) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Returns whether there are any active MongoDB extent
     * @return
     */
    static boolean hasActiveExtent() {
        return depthThreadLocal.get() != null && depthThreadLocal.get() > 0;
    }

    /**
     * Map the java method names to QueryOp values defined in https://github.com/tracelytics/launchpad/wiki/mongodb-client-spec 
     * @param methodName
     * @return
     */
    protected static String mapMethodNameToQueryOp(String methodName) {
        if (SPECIAL_METHOD_NAME_TO_QUERY_OP.containsKey(methodName)) {
            return SPECIAL_METHOD_NAME_TO_QUERY_OP.get(methodName);
        } else {
            return methodName;
        }
    }

    public static boolean isEmptyQuery(Object queryObject) {
        if (queryObject instanceof String) {
            String queryString = (String) queryObject;
            return "{}".equals(queryString.replaceAll("\\s+", ""));
        }
        return false;
    }
}
