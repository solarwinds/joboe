package com.appoptics.instrumentation.nosql.mongo3;

import com.google.auto.service.AutoService;
import com.tracelytics.ext.google.common.cache.Cache;
import com.tracelytics.ext.google.common.cache.CacheBuilder;
import com.tracelytics.ext.google.common.cache.LoadingCache;
import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.*;
import com.tracelytics.instrumentation.Module;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.Event;
import com.tracelytics.joboe.span.impl.ScopeManager;
import com.tracelytics.joboe.span.impl.Span;

import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

/**
 * Instruments the <code>com.mongodb.connection.Connection</code> for actual host and port used for the operation
 *   
 * @author pluk
 *
 */
@AutoService(ClassInstrumentation.class)
@Instrument(targetType = { "com.mongodb.connection.Connection", "com.mongodb.connection.AsyncConnection" } , module = Module.MONGODB)
public class Mongo3ConnectionInstrumentation extends Mongo3BaseInstrumentation {
    private static final String CLASS_NAME = Mongo3ConnectionInstrumentation.class.getName();
    private static final String CALLBACK_WRAPPER_CLASS = "com.appoptics.apploader.instrumenter.nosql.mongo3.wrapper.CallbackWrapper";

    protected enum MethodType { COMMAND, QUERY, INSERT, UPDATE, DELETE, INSERT_COMMAND, UPDATE_COMMAND, DELETE_COMMAND }

    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<MethodType>> syncMethodMatchers = Arrays.asList(new MethodMatcher<MethodType>("command", new String[] { }, "java.lang.Object", MethodType.COMMAND),
                                                                                  new MethodMatcher<MethodType>("query", new String[] { }, "com.mongodb.connection.QueryResult", MethodType.QUERY),
                                                                                  new MethodMatcher<MethodType>("insert", new String[] { }, "com.mongodb.WriteConcernResult", MethodType.INSERT),
                                                                                  new MethodMatcher<MethodType>("update", new String[] { }, "com.mongodb.WriteConcernResult", MethodType.UPDATE),
                                                                                  new MethodMatcher<MethodType>("delete", new String[] { }, "com.mongodb.WriteConcernResult", MethodType.DELETE),
                                                                                  new MethodMatcher<MethodType>("insertCommand", new String[] { }, "com.mongodb.bulk.BulkWriteResult", MethodType.INSERT_COMMAND),
                                                                                  new MethodMatcher<MethodType>("updateCommand", new String[] { }, "com.mongodb.bulk.BulkWriteResult", MethodType.UPDATE_COMMAND),
                                                                                  new MethodMatcher<MethodType>("deleteCommand", new String[] { }, "com.mongodb.bulk.BulkWriteResult", MethodType.DELETE_COMMAND));
    
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<MethodType>> asyncMethodMatchers = Arrays.asList(new MethodMatcher<MethodType>("commandAsync", new String[] { }, "void", MethodType.COMMAND),
                                                                                  new MethodMatcher<MethodType>("queryAsync", new String[] { }, "void", MethodType.QUERY),
                                                                                  new MethodMatcher<MethodType>("insertAsync", new String[] { }, "void", MethodType.INSERT),
                                                                                  new MethodMatcher<MethodType>("updateAsync", new String[] { }, "void", MethodType.UPDATE),
                                                                                  new MethodMatcher<MethodType>("deleteAsync", new String[] { }, "void", MethodType.DELETE),
                                                                                  new MethodMatcher<MethodType>("insertCommandAsync", new String[] { }, "void", MethodType.INSERT_COMMAND),
                                                                                  new MethodMatcher<MethodType>("updateCommandAsync", new String[] { }, "void", MethodType.UPDATE_COMMAND),
                                                                                  new MethodMatcher<MethodType>("deleteCommandAsync", new String[] { }, "void", MethodType.DELETE_COMMAND));
    
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {
        for (Entry<CtMethod, MethodType> entry : findMatchingMethods(cc, syncMethodMatchers).entrySet()) {
            insertBefore(entry.getKey(), CLASS_NAME + ".reportRemoteHost(getDescription() != null ? getDescription().getServerAddress() : null);");
        }
        
        for (Entry<CtMethod, MethodType> entry : findMatchingMethods(cc, asyncMethodMatchers).entrySet()) {
            int callbackIndex = findCallbackParameterIndex(entry.getKey());
            if (callbackIndex >= 0) {
                String callbackToken = "$" + (callbackIndex + 1);
                insertBefore(entry.getKey(), CLASS_NAME + ".setRemoteHostToCallback(getDescription() != null ? getDescription().getServerAddress() : null, " + callbackToken + ");", false);
            }
        }
        
        return true;
    }

    /**
     * Reports the remote host as info event for synchronous operations
     * @param serverAddress
     */
    public static void reportRemoteHost(Object serverAddress) {
        if (serverAddress != null) {
            Span span = ScopeManager.INSTANCE.activeSpan();
            if (span != null) {
                span.setTag("RemoteHost", serverAddress.toString());
            }
        }
    }
    
    /**
     * Sets remote host info to the callback object so the value can be reported on the asynchronous exit event
     * @param serverAddress
     * @param target
     */
    public static void setRemoteHostToCallback(Object serverAddress, Object target) {
        if (serverAddress != null && target instanceof Mongo3Callback) {
            ((Mongo3Callback) target).tvSetRemoteHost(serverAddress.toString());
        }
    }
}