package com.tracelytics.instrumentation.nosql.cassandra;

import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.MethodMatcher;

/**
 * Instrumentation on Cassandra's Cluster, we need to trace its <code>connect</code> method in order to report keyspace name used to obtain a <code>Session</code>
 * @author pluk
 *
 */
public class CassandraClusterInstrumentation extends CassandraBaseInstrumentation {
    private static final String CLASS_NAME = CassandraClusterInstrumentation.class.getName();

    // Several common Instrumented method OpTypes
    private static enum OpType {
        CONNECT
    }

    // List of method matchers that declare method with signature patterns that we want to instrument
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<OpType>> methodMatchers = Arrays.asList(
         new MethodMatcher<OpType>("connect", new String[] { "java.lang.String" }, "com.datastax.driver.core.Session", OpType.CONNECT)
    );
    
    

    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {
        
        for (Entry<CtMethod, OpType> matchingMethodEntry : findMatchingMethods(cc, methodMatchers).entrySet()) {
            CtMethod method = matchingMethodEntry.getKey();
            if (shouldModify(cc, method)) {
                insertAfter(method, CLASS_NAME + ".recordKeyspace($_, $1);", true);
            }
        }

        return true;
    }
   
    public static void recordKeyspace(Object session, String keyspace) {
        if (keyspace != null && session instanceof SessionWithKeyspace) {
            ((SessionWithKeyspace)session).tvSetKeyspace(keyspace);
        }
    }
}