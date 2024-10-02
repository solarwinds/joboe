package com.appoptics.instrumentation.nosql.mongo3;

import com.google.auto.service.AutoService;
import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.Instrument;
import com.tracelytics.instrumentation.MethodMatcher;
import com.tracelytics.instrumentation.Module;

import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

/**
 * Capture connection exception for later version of mongo client which the connection no longer happens within the operation itself
 */
@AutoService(ClassInstrumentation.class)
@Instrument(targetType = "com.mongodb.client.internal.MongoClientDelegate", module = Module.MONGODB)
public class Mongo3ClientDelegateInstrumentation extends Mongo3BaseInstrumentation {
    private enum MethodType { CREATE_CLIENT_SESSION }

    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<MethodType>> targetMethodMatchers = Arrays.asList(new MethodMatcher<MethodType>("createClientSession", new String[] {}, "com.mongodb.client.ClientSession", MethodType.CREATE_CLIENT_SESSION));

    

    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {

        for (Entry<CtMethod, MethodType> methodEntry : findMatchingMethods(cc, targetMethodMatchers).entrySet()) {
            addErrorReporting(methodEntry.getKey(), "com.mongodb.MongoException", LAYER_NAME, classPool);
        }

        return true;
    }
}