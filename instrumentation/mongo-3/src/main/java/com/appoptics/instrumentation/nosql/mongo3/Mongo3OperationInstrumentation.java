package com.appoptics.instrumentation.nosql.mongo3;

import com.google.auto.service.AutoService;
import com.tracelytics.ext.javassist.*;
import com.tracelytics.ext.javassist.bytecode.DuplicateMemberException;
import com.tracelytics.instrumentation.Module;
import com.tracelytics.instrumentation.*;
import com.tracelytics.joboe.span.impl.Scope;
import com.tracelytics.joboe.span.impl.ScopeManager;
import com.tracelytics.joboe.span.impl.Span;
import com.tracelytics.joboe.span.impl.Tracer;

import java.util.*;
import java.util.Map.Entry;


/**
 * Instruments Mongo 3.x operations. Take note that all of the methods in the new classes <code>MongoDatabase</code> and <code>MongoCollection</code> introduced in Mongo 3.x eventually call <code>execute</code>
 * or <code>executeAsync</code> method of <code>com.mongodb.operation.WriteOperation</code> or <code>com.mongodb.operation.ReadOperation</code> instrumented by this class
 *
 * For synchronous operation, this handles both the span start and finish.
 *
 * For asynchronous operation, this handles the span start and tag the started span by replacing the callback instance with our wrapper. The wrapper then later on finishes the span when the async operation completes
 *   
 * @author pluk
 *
 */
@AutoService(ClassInstrumentation.class)
@Instrument(targetType = { "com.mongodb.operation.WriteOperation", "com.mongodb.operation.ReadOperation" }, module = Module.MONGODB, appLoaderPackage = { "com.appoptics.apploader.instrumenter.nosql.mongo3", "com.appoptics.apploader.instrumenter.nosql.mongo3.wrapper" })
public class Mongo3OperationInstrumentation extends Mongo3BaseInstrumentation {
    private static final String CLASS_NAME = Mongo3OperationInstrumentation.class.getName();
    private static final String QUERY_INSTRUMENTER_CLASS_NAME = "com.appoptics.apploader.instrumenter.nosql.mongo3.Mongo3QueryInstrumenter";
    private static final String CALLBACK_WRAPPER_CLASS = "com.appoptics.apploader.instrumenter.nosql.mongo3.wrapper.CallbackWrapper";
    private static final Set<String> EXCLUDED_CLASSES = new HashSet<String>();

    private enum MethodType { SYNC, ASYNC }

    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<MethodType>> targetMethodMatchers = Arrays.asList(new MethodMatcher<MethodType>("execute", new String[0], "java.lang.Object", MethodType.SYNC),
                                                                                        new MethodMatcher<MethodType>("executeAsync", new String[] { }, "void", MethodType.ASYNC));
    
    static {
        EXCLUDED_CLASSES.add("com.mongodb.client.internal.MapReduceIterableImpl$WrappedMapReduceReadOperation");
    }
    
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {
        if (EXCLUDED_CLASSES.contains(cc.getName())) {
            return false;
        }
        //Maps to the operation type based on the Operation class name
        OpType opType = OpType.byClassName(cc.getName());
        
        if (opType == OpType.UNKNOWN) {
            logger.debug("Cannot identify Mongodb 3.x operation class [" + className + "]"); //it's probably okay if it's a proxy class
        }
        
        tagClass(cc, opType);
        
        for (Entry<CtMethod, MethodType> methodEntry : findMatchingMethods(cc, targetMethodMatchers).entrySet()) {
            addErrorReporting(methodEntry.getKey(), "com.mongodb.MongoException", LAYER_NAME, classPool);
            modifyEntry(cc, methodEntry.getKey(), opType, methodEntry.getValue() == MethodType.SYNC);
            modifyExit(methodEntry.getKey(), opType, methodEntry.getValue() == MethodType.SYNC);
        }

        return true;
    }
    
