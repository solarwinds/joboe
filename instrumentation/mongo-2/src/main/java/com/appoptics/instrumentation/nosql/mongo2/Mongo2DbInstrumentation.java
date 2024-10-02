package com.appoptics.instrumentation.nosql.mongo2;

import com.google.auto.service.AutoService;
import com.tracelytics.ext.javassist.CannotCompileException;
import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.ext.javassist.NotFoundException;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.Instrument;
import com.tracelytics.instrumentation.MethodMatcher;
import com.tracelytics.instrumentation.Module;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.Event;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Instrumentation on MongoDB's DB. All of the operations on Mongo DB are traced here
 * 
 * @author pluk
 *
 */
@AutoService(ClassInstrumentation.class)
@Instrument(targetType = "com.mongodb.DB", module = Module.MONGODB)
public class Mongo2DbInstrumentation extends Mongo2BaseInstrumentation {
    private static final String CLASS_NAME = Mongo2DbInstrumentation.class.getName();

    private static enum OpType {
        COMMAND, SIMPLE, CREATE_COLLECTION
    }

    // List of method matchers that declare method with signature patterns that we want to instrument
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<OpType>> opMethods = Arrays.asList(
                                                                         //new MethodMatcher<OpType>("command", new String[]{ "com.mongodb.DBObject", "int", "com.mongodb.ReadPreference", "com.mongodb.DBEncoder" }, "com.mongodb.CommandResult", OpType.COMMAND),
                                                                         new MethodMatcher<OpType>("command", new String[] { "com.mongodb.DBObject", "int" }, "com.mongodb.CommandResult", OpType.COMMAND), //2.x signature
                                                                         new MethodMatcher<OpType>("command", new String[] { "com.mongodb.DBObject", "com.mongodb.ReadPreference", "com.mongodb.DBEncoder" }, "com.mongodb.CommandResult", OpType.COMMAND), //3.x signature
                                                                         new MethodMatcher<OpType>("createCollection", new String[] { "java.lang.String" }, "com.mongodb.DBCollection", OpType.CREATE_COLLECTION),
                                                                         new MethodMatcher<OpType>("dropDatabase", new String[] {}, "void", OpType.SIMPLE)
                                                                 );

    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {
        applyOpInstrumentation(cc, opMethods);

        return true;
    }

    private void applyOpInstrumentation(CtClass cc, List<MethodMatcher<OpType>> methodMatchers)
        throws CannotCompileException, NotFoundException {
        Map<CtMethod, OpType> matchingMethods = findMatchingMethods(cc, methodMatchers);

        for (Entry<CtMethod, OpType> matchingMethodEntry : matchingMethods.entrySet()) {
            CtMethod method = matchingMethodEntry.getKey();
            if (shouldModify(cc, method)) {
                OpType opType = matchingMethodEntry.getValue();

                addErrorReporting(method, "com.mongodb.MongoException", LAYER_NAME, classPool);          
                if (opType == OpType.COMMAND) {
                    insertBefore(method, CLASS_NAME + ".layerEntry(getName(), \"" + method.getName() + "\", $1 != null ? java.util.Collections.singletonMap(\"Command\", $1.toString()) : null);");
                } else if (opType == OpType.CREATE_COLLECTION) {
                    insertBefore(method, CLASS_NAME + ".layerEntry(getName(), \"" + method.getName() + "\", $1 != null ? java.util.Collections.singletonMap(\"Collection\", $1) : null);");
                } else if (opType == OpType.SIMPLE) {
                    insertBefore(method, CLASS_NAME + ".layerEntry(getName(), \"" + method.getName() + "\", null);");
                } else {
                    logger.warn("Unknown Mongo DB operation (DB) with mapped enum OpType [" + opType + "]");
                    insertBefore(method, CLASS_NAME + ".layerEntry(getName(), \"" + method.getName() + "\", null);");
                }
                insertAfter(method, CLASS_NAME + ".layerExit();", true);
            }
        }

    }

    public static void layerEntry(String dbName, String op, Map<String, Object> extraKeyValues) {
        if (shouldStartExtent()) {
            Event event = Context.createEvent();
            event.addInfo("Layer", LAYER_NAME,
                          "Label", "entry",
                          "Flavor", FLAVOR,
                          "QueryOp", mapMethodNameToQueryOp(op),
                          "Query", ""); //insert an empty String, this allow correct rendering of query span details on front-end

            if (dbName != null) {
                event.addInfo("Database", dbName);
            }

            if (extraKeyValues != null) {
                for (Entry<String, Object> entry : extraKeyValues.entrySet()) {
                    if (entry.getValue() != null) {
                        event.addInfo(entry.getKey(), entry.getValue());
                    }
                }
            }

            event.report();
        }
    }

    public static void layerExit() {
        if (shouldEndExtent()) {
            Event event = Context.createEvent();
            event.addInfo("Layer", LAYER_NAME,
                          "Label", "exit");

            event.report();
        }
    }

}