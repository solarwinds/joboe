package com.appoptics.instrumentation.nosql.mongo2;

import com.google.auto.service.AutoService;
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
import java.util.Set;

/**
 * Instrumentation of MongoDB's DBPort. Take note that this is close to the lowest level of MongoDB operation before reaching the outgoing network channel. Therefore, this can trace:
 * 1. The actual remote host if the MongoDB client was created with a list of hosts
 * 2. The actual operation that triggers round trips to remote hosts
 * 
 * Take note that if there is already an active MongoDB extent, we will only add the host information to that extent by sending an INFO event. Otherwise, we will create a new extent and use the information from
 * MongoDbCursorInstrumentation to populate the entry event
 * 
 * @author pluk
 *
 */
@AutoService(ClassInstrumentation.class)
@Instrument(targetType = "com.mongodb.DBPort", module = Module.MONGODB, appLoaderPackage = "com.appoptics.apploader.instrumenter.nosql.mongo2")
public class Mongo2DbPortInstrumentation extends Mongo2BaseInstrumentation {
    private static final String CLASS_NAME = Mongo2DbPortInstrumentation.class.getName();
    private static final String INSTRUMENTER_CLASS_NAME = "com.appoptics.apploader.instrumenter.nosql.mongo2.Mongo2QueryInstrumenter";

    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<Integer>> targetMethodMatchers = Arrays.asList(
                                                                                     new MethodMatcher<Integer>("say", new String[] { "com.mongodb.OutMessage" }, "void", 0),
                                                                                     new MethodMatcher<Integer>("call", new String[] { "com.mongodb.OutMessage" }, "com.mongodb.Response", 0)
                                                                             );

    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {

        Set<CtMethod> methods = findMatchingMethods(cc, targetMethodMatchers).keySet();

        boolean supportExtendedInformation = checkSupportExtendedInformation();

        for (CtMethod method : methods) {
            if (shouldModify(cc, method)) {
                if (supportExtendedInformation) { //check whether getOpCode methods are available
                    insertBefore(method,
                            "String query = " + INSTRUMENTER_CLASS_NAME + ".getQuery();" +
                            CLASS_NAME + ".startPortOperation(" +
                                         "host(), " +
                                         "($1 != null && $1.getOpCode() != null) ? $1.getOpCode().toString() : null," +
                                    "query);");

                } else {
                    insertBefore(method,
                            "String query = " + INSTRUMENTER_CLASS_NAME + ".getQuery();" +
                                    CLASS_NAME + ".startPortOperation(host(), null, query);");

                }

                insertAfter(method, CLASS_NAME + ".endPortOperation();", true);
            }
        }
        return true;
    }

    /**
     * Check whether getOpCode method is available. Some older version of MongoDB client do not have that method therefore we should avoid injecting code that invokes that method in those cases
     * @return
     */
    private boolean checkSupportExtendedInformation() {
        try {
            CtClass outMessageClass = classPool.get("com.mongodb.OutMessage");
            try {
                outMessageClass.getMethod("getOpCode", "()Lcom/mongodb/OutMessage$OpCode;");

                return true;
            } catch (NotFoundException e) {
                //cannot find the methods, we cannot extract extended information;
                return false;
            }
        } catch (NotFoundException e) {
            logger.warn("Cannot check signatures for com.mongodb.OutMessage as class cannot be loaded");
            return false;
        }
    }

    public static void startPortOperation(String serverAddress, String opCodeString, String query) {

        Event event = Context.createEvent();

        if (shouldStartExtent()) { //check whether a new extent should be started
            event.addInfo("Layer", LAYER_NAME,
                          "Label", "entry",
                          "Flavor", FLAVOR);

            Mongo2DbCursorInstrumentation.CursorOpInfo cursorOp = Mongo2DbCursorInstrumentation.getCurrentCursorOp();
            if (cursorOp != null) {
                event.addInfo("QueryOp", "find");

                if (cursorOp.getOp() != null) {
                    event.addInfo("CursorOp", cursorOp.getOp());
                }

//                Object queryObject = cursorOp.getQuery();
//                if (queryObject != null) {
//                    if (isEmptyQuery(queryObject.toString())) {
//                        event.addInfo("Query", "all");
//                    } else {
//                        event.addInfo("Query", SANITIZER.sanitize(queryObject));
//                    }
//                }
                if (query != null) {
                    event.addInfo("Query", query);
                }

                if (cursorOp.getDatabaseName() != null) {
                    event.addInfo("Database", cursorOp.getDatabaseName());
                }

                if (cursorOp.getCollectionName() != null) {
                    event.addInfo("Collection", cursorOp.getCollectionName());
                }

                if (cursorOp.getSort() != null) {
                    event.addInfo("Sort", cursorOp.getSort());
                }

                if (cursorOp.getBatchSize() != 0) { //batch size can be negative and it's valid
                    event.addInfo("BatchSize", cursorOp.getBatchSize());
                }

                if (cursorOp.getLimit() > 0) {
                    event.addInfo("Limit", cursorOp.getLimit());
                }

                event.addInfo("CursorId", cursorOp.getId());
            }

        } else { //otherwise do not create an entry event (new extent) but still report an info event
            event.addInfo("Layer", LAYER_NAME,
                          "Label", "info");
        }

        if (serverAddress != null) {
            if (serverAddress.startsWith("/")) {
                serverAddress = serverAddress.substring(1);
            }
            event.addInfo("RemoteHost", serverAddress);
        }
        if (opCodeString != null) {
            event.addInfo("OpCode", opCodeString);
        }

        event.report();

    }

    public static void endPortOperation() {
        if (shouldEndExtent()) {
            Event event = Context.createEvent();
            event.addInfo("Layer", LAYER_NAME,
                          "Label", "exit");
            event.report();
        }
    }

}