    private void modifyEntry(CtClass ctClass, CtMethod method, OpType opType, boolean isSync) throws CannotCompileException, NotFoundException {
        String queryOp = opType.getQueryOp();

        if (opType == OpType.AGGREGATE || opType == OpType.AGGREGATE_EXPLAIN || opType == OpType.AGGREGATE_TO_COLLECTION) {
            String getPipelineStatement;
            
            try {
                ctClass.getField("pipeline");
                getPipelineStatement = "pipeline";
            } catch (NotFoundException e) {
                try {
                    ctClass.getField("wrapped");
                    getPipelineStatement = "wrapped.getPipeline()";
                } catch (NotFoundException e1) {
                    logger.warn("Cannot find field pipeline nor wrap for " + ctClass.getName() + " , probably on a version that we do not support. The instrumentation will not report the AggregationOps KV.");
                    getPipelineStatement = null;
                }
            }
            
            if (getPipelineStatement != null) {
                insertBefore(method,
                        "String[] opsArray;" +
                        "java.util.List pipelineList = "  + getPipelineStatement + ";" +
                        "if (pipelineList != null) { " +
                        "    opsArray = new String[pipelineList.size()];" +
                        "    for (int i = 0 ; i < pipelineList.size() ; i ++) {" +
                        "        Object pipelineObject = pipelineList.get(i);" +
                        "        opsArray[i] = pipelineObject != null ? pipelineObject.toString() : null;" +
                        "    }" +
                        "} else {" +
                        "    opsArray = new String[0];" +
                        "}" +
                        CLASS_NAME + ".layerEntry(this, \"" + queryOp + "\", java.util.Collections.singletonMap(\"AggregationOps\", opsArray), " + getAsyncParameters(isSync) + ");");
            } else {
                insertBefore(method, CLASS_NAME + ".layerEntry(this, \"" + queryOp + "\", null, " + getAsyncParameters(isSync) + ");");
            }
            
            
        } else if (opType == OpType.BULK_WRITE) {
            String insertRequestClass = getQualifiedClassName("bulk.InsertRequest");
            String updateRequestClass = getQualifiedClassName("bulk.UpdateRequest");
            String deleteRequestClass = getQualifiedClassName("bulk.DeleteRequest");
            String getUpdateMethodName = getUpdateMethodName();

            insertBefore(method,
                         "String queryOp = \"" + queryOp + "\";" + //the default
                         "java.util.List writeRequests = getWriteRequests();" +
                         "java.util.Map keyValues = new java.util.HashMap();" +
                         "if (writeRequests != null && writeRequests.size() == 1) {" + //then identify the type
                         "    Object writeRequest = writeRequests.get(0);" +
                         "    if (writeRequest instanceof " + insertRequestClass + ") {" +
                         "        queryOp = \"insert\";" +
                         "    } else if (writeRequest instanceof " + updateRequestClass + ") {" +
                         "        queryOp = ((" + updateRequestClass + ") writeRequest).isMulti() ? \"update_many\" : \"update_one\";" +
                         "        if (((" + updateRequestClass + ")writeRequest)." + getUpdateMethodName + "() != null) {  keyValues.put(\"Update_Document\", " + QUERY_INSTRUMENTER_CLASS_NAME + ".sanitize(((" + updateRequestClass + ")writeRequest)." + getUpdateMethodName + "())); }" +
                         "        if (((" + updateRequestClass + ")writeRequest).getFilter() != null) {  keyValues.put(\"Query\", " + QUERY_INSTRUMENTER_CLASS_NAME + ".sanitize(((" + updateRequestClass + ") writeRequest).getFilter())); }" +
                         "    } else if (writeRequest instanceof " + deleteRequestClass + ") {" +
                         "        queryOp = ((" + deleteRequestClass + ") writeRequest).isMulti() ? \"delete_many\" : \"delete_one\";" +
                         "        if (((" + deleteRequestClass + ")writeRequest).getFilter() != null) {  keyValues.put(\"Query\", " + QUERY_INSTRUMENTER_CLASS_NAME + ".sanitize(((" + deleteRequestClass + ")writeRequest).getFilter())); }" +
                         "    }" +
                         "}" +
                         CLASS_NAME + ".layerEntry(this, queryOp, keyValues, " + getAsyncParameters(isSync) + ");");
        } else if (opType == OpType.COMMAND_READ || opType == OpType.COMMAND_WRITE) {
            insertBefore(method, CLASS_NAME + ".layerEntry(this, \"" + queryOp + "\", command != null ? java.util.Collections.singletonMap(\"Command\", command.toString()) : null, " + getAsyncParameters(isSync) + ");");
        } else if (opType == OpType.COUNT) {
            insertBefore(method, CLASS_NAME + ".layerEntry(this, \"" + queryOp + "\", getFilter() != null ? java.util.Collections.singletonMap(\"Query\", " + QUERY_INSTRUMENTER_CLASS_NAME + ".sanitize(getFilter())) : null, " + getAsyncParameters(isSync) + ");");
        } else if (opType == OpType.CREATE_INDEX) {
            String indexRequestClass = getQualifiedClassName("bulk.IndexRequest");
            insertBefore(method, CLASS_NAME + ".layerEntry(this, \"" + queryOp + "\", getRequests().size() == 1 ? java.util.Collections.singletonMap(\"IndexKeys\", ((" + indexRequestClass + ")getRequests().get(0)).getKeys().toString()) : null, " + getAsyncParameters(isSync) + ");");
        } else if (opType == OpType.CREATE_USER) {
            insertBefore(method, CLASS_NAME + ".layerEntry(this, \"" + queryOp + "\", getCredential() != null && getCredential().getUserName() != null ? java.util.Collections.singletonMap(\"UserName\", getCredential().getUserName()) : null, " + getAsyncParameters(isSync) + ");");
        } else if (opType == OpType.DISTINCT) {
            insertBefore(method, 
                         "java.util.Map keyValues = new java.util.HashMap();" +
                         "if (getFilter() != null) { keyValues.put(\"Query\", " + QUERY_INSTRUMENTER_CLASS_NAME + ".sanitize(getFilter())); }" +
                         "if (fieldName != null) { keyValues.put(\"Key\", fieldName); }" +
                         CLASS_NAME + ".layerEntry(this, \"" + queryOp + "\", keyValues, " + getAsyncParameters(isSync) + ");");
        } else if (opType == OpType.DROP_INDEX) {
            insertBefore(method, CLASS_NAME + ".layerEntry(this, \"" + queryOp + "\", indexName != null ? java.util.Collections.singletonMap(\"IndexName\", indexName) : null, " + getAsyncParameters(isSync) + ");");
        } else if (opType == OpType.DROP_USER) {
            insertBefore(method, CLASS_NAME + ".layerEntry(this, \"" + queryOp + "\", userName != null ? java.util.Collections.singletonMap(\"UserName\", userName) : null, " + getAsyncParameters(isSync) + ");");
        } else if (opType == OpType.FIND) {
            insertBefore(method, 
                         "java.util.Map keyValues = new java.util.HashMap();" +
                         "if (getFilter() != null) { keyValues.put(\"Query\", " + QUERY_INSTRUMENTER_CLASS_NAME + ".sanitize(getFilter())); }" +
                         "keyValues.put(\"Limit\", Integer.valueOf(getLimit()));" +
                         CLASS_NAME + ".layerEntry(this, \"" + queryOp + "\", keyValues, " + getAsyncParameters(isSync) + ");");
        } else if (opType == OpType.FIND_AND_MODIFY) { 
            insertBefore(method, 
                         "java.util.Map keyValues = new java.util.HashMap();" +
                         "String queryOp = null;" +
                         "if (this instanceof " + Mongo3FindAndModify.class.getName() + ") { " +
                         "    Object target = ((" + Mongo3FindAndModify.class.getName() + ") this).tvGetModifyTarget(); " +
                         "    if (target != null) { keyValues.put(\"Update_Document\","  + QUERY_INSTRUMENTER_CLASS_NAME + ".sanitize(target)); }" +
                         "    Object filter = ((" + Mongo3FindAndModify.class.getName() + ") this).tvGetFilter(); " +
                         "    if (filter != null) { keyValues.put(\"Query\", " + QUERY_INSTRUMENTER_CLASS_NAME + ".sanitize(filter)); }" +
                         "    queryOp = ((" + Mongo3FindAndModify.class.getName() + ") this).tvGetQueryOp(); " +
                         "}" +
                         //"if (getReplacement() != null) { keyValues.put(\"Update_Document\", getReplacement().toString()); }" +
                         //"if (getUpdate() != null) { keyValues.put(\"Update_Document\", getUpdate().toString()); }" +
                         CLASS_NAME + ".layerEntry(this, queryOp, keyValues, " + getAsyncParameters(isSync) + ");");
        } else if (opType == OpType.MAP_REDUCE || opType == OpType.MAP_REDUCE_INLINE) {
            insertBefore(method,
                         "java.util.Map keyValues = new java.util.HashMap();" +
                         "if (getFilter() != null) { keyValues.put(\"Query\", " + QUERY_INSTRUMENTER_CLASS_NAME + ".sanitize(getFilter())); }" +
                         "if (getMapFunction() != null) { keyValues.put(\"Map_Function\", getMapFunction().getCode()); }" +
                         "if (getReduceFunction() != null) { keyValues.put(\"Reduce_Function\", getReduceFunction().getCode()); }" +
                         "keyValues.put(\"Limit\", Integer.valueOf(getLimit()));" +
                         CLASS_NAME + ".layerEntry(this, \"" + queryOp + "\", keyValues, " + getAsyncParameters(isSync) + ");");
        } else if (opType == OpType.RENAME_COLLECTION) {
            insertBefore(method,
                         "java.util.Map keyValues = new java.util.HashMap();" +
                         "if (originalNamespace != null) { keyValues.put(\"Database\", originalNamespace.getDatabaseName()); keyValues.put(\"Collection\", originalNamespace.getCollectionName()); }" +
                         "if (newNamespace != null) { keyValues.put(\"New_Collection_Name\", newNamespace.getCollectionName()); }" +
                         CLASS_NAME + ".layerEntry(this, \"" + queryOp + "\", keyValues, " + getAsyncParameters(isSync) + ");");
        } else if (opType == OpType.UPDATE) {
            String updateRequestClass = getQualifiedClassName("bulk.UpdateRequest");
            String getUpdateMethodName = getUpdateMethodName();
            insertBefore(method,
                         "java.util.Map keyValues = new java.util.HashMap();" +
                         "if (getUpdateRequests().size() == 1) { " +
                         "    if (((" + updateRequestClass + ")getUpdateRequests().get(0)).getFilter() != null) {" +
                         "        keyValues.put(\"Query\", " + QUERY_INSTRUMENTER_CLASS_NAME + ".sanitize(getUpdateRequests().get(0).getFilter())); " +
                         "    }" +
                         "    if (((" + updateRequestClass + ")getUpdateRequests().get(0))." + getUpdateMethodName + "() != null) {" +
                         "        keyValues.put(\"Update_Document\", " + QUERY_INSTRUMENTER_CLASS_NAME + ".sanitize(getUpdateRequests().get(0)." + getUpdateMethodName + "())); " +
                         "    }" +
                         "}" +
                         CLASS_NAME + ".layerEntry(this, \"" + queryOp + "\", keyValues, " + getAsyncParameters(isSync) + ");");
        } else if (opType == OpType.UPDATE_USER) {
            insertBefore(method, CLASS_NAME + ".layerEntry(this, \"" + queryOp + "\", getCredential() != null && getCredential().getUserName() != null ? java.util.Collections.singletonMap(\"UserName\", getCredential().getUserName()) : null, " + getAsyncParameters(isSync) + ");");
        } else if (opType == OpType.USER_EXISTS) {
            insertBefore(method, CLASS_NAME + ".layerEntry(this, \"" + queryOp + "\", userName != null ? java.util.Collections.singletonMap(\"UserName\", userName) : null, " + getAsyncParameters(isSync) + ");");
        } else {
            insertBefore(method, CLASS_NAME + ".layerEntry(this, \"" + queryOp + "\", null, " + getAsyncParameters(isSync) + ");");
        }

        int callbackParameterIndex = findCallbackParameterIndex(method);
        if (callbackParameterIndex > -1) { //then it's an async method, replace the callback with our wrapper
            String callbackToken = "$" + (callbackParameterIndex + 1);
            insertBefore(method, callbackToken + " = new " + getCallbackWrapperClassName() + "(" + callbackToken + ");"); //this gets insert at the TOP of the method now
        }
    }

