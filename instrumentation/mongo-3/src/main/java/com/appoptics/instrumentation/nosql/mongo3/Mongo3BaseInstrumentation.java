package com.appoptics.instrumentation.nosql.mongo3;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.ext.javassist.NotFoundException;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.FrameworkVersion;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Base class for Mongodb Instrumentation
 *
 * @author Patson Luk
 *
 */
public abstract class Mongo3BaseInstrumentation extends ClassInstrumentation {
    public static final String LAYER_NAME = "mongoDB";
    public static final String FLAVOR = "mongodb";

    //From method name in java to the QueryOp values defined in https://github.com/tracelytics/launchpad/wiki/mongodb-client-spec
    private static final Map<String, String> SPECIAL_METHOD_NAME_TO_QUERY_OP;
    private static final FrameworkVersion V4_0 = new FrameworkVersion(4, 0);
    private static final FrameworkVersion V3_0 = new FrameworkVersion(3, 0);

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
    
    protected static boolean isEmptyQuery(Object queryObject) {
        if (queryObject instanceof String) {
            String queryString = (String) queryObject;
            return queryString != null && "{}".equals(queryString.replaceAll("\\s+", ""));
        }
        return false;
    }

    protected FrameworkVersion getVersion() {
        try {
            classPool.get("com.mongodb.internal.operation.WriteOperation");
            return V4_0;
        } catch (NotFoundException e) {
            logger.debug("Cannot load com.mongodb.internal.operation.WriteOperation, running Mongo db 3.x");
            return V3_0;
        }
    }

    protected int getMajorVersion() {
        return getVersion().getMajorVersion();
    }


    protected int findCallbackParameterIndex(CtMethod method) {
        try {
            CtClass callbackClass = getMajorVersion() >= 4 ? classPool.getCtClass("com.mongodb.internal.async.SingleResultCallback") : classPool.getCtClass("com.mongodb.async.SingleResultCallback");

            int index = 0;
            for (CtClass parameterType : method.getParameterTypes()) {
                if (callbackClass.equals(parameterType)) {
                    return index;
                }
                index ++;
            }
        } catch (NotFoundException e) {

        }
        return -1;
    }
}