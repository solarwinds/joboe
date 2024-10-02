package com.appoptics.instrumentation.nosql.mongo4;

import com.appoptics.instrumentation.nosql.mongo3.Mongo3OperationInstrumentation;
import com.google.auto.service.AutoService;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.Instrument;
import com.tracelytics.instrumentation.Module;


/**
 * Same instrumentation as the Mongo 3 version except that the target class package is `com.mongo.internal.operation` instead of
 * `com.mongo.operation`
 *
 * Some of the class name/method lookup method has been overwritten to properly handle mongo 4 operations
 *
 * @author pluk
 */
@AutoService(ClassInstrumentation.class)
@Instrument(targetType = { "com.mongodb.internal.operation.WriteOperation", "com.mongodb.internal.operation.ReadOperation" }, module = Module.MONGODB, appLoaderPackage = { "com.appoptics.apploader.instrumenter.nosql.mongo3", "com.appoptics.apploader.instrumenter.nosql.mongo4", "com.appoptics.apploader.instrumenter.nosql.mongo4.wrapper" })
public class Mongo4OperationInstrumentation extends Mongo3OperationInstrumentation {
    private static final String CALLBACK_WRAPPER_CLASS = "com.appoptics.apploader.instrumenter.nosql.mongo4.wrapper.CallbackWrapper";
    @Override
    protected String getCallbackWrapperClassName() {
        return CALLBACK_WRAPPER_CLASS;
    }

    @Override
    protected String getQualifiedClassName(String classNameSegment) {
        return "com.mongodb.internal." + classNameSegment;
    }

    @Override
    protected String getUpdateMethodName() {
        return "getUpdateValue";
    }
}