    private String getAsyncParameters(boolean isSync) {
        if (isSync) {
            return "true, null";
        } else {
            return "false, $2";
        }
    }

    protected String getQualifiedClassName(String classNameSegment) {
        return "com.mongodb." + classNameSegment;
    }

    protected String getUpdateMethodName() {
        return "getUpdate";
    }

    private void tagFindAndModifyClass(CtClass cc) throws CannotCompileException, NotFoundException {
        String modifyTargetExpression;
        String queryOp;
        if (cc.getName().equals(getQualifiedClassName("operation.FindAndUpdateOperation"))) {
            modifyTargetExpression =  "getUpdate()";
            queryOp = "find_and_modify";
        } else if (cc.getName().equals(getQualifiedClassName("operation.FindAndReplaceOperation"))) {
            modifyTargetExpression =  "getReplacement()";
            queryOp = "find_and_modify";
        } else if (cc.getName().equals(getQualifiedClassName("operation.FindAndDeleteOperation"))) {
            modifyTargetExpression =  null;
            queryOp = "find_and_delete";
        } else { //not a target class
            return;
        }
        if (modifyTargetExpression != null) {
            cc.addMethod(CtNewMethod.make("public Object tvGetModifyTarget() { return " + modifyTargetExpression + "; }", cc));
        } else {
            cc.addMethod(CtNewMethod.make("public Object tvGetModifyTarget() { return null; }", cc));
        }
        cc.addMethod(CtNewMethod.make("public Object tvGetFilter() { return getFilter(); }", cc));
        cc.addMethod(CtNewMethod.make("public String tvGetQueryOp() { return \"" + queryOp + "\"; }", cc));

        tagInterface(cc, Mongo3FindAndModify.class.getName());

    }

