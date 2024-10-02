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
 * Instrumentation on MongoDB's DBCollection. All of the operations on Mongo DB Collection are traced here
 * @author pluk
 *
 */
@AutoService(ClassInstrumentation.class)
@Instrument(targetType = "com.mongodb.DBCollection", module = Module.MONGODB, appLoaderPackage = "com.appoptics.apploader.instrumenter.nosql.mongo2")
public class Mongo2DbCollectionInstrumentation extends Mongo2BaseInstrumentation {
    private static final String CLASS_NAME = Mongo2DbCollectionInstrumentation.class.getName();
    private static final String INSTRUMENTER_CLASS_NAME = "com.appoptics.apploader.instrumenter.nosql.mongo2.Mongo2QueryInstrumenter";

    // Several common Instrumented method OpTypes
    private static enum OpType {
        SIMPLE, FIND_ALL, FIND, FIND_ONE, QUERY, DISTINCT, GROUP, INSERT_1, INSERT_2, MAP_REDUCE_1, MAP_REDUCE_2, RENAME, FIND_AND_MODIFY, UPDATE, AGGREGATE_OLD, AGGREGATE_NEW, INDEX, PARALLEL_SCAN
    }

    // List of method matchers that declare method with signature patterns that we want to instrument
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<OpType>> opMethods = Arrays.asList(
         //new MethodMatcher<OpType>("addOption", new String[]{}, "void", OpType.SIMPLE),
         new MethodMatcher<OpType>("aggregate", new String[] { "com.mongodb.DBObject", "com.mongodb.DBObject[]"}, "java.lang.Object", OpType.AGGREGATE_OLD),
         new MethodMatcher<OpType>("aggregate", new String[] { "java.util.List"}, "java.lang.Object", OpType.AGGREGATE_NEW),
         //new MethodMatcher<OpType>("count", new String[]{}, "long", OpType.INST_OP),
         //new MethodMatcher<OpType>("getCount",  new String[] {"com.mongodb.DBObject", "com.mongodb.DBObject", "long", "long", "com.mongodb.ReadPreference" }, "long", OpType.QUERY),
         new MethodMatcher<OpType>("getCount", new String[] { "com.mongodb.DBObject", "com.mongodb.DBObject", "long", "long" }, "long", OpType.QUERY),
         //new MethodMatcher<OpType>("createIndex", new String[]{ "com.mongodb.DBObject", "com.mongodb.DBObject", "com.mongodb.DBEncoder" }, "void", OpType.QUERY),
         new MethodMatcher<OpType>("createIndex", new String[] { "com.mongodb.DBObject", "com.mongodb.DBObject" }, "void", OpType.QUERY),
         //new MethodMatcher<OpType>("distinct", new String[]{ "java.lang.String", "com.mongodb.DBObject", "com.mongodb.ReadPreference" }, "java.util.List", OpType.DISTINCT_1),
         new MethodMatcher<OpType>("distinct", new String[] { "java.lang.String", "com.mongodb.DBObject" }, "java.util.List", OpType.DISTINCT),
         new MethodMatcher<OpType>("drop", new String[] {}, "void", OpType.SIMPLE),
         new MethodMatcher<OpType>("dropIndex", new String[] { "java.lang.String" }, "void", OpType.INDEX),
         new MethodMatcher<OpType>("dropIndexes", new String[] { "java.lang.String" }, "void", OpType.INDEX),
         new MethodMatcher<OpType>("ensureIndex", new String[] { "com.mongodb.DBObject", "com.mongodb.DBObject" }, "void", OpType.QUERY),
         //new MethodMatcher<OpType>("find", new String[]{ }, "com.mongodb.DBCursor", OpType.FIND_ALL, true), //have to match exactly 0 params, otherwise find(DBObject) will be instrumented twice
         //new MethodMatcher<OpType>("find", new String[]{  "com.mongodb.DBObject" }, "com.mongodb.DBCursor", OpType.FIND),
         new MethodMatcher<OpType>("findAndModify", new String[] { "com.mongodb.DBObject", "com.mongodb.DBObject", "com.mongodb.DBObject", "boolean", "com.mongodb.DBObject", "boolean", "boolean" }, "com.mongodb.DBObject", OpType.FIND_AND_MODIFY),
         //new MethodMatcher<OpType>("findOne", new String[]{ "com.mongodb.DBObject", "com.mongodb.DBObject", "com.mongodb.DBObject", "com.mongodb.ReadPreference" }, "com.mongodb.DBObject", OpType.QUERY),
         new MethodMatcher<OpType>("findOne", new String[] { "com.mongodb.DBObject", "com.mongodb.DBObject", "com.mongodb.DBObject" }, "com.mongodb.DBObject", OpType.FIND_ONE), //only newer version has the 3rd argument, which is sort
         new MethodMatcher<OpType>("findOne", new String[] { "com.mongodb.DBObject" }, "com.mongodb.DBObject", OpType.QUERY), //older version
         new MethodMatcher<OpType>("getIndexInfo", new String[] {}, "java.util.List", OpType.SIMPLE),
         new MethodMatcher<OpType>("getStats", new String[] {}, "com.mongodb.CommandResult", OpType.SIMPLE),
         //new MethodMatcher<OpType>("group", new String[]{ "com.mongodb.GroupCommand", "com.mongodb.ReadPreference" }, "com.mongodb.DBObject", OpType.GROUP),
         new MethodMatcher<OpType>("group", new String[] { "com.mongodb.GroupCommand" }, "com.mongodb.DBObject", OpType.GROUP),
         //new MethodMatcher<OpType>("insert", new String[]{ "java.util.List", "com.mongodb.WriteConcern", "com.mongodb.DBEncoder" }, "com.mongodb.WriteResult", OpType.INSERT),
         new MethodMatcher<OpType>("insert", new String[] { "java.util.List", "com.mongodb.WriteConcern" }, "com.mongodb.WriteResult", OpType.INSERT_1),
         new MethodMatcher<OpType>("insert", new String[] { "com.mongodb.DBObject[]", "com.mongodb.WriteConcern" }, "com.mongodb.WriteResult", OpType.INSERT_2),
         new MethodMatcher<OpType>("mapReduce", new String[] { "com.mongodb.MapReduceCommand" }, "com.mongodb.MapReduceOutput", OpType.MAP_REDUCE_1),
         new MethodMatcher<OpType>("mapReduce", new String[] { "com.mongodb.DBObject" }, "com.mongodb.MapReduceOutput", OpType.MAP_REDUCE_2),
         new MethodMatcher<OpType>("parallelScan", new String[] { "com.mongodb.ParallelScanOptions" }, "java.util.List", OpType.PARALLEL_SCAN),
         //new MethodMatcher<OpType>("remove", new String[]{ "com.mongodb.DBObject", "com.mongodb.WriteConcern", "com.mongodb.DBEncoder" }, "com.mongodb.WriteResult", OpType.QUERY),
         new MethodMatcher<OpType>("remove", new String[] { "com.mongodb.DBObject", "com.mongodb.WriteConcern" }, "com.mongodb.WriteResult", OpType.QUERY),
         new MethodMatcher<OpType>("rename", new String[] { "java.lang.String", "boolean" }, "com.mongodb.DBCollection", OpType.RENAME),
         //new MethodMatcher<OpType>("resetOptions", new String[]{ }, "void", OpType.SIMPLE),
         new MethodMatcher<OpType>("save", new String[] { "com.mongodb.DBObject", "com.mongodb.WriteConcern" }, "com.mongodb.WriteResult", OpType.QUERY),
         //new MethodMatcher<OpType>("setOptions", new String[]{ "int" }, "void", OpType.SIMPLE),
         //new MethodMatcher<OpType>("update", new String[]{ "com.mongodb.DBObject", "com.mongodb.DBObject", "boolean", "boolean", "com.mongodb.WriteConcern", "com.mongodb.DBEncoder" }, "com.mongodb.WriteResult", OpType.UPDATE)
         new MethodMatcher<OpType>("update", new String[] { "com.mongodb.DBObject", "com.mongodb.DBObject", "boolean", "boolean", "com.mongodb.WriteConcern" }, "com.mongodb.WriteResult", OpType.UPDATE)
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
                modifyEntry(method, opType);
                modifyExit(method, opType);
            }
        }

    }

    private void modifyExit(CtMethod method, OpType opType)
        throws CannotCompileException {
        insertAfter(method, CLASS_NAME + ".layerExit();", true);
    }

    private void modifyEntry(CtMethod method, OpType opType)
        throws CannotCompileException {
        if (opType == OpType.SIMPLE) { //then just report the entry event
            insertBefore(method, CLASS_NAME + ".layerEntry(getDB() != null ? getDB().getName() : null, getName(), \"" + method.getName() + "\", null);");
//        } else if (opType == OpType.FIND) { //then take the first param which is DbObject
//            insertBefore(method, CLASS_NAME + ".layerEntry(getDB() != null ? getDB().getName() : null, getName(), \"" + method.getName() + "\", $1 != null ? java.util.Collections.singletonMap(\"Query\", $1.toString()) : null);");
        } else if (opType == OpType.FIND_ALL) { //then the param is "all"
            insertBefore(method, CLASS_NAME + ".layerEntry(getDB() != null ? getDB().getName() : null, getName(), \"" + method.getName() + "\", java.util.Collections.singletonMap(\"Query\", \"all\"));");
        } else if (opType == OpType.FIND_ONE) { //then take the first param (query) and third param (sort)
            insertBefore(method,
                         "java.util.Map keyValues = new java.util.HashMap();" +
                          "if ($1 != null) { keyValues.put(\"Query\", " + INSTRUMENTER_CLASS_NAME + ".getQuery($1)); }" +
                          "if ($3 != null) { keyValues.put(\"Sort\", $3.toString()); }" +
                          CLASS_NAME + ".layerEntry(getDB() != null ? getDB().getName() : null, getName(), \"" + method.getName() + "\", keyValues);");
        } else if (opType == OpType.QUERY) { //then take the first param which is DbObject
            insertBefore(method, CLASS_NAME + ".layerEntry(getDB() != null ? getDB().getName() : null, getName(), \"" + method.getName() + "\", $1 != null ? java.util.Collections.singletonMap(\"Query\", " + INSTRUMENTER_CLASS_NAME + ".getQuery($1)) : null);");
        } else if (opType == OpType.DISTINCT) { //then take the first param which is String, 2nd param which is query DBObject
            insertBefore(method,
                         "java.util.Map keyValues = new java.util.HashMap();" +
                         "if ($1 != null) { keyValues.put(\"Key\", $1.toString()); }" +
                         "if ($2 != null) { keyValues.put(\"Query\", " + INSTRUMENTER_CLASS_NAME + ".getQuery($2)); }" +
                         CLASS_NAME + ".layerEntry(getDB() != null ? getDB().getName() : null, getName(), \"" + method.getName() + "\", keyValues);");
        } else if (opType == OpType.GROUP) { //then take the 1st param which is GroupCommand
            insertBefore(method, CLASS_NAME + ".layerEntry(getDB() != null ? getDB().getName() : null, getName(), \"" + method.getName() + "\", $1 != null ? java.util.Collections.singletonMap(\"Query\", " + INSTRUMENTER_CLASS_NAME + ".getQuery($1.toDBObject())) : null);");
        } else if (opType == OpType.INSERT_1) { //then take the 1st param which is List
            insertBefore(method, CLASS_NAME + ".layerEntry(getDB() != null ? getDB().getName() : null, getName(), \"" + method.getName() + "\", $1 != null ? java.util.Collections.singletonMap(\"NumDocumentsAffected\", Integer.valueOf($1.size())) : null);");
        } else if (opType == OpType.INSERT_2) { //then take the 1st param which is Array
            insertBefore(method, CLASS_NAME + ".layerEntry(getDB() != null ? getDB().getName() : null, getName(), \"" + method.getName() + "\", $1 != null ? java.util.Collections.singletonMap(\"NumDocumentsAffected\", Integer.valueOf($1.length)) : null);");
        } else if (opType == OpType.MAP_REDUCE_1) { //then take the 1st param which is MapReduceCommand
            insertBefore(method,
                         "java.util.Map keyValues = new java.util.HashMap();" +
                         "if ($1 != null) {" +
                         "    if ($1.getMap() != null) { keyValues.put(\"Map_Function\", $1.getMap()); }" +
                         "    if ($1.getReduce() != null) { keyValues.put(\"Reduce_Function\", $1.getReduce()); }" +
                         "    if ($1.getQuery() != null) { keyValues.put(\"Query\", " + INSTRUMENTER_CLASS_NAME + ".getQuery($1.getQuery())); } " +
                         "}" +
                         "boolean isInline = ($1.getOutputType() == com.mongodb.MapReduceCommand.OutputType.INLINE);" +
                         CLASS_NAME + ".layerEntry(getDB() != null ? getDB().getName() : null, getName(), isInline ? \"inlineMapReduce\" : \"mapReduce\", keyValues);");
        } else if (opType == OpType.MAP_REDUCE_2) { //then take the 1st param which is DBObject
            insertBefore(method,
                         "java.util.Map keyValues = new java.util.HashMap();" +
                         "if ($1 != null) {" +
                         "    if ($1.get(\"map\") != null) { keyValues.put(\"Map_Function\", $1.get(\"map\")); }" +
                         "    if ($1.get(\"reduce\") != null) { keyValues.put(\"Reduce_Function\", $1.get(\"reduce\")); }" +
                         "    if ($1.get(\"query\") != null) { keyValues.put(\"Query\", " + INSTRUMENTER_CLASS_NAME + ".getQuery($1.get(\"query\"))); }" +
                         "}" +
                         "boolean isInline = ($1.get(\"out\") instanceof com.mongodb.DBObject) && (new Integer(1).equals(((com.mongodb.DBObject)$1.get(\"out\")).get(\"inline\")));" +
                         CLASS_NAME + ".layerEntry(getDB() != null ? getDB().getName() : null, getName(), isInline ? \"inlineMapReduce\" : \"mapReduce\", keyValues);");
        } else if (opType == OpType.RENAME) { //then take the first param which is String
            insertBefore(method, CLASS_NAME + ".layerEntry(getDB() != null ? getDB().getName() : null, getName(), \"" + method.getName() + "\", $1 != null ? java.util.Collections.singletonMap(\"New_Collection_Name\", $1) : null);");
        } else if (opType == OpType.UPDATE) { //then take the 1st and 2nd argument, which both are DbObject
            insertBefore(method,
                         "java.util.Map keyValues = new java.util.HashMap();" +
                         "if ($1 != null) { keyValues.put(\"Query\", " + INSTRUMENTER_CLASS_NAME + ".getQuery($1)); }" +
                         "if ($2 != null) { keyValues.put(\"Update_Document\", " + INSTRUMENTER_CLASS_NAME + ".getQuery($2)); }" +
                         CLASS_NAME + ".layerEntry(getDB() != null ? getDB().getName() : null, getName(), \"" + method.getName() + "\", keyValues);");
        } else if (opType == OpType.FIND_AND_MODIFY) { //then take the 1st and 5th argument, which both are DbObject
            insertBefore(method,
                         "java.util.Map keyValues = new java.util.HashMap();" +
                         "if ($1 != null) { keyValues.put(\"Query\", " + INSTRUMENTER_CLASS_NAME + ".getQuery($1)); }" +
                         "if ($5 != null) { keyValues.put(\"Update_Document\", " + INSTRUMENTER_CLASS_NAME + ".getQuery($5)); }" +
                         CLASS_NAME + ".layerEntry(getDB() != null ? getDB().getName() : null, getName(), \"" + method.getName() + "\", keyValues);");
        } else if (opType == OpType.AGGREGATE_OLD) { //then take the 1st and 2nd argument
            insertBefore(method,
                         "int opsCount = ($1 != null ? 1 : 0);" +
                         "opsCount += $2 != null ? $2.length : 0;" +
                         "String[] opsArray = new String[opsCount];" +
                         "if ($1 != null) { opsArray[0] = $1.toString(); }" +
                         "if ($2 != null) { " +
                         "    for (int i = 0 ; i < $2.length ; i ++) {" +
                         "        opsArray[1 + i] = ($2[i] != null ? $2[i].toString() : null);" +
                         "    }" +
                         "}" +
                         CLASS_NAME + ".layerEntry(getDB() != null ? getDB().getName() : null, getName(), \"" + method.getName() + "\", java.util.Collections.singletonMap(\"AggregationOps\", opsArray));");
        } else if (opType == OpType.AGGREGATE_NEW) { //then take the 1st argument, which is a List
            insertBefore(method,
                         "String[] opsArray;" +
                         "if ($1 != null) { " +
                         "    opsArray = new String[$1.size()];" +
                         "    for (int i = 0 ; i < $1.size() ; i ++) {" +
                         "        opsArray[i] = $1.get(i) != null ? $1.get(i).toString() : null;" +
                         "    }" +
                         "} else {" +
                         "    opsArray = new String[0];" +
                         "}" +
                         CLASS_NAME + ".layerEntry(getDB() != null ? getDB().getName() : null, getName(), \"" + method.getName() + "\", java.util.Collections.singletonMap(\"AggregationOps\", opsArray));");
        } else if (opType == OpType.INDEX) { //then take the first param which is String
            insertBefore(method, CLASS_NAME + ".layerEntry(getDB() != null ? getDB().getName() : null, getName(), \"" + method.getName() + "\", $1 != null ? java.util.Collections.singletonMap(\"IndexName\", $1) : null);");
        } else if (opType == OpType.PARALLEL_SCAN) { //then take the first param, which is ParallelScanOptions with its NumCursors value
            insertBefore(method, CLASS_NAME + ".layerEntry(getDB() != null ? getDB().getName() : null, getName(), \"" + method.getName() + "\", $1 != null ? java.util.Collections.singletonMap(\"NumCursors\", new Integer($1.getNumCursors())) : null);");
        } else { //unknown operations not handled
            logger.warn("Unknown Mongo DB operation (DBCollection) with mapped enum OpType [" + opType + "]");
            insertBefore(method, CLASS_NAME + ".layerEntry(getDB() != null ? getDB().getName() : null, getName(), \"" + method.getName() + "\", null);");
        }
    }

    public static void layerEntry(String dbName, String collectionName, String op, Map<String, Object> extraKeyValues) {
        if (shouldStartExtent()) { //check whether there are already active MongoDB extent to avoid nested instrumentation
            Event event = Context.createEvent();
            event.addInfo("Layer", LAYER_NAME,
                          "Label", "entry",
                          "Flavor", FLAVOR,
                          "QueryOp", mapMethodNameToQueryOp(op));
            if (dbName != null) {
                event.addInfo("Database", dbName);
            }

            if (collectionName != null) {
                event.addInfo("Collection", collectionName);
            }
            
            if (!"command".equals(op) && (extraKeyValues == null || !extraKeyValues.containsKey("Query"))) {
                event.addInfo("Query", ""); //insert an empty String, this allow correct rendering of query span details on front-end
            } 

            if (extraKeyValues != null) {
                for (Entry<String, Object> entry : extraKeyValues.entrySet()) {
                    if (entry.getValue() != null) {
                        if ("Query".equals(entry.getKey()) && isEmptyQuery((String)entry.getValue())) { //as defined in https://github.com/tracelytics/launchpad/wiki/mongodb-client-spec, should display "all" for empty query "{ }"
                            event.addInfo(entry.getKey(), "all");
                        } else {
                            event.addInfo(entry.getKey(), entry.getValue());
                        }
                    }
                }
            }

            event.report();
        }
    }

    public static void layerExit() {
        if (shouldEndExtent()) { //check whether there are already active MongoDB extent to avoid nested instrumentation
            Event event = Context.createEvent();
            event.addInfo("Layer", LAYER_NAME,
                          "Label", "exit");

            event.report();
        }
    }
}