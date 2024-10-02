package com.appoptics.instrumentation.nosql.mongo4;

import com.appoptics.instrumentation.nosql.mongo3.Mongo3ConnectionInstrumentation;
import com.google.auto.service.AutoService;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.Instrument;
import com.tracelytics.instrumentation.Module;

/**
 * Same instrumentation as the Mongo 3 version except that the target class package is `com.mongo.internal.connection` instead of
 * `com.mongo.connection`
 * @author pluk
 *
 */
@AutoService(ClassInstrumentation.class)
@Instrument(targetType = { "com.mongodb.internal.connection.Connection", "com.mongodb.internal.connection.AsyncConnection" } , module = Module.MONGODB)
public class Mongo4ConnectionInstrumentation extends Mongo3ConnectionInstrumentation {
}