    public static void modifyExit(CtMethod method, OpType opType, boolean isSync) throws CannotCompileException {
        if (isSync) {
            if (opType == OpType.INSERT || opType == OpType.UPDATE || opType == OpType.DELETE) {
                insertAfter(method, CLASS_NAME + ".layerExit($_ != null && $_.wasAcknowledged() ? java.util.Collections.singletonMap(\"NumDocumentsAffected\", Integer.valueOf($_.getCount())) : null, true);");
            } else if (opType == OpType.BULK_WRITE) {
                insertAfter(method,
                            "int documentsAffectedCount = 0;" +
                            "if ($_ != null && $_.wasAcknowledged()) {" +
                            "    documentsAffectedCount = $_.getInsertedCount() + $_.getDeletedCount() + $_.getModifiedCount();" +
                            "}" +
                            CLASS_NAME + ".layerExit(java.util.Collections.singletonMap(\"NumDocumentsAffected\", Integer.valueOf(documentsAffectedCount)), true);", true);
            } else if (opType == OpType.FIND_AND_MODIFY) {
                insertAfter(method, CLASS_NAME + ".layerExit(java.util.Collections.singletonMap(\"NumDocumentsAffected\", $_ != null ? Integer.valueOf(1) : Integer.valueOf(0)), true);", true);
            } else {
                insertAfter(method, CLASS_NAME + ".layerExit(null, true);", true);
            }
        } else {
            insertAfter(method, CLASS_NAME + ".layerExit(null, false);", true);
        }
    }



