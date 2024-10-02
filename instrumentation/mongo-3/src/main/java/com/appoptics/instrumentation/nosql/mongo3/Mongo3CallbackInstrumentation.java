package com.appoptics.instrumentation.nosql.mongo3;

import com.google.auto.service.AutoService;
import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtField;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.ext.javassist.CtNewMethod;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.Instrument;
import com.tracelytics.instrumentation.MethodMatcher;
import com.tracelytics.instrumentation.Module;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.Event;
import com.tracelytics.joboe.Metadata;

import java.util.Arrays;
import java.util.List;

/**
 * Patches Mongo 3.x's callback classes to store remote host infor for asynchronous driver instrumentation
 *
 * @author pluk
 *
 */
@AutoService(ClassInstrumentation.class)
@Instrument(targetType = "com.mongodb.async.SingleResultCallback", module = Module.MONGODB)
public class Mongo3CallbackInstrumentation extends Mongo3BaseInstrumentation {
    private static final String CLASS_NAME = Mongo3CallbackInstrumentation.class.getName();

    private enum MethodType { ON_RESULT }

    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<MethodType>> targetMethodMatchers = Arrays.asList(new MethodMatcher<MethodType>("onResult", new String[] { "java.lang.Object", "java.lang.Throwable"}, "void", MethodType.ON_RESULT));

    //keep track of the remote host tagged to the callback such that the actual exit event (reported by another callback instance) can report the value
    private static ThreadLocal<String> remoteHostThreadlocal = new ThreadLocal<String>();

    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
            throws Exception {

        cc.addField(CtField.make("private String tvRemoteHost;", cc));
        cc.addMethod(CtNewMethod.make("public void tvSetRemoteHost(String remoteHost) { tvRemoteHost = remoteHost; } ", cc));
        cc.addMethod(CtNewMethod.make("public String tvGetRemoteHost() { return tvRemoteHost; } ", cc));
        tagInterface(cc, Mongo3Callback.class.getName());

        for (CtMethod method : findMatchingMethods(cc, targetMethodMatchers).keySet()) {
            insertBefore(method, CLASS_NAME + ".preOnResult(this);", false);
            insertAfter(method, CLASS_NAME + ".postOnResult();", true, false);
        }

        return true;
    }

    /**
     * If this callback instance has RemoteHost value set by {@link Mongo3ConnectionInstrumentation},
     * propagate the value by setting it to the threadlocal such that the {@link com.appoptics.apploader.instrumenter.nosql.mongo3.wrapper.CallbackWrapper}
     * span exit can use it later on within the same thread.
     *
     * This necessary as the remote host address set by Mongo3ConnectionInstrumentation is not the same instance as our CallbackWrapper
     * @param callbackObject
     */
    public static void preOnResult(Object callbackObject) {
        Mongo3Callback callback = (Mongo3Callback) callbackObject;

        String remoteHost = callback.tvGetRemoteHost();

        if (remoteHost != null) {
            remoteHostThreadlocal.set(remoteHost);
        }
    }

    public static String getActiveRemoteHost() {
        return remoteHostThreadlocal.get();
    }

    public static void postOnResult() {
        remoteHostThreadlocal.remove();
    }

}