    /**
     * Adds methods and tag the class with our interface (@link Mongo3Operation} so we can get the database and collection name more conveniently
     * @param cc
     * @throws CannotCompileException
     * @throws NotFoundException
     */
    private void tagClass(CtClass cc, OpType opType) throws CannotCompileException, NotFoundException {
        try {
            if (opType == OpType.FIND_AND_MODIFY) {
                tagFindAndModifyClass(cc);
            } else {
                cc.addMethod(CtNewMethod.make("public String tvGetQueryOp() { return \"" + opType.queryOp + "\"; }", cc));
            }
            cc.getMethod("getNamespace", "()Lcom/mongodb/MongoNamespace;"); //try the getter method first
            cc.addMethod(CtNewMethod.make("public String tvGetCollectionName() { return getNamespace() != null ? getNamespace().getCollectionName() : null; } ", cc));
            cc.addMethod(CtNewMethod.make("public String tvGetDatabaseName() { return getNamespace() != null ? getNamespace().getDatabaseName() : null; } ", cc));
        } catch (NotFoundException e) {
            try {
                cc.getDeclaredField("namespace"); //then try the private field namespace
                cc.addMethod(CtNewMethod.make("public String tvGetCollectionName() { return namespace != null ? namespace.getCollectionName() : null; } ", cc));
                cc.addMethod(CtNewMethod.make("public String tvGetDatabaseName() { return namespace != null ? namespace.getDatabaseName() : null; } ", cc));
            } catch (NotFoundException e1) {
                try {
                    cc.getDeclaredField("databaseName"); //then try the private field databaseName, collection not applicable
                    cc.addMethod(CtNewMethod.make("public String tvGetDatabaseName() { return databaseName; } ", cc));
                } catch (NotFoundException e2) {
                    //ok, some operation does not have any database/collection name, for example ListDatabasesOperation
                    cc.addMethod(CtNewMethod.make("public String tvGetDatabaseName() { return null; } ", cc));
                }

                try {
                    cc.getDeclaredField("collectionName"); //then try the private field collectionName (for example CreateCollectionOperation)
                    cc.addMethod(CtNewMethod.make("public String tvGetCollectionName() { return collectionName; } ", cc));
                } catch (NotFoundException e2) {
                    try {
                        cc.addMethod(CtNewMethod.make("public String tvGetCollectionName() { return null; } ", cc));
                    } catch (DuplicateMemberException e4) {
                        //ok it is the com.mongodb.operation.CreateCollectionOperation class
                    }
                }
            }
        }

        tagInterface(cc, Mongo3Operation.class.getName());

    }


    /**
     * Creates the entry event of the mongo operation. Take note that special handling is applied to the callback object of Asynchronus operation in order to create
     * asynchronous spans
     */
    public static void layerEntry(Object operationObject, String queryOp, Map<String, Object> keyValues, boolean isSync, Object callback) {
        Mongo3Operation operation = (Mongo3Operation) operationObject;

        if (shouldStartExtent()) {
            Tracer.SpanBuilder spanBuilder = buildTraceEventSpan(LAYER_NAME);

            spanBuilder.withTag("Flavor", FLAVOR);
            if (queryOp != null) {
                spanBuilder.withTag("QueryOp", queryOp);
            }

            if (operation.tvGetDatabaseName() != null) {
                spanBuilder.withTag("Database", operation.tvGetDatabaseName());
            }

            if (operation.tvGetCollectionName() != null) {
                spanBuilder.withTag("Collection", operation.tvGetCollectionName());
            }


            boolean hasQuery = false;
            if (keyValues != null) {
                for (Entry<String, Object> entry : keyValues.entrySet()) {
                    if (entry.getValue() != null) {
                        if ("Query".equals(entry.getKey())) {
                            hasQuery = true;
                        }

                        if ("Query".equals(entry.getKey()) && isEmptyQuery(entry.getValue())) { //as defined in https://github.com/tracelytics/launchpad/wiki/mongodb-client-spec, should display "all" for empty query "{ }"
                            spanBuilder.withTag(entry.getKey(), "all");
                        } else {
                            spanBuilder.withTag(entry.getKey(), entry.getValue());
                        }
                    }
                }
            }

            if (!hasQuery) {
                spanBuilder.withTag("Query", ""); //insert an empty String, this allow correct rendering of query span details on front-end
            }


            if (!isSync) { //async operation
                spanBuilder.withSpanProperty(Span.SpanProperty.IS_ASYNC, true);
                Span span = spanBuilder.start();
                if (callback instanceof SpanAware) {
                    ((SpanAware) callback).tvSetSpan(span);
                }
            } else {
                spanBuilder.startActive(true);
            }


        }
    }

    /**
     * Creates the exit event for synchronous operations
     * will appear as a fork in the event graph
     * @param keyValues
     * @param isSync
     */
    public static void layerExit(Map<String, Object> keyValues, boolean isSync) {
        if (shouldEndExtent()) {
            if (isSync) { //only report exit event here if it is a synchronous operation
                Scope scope = ScopeManager.INSTANCE.active();
                if (scope != null) {
                    if (keyValues != null && scope.span() != null) {
                        for (Entry<String, Object> entry : keyValues.entrySet()) {
                            scope.span().setTagAsObject(entry.getKey(), entry.getValue());
                        }
                    }
                    scope.close();
                }
            }
        }
    }

    private enum OpType {
        AGGREGATE_TO_COLLECTION("AggregateToCollectionOperation", "aggregate"),
        DELETE("DeleteOperation", "delete"),
        INSERT("InsertOperation", "insert"),
        UPDATE("UpdateOperation", "update"),
        COMMAND_WRITE("CommandWriteOperation", "command"),
        CREATE_COLLECTION("CreateCollectionOperation", "create_collection"),
        CREATE_INDEX("CreateIndexesOperation", "create_index"),
        CREATE_USER("CreateUserOperation", "create_user"),
        DROP_COLLECTION("DropCollectionOperation", "drop_collection"),
        DROP_DATABASE("DropDatabaseOperation", "drop_database"),
        DROP_INDEX("DropIndexOperation", "drop_index"),
        DROP_USER("DropUserOperation", "drop_user"),
        FIND_AND_MODIFY(new String[] { "FindAndReplaceOperation", "FindAndUpdateOperation", "BaseFindAndModifyOperation", "FindAndDeleteOperation" }, null), //operation name is null, as it should be determined on the actual class type and since 3.8 : com.mongodb.operation.BaseFindAndModifyOperation
        MAP_REDUCE("MapReduceToCollectionOperation", "map_reduce"),
        BULK_WRITE("MixedBulkWriteOperation", "bulk_write"),
        RENAME_COLLECTION("RenameCollectionOperation", "rename_collection"),
        UPDATE_USER("UpdateUserOperation", "update_user"),
        AGGREGATE_EXPLAIN("AggregateExplainOperation", "aggregate_explain"),
        AGGREGATE("AggregateOperation", "aggregate"),
        COMMAND_READ("CommandReadOperation", "command"),
        COUNT("CountOperation", "count"),
        DISTINCT("DistinctOperation", "distinct"),
        FIND("FindOperation", "find"),
        GROUP("GroupOperation", "group"),
        LIST_COLLECTIONS("ListCollectionsOperation", "list_collections"),
        LIST_DATABASES("ListDatabasesOperation", "list_databases"),
        LIST_INDEXES("ListIndexesOperation", "list_indexes"),
        MAP_REDUCE_INLINE("MapReduceWithInlineResultsOperation", "inline_map_reduce"),
        PARALLEL_SCAN("ParallelCollectionScanOperation", "parallel_scan"),
        USER_EXISTS("UserExistsOperation", "user_exists"),
        UNKNOWN(new String[0], "unknown");

        private String queryOp;
        private String[] simpleClassNames;
        private static Map<String, OpType> classNameLookup;
        private static String[] packageNames = new String[] { "com.mongodb.operation", "com.mongodb.internal.operation" };

        private OpType() {}

        private OpType(String simpleClassName, String queryOp) {
            this.simpleClassNames = new String[] { simpleClassName };
            this.queryOp = queryOp;
        }

        private OpType(String[] simpleClassNames, String queryOp) {
            this.simpleClassNames = simpleClassNames;
            this.queryOp = queryOp;
        }

        private static OpType byClassName(String className) {
            synchronized (OpType.class) {
                if (classNameLookup == null) {
                    classNameLookup = new HashMap<String, Mongo3OperationInstrumentation.OpType>();
                    for (OpType opType : OpType.values()) {
                        for (String opSimpleClassName : opType.simpleClassNames) {
                            for (String packageName : packageNames) {
                                String opClassName = packageName + "." + opSimpleClassName;
                                classNameLookup.put(opClassName, opType);
                            }
                        }
                    }
                }
            }
            
            OpType opType = classNameLookup.get(className);
            return opType != null ? opType : UNKNOWN;
        }
        
        //Query op KV value corresponds to the operation class/type
        private String getQueryOp() {
            return queryOp;
        }
    }

    protected String getCallbackWrapperClassName() {
        return CALLBACK_WRAPPER_CLASS;
    }
}
