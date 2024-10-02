package com.appoptics.instrumentation.nosql.mongo2;

import com.google.auto.service.AutoService;
import com.tracelytics.ext.javassist.*;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.Instrument;
import com.tracelytics.instrumentation.MethodMatcher;
import com.tracelytics.instrumentation.Module;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Instrumentation on MongoDB's DBCursor. We do not trace the cursor operations here directly, instead we store the cursor information here as ThreadLocal and then let MongoDbPortInstrumentation reads and creates
 * extent instead. This is done to avoid tracing excessive DBCursor operations that do not trigger any real network round trip to MongoDB server
 * 
 * @author pluk
 *
 */
@AutoService(ClassInstrumentation.class)
@Instrument(targetType = "com.mongodb.DBCursor", module = Module.MONGODB)
public class Mongo2DbCursorInstrumentation extends Mongo2BaseInstrumentation {
    private static final String CLASS_NAME = Mongo2DbCursorInstrumentation.class.getName();

    private static ThreadLocal<CursorOpInfo> currentOpInfo = new ThreadLocal<CursorOpInfo>();

    private enum MethodType {
        OTHER
    }

    @SuppressWarnings("unchecked")
    /**
     * List of cursor methods that might trigger network traffic
     */
    private static List<MethodMatcher<MethodType>> targetMethodMatchers = Arrays.asList(
                                                                                        new MethodMatcher<MethodType>("next", new String[] {}, "com.mongodb.DBObject", MethodType.OTHER),
                                                                                        new MethodMatcher<MethodType>("hasNext", new String[] {}, "boolean", MethodType.OTHER),
                                                                                        new MethodMatcher<MethodType>("length", new String[] {}, "int", MethodType.OTHER),
                                                                                        new MethodMatcher<MethodType>("toArray", new String[] {}, "java.util.List", MethodType.OTHER),
                                                                                        new MethodMatcher<MethodType>("itcount", new String[] {}, "int", MethodType.OTHER)
                                                                                );

    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {

        Map<CtMethod, MethodType> matches = findMatchingMethods(cc, targetMethodMatchers);

        cc.addField(CtField.make(CursorOpInfo.class.getName() + " currentCursorInfo;", cc));
        
        patchSort(cc);
        
        String getLimitExpression = findGetLimitExpression(cc);
        String getBatchSizeExpression = findGetBatchSizeExpression(cc);
        
        
        for (Entry<CtMethod, MethodType> match : matches.entrySet()) {
            CtMethod method = match.getKey();
            if (match.getValue() == MethodType.OTHER) {
                addErrorReporting(method, "com.mongodb.MongoException", LAYER_NAME, classPool);
                insertBefore(method,
                             "if (currentCursorInfo == null) {" + //do this only once to avoid creating new objects repeatedly
                             "    currentCursorInfo = new "  + CursorOpInfo.class.getName() + "(\"" + method.getName() + "\", " +
                             "                                         getQuery()," +
                             "                                         (getCollection() != null && getCollection().getDB() != null) ? getCollection().getDB().getName() : null," +
                             "                                         getCollection() != null ? getCollection().getName() : null," +
                             "                                         tvGetSort() != null ? tvGetSort().toString() : null," + 
                                                                       (getLimitExpression != null ? getLimitExpression + "," : "null,") +
                             "                                         System.identityHashCode(this));" +
                             "}" +
                             "currentCursorInfo.setBatchSize(" + (getBatchSizeExpression != null ? getBatchSizeExpression : "null") +  ");" + //cannot lazy initialize the  batchSize in the currentCursorInfo as this can be modified after the cursor is iterated	    				
                             CLASS_NAME + ".startCursorOp(currentCursorInfo);");
                insertAfter(method, CLASS_NAME + ".endCursorOp();", true);
            }
        }

        return true;
    }

    /**
     * Identifies the expression used to get the limit info from Cursor. 
     * 
     * Ideally it should call getLimit() Method. However in earlier mongodb version, such a method is not present and we might have to use the field _limit directly
     * @param cc
     * @return
     */
    private String findGetLimitExpression(CtClass cc) {
        try {
            cc.getDeclaredMethod("getLimit", new CtClass[0]);
            return "getLimit()";
        } catch (NotFoundException e) {
            try {
                cc.getDeclaredField("_limit"); //then try getting the field directly
                return "_limit";
            } catch (NotFoundException e1) {
                logger.warn("Cannot find getLimit() method nor _limit field from MongoDb cursor");
                return null;
            }
        }
    }
    
    /**
     * Identifies the expression used to get the batch size from Cursor. 
     * 
     * Ideally it should call getBatchSize() Method. However in earlier mongodb version, such a method is not present and we might have to use the field _batchSize directly
     * @param cc
     * @return
     */
    private String findGetBatchSizeExpression(CtClass cc) {
        try {
            cc.getDeclaredMethod("getBatchSize", new CtClass[0]);
            return "getBatchSize()";
        } catch (NotFoundException e) {
            try {
                cc.getDeclaredField("_batchSize"); //then try getting the field directly
                return "_batchSize";
            } catch (NotFoundException e1) {
                logger.warn("Cannot find getBatchSize() method nor _batchSize field from MongoDb cursor");
                return null;
            }
        }
    }

    /**
     * Patch the DBCursor to provide `tvGetSort` which returns the sorting criteria 
     *   
     * @param cc
     * @throws CannotCompileException
     * @throws NotFoundException
     */
    private void patchSort(CtClass cc) throws CannotCompileException, NotFoundException {
        try {
            cc.getDeclaredField("_orderBy");
            cc.addMethod(CtNewMethod.make("public Object tvGetSort() { return _orderBy; }", cc));
        } catch (NotFoundException e) {
            try {
                cc.getDeclaredField("sort");
                cc.addMethod(CtNewMethod.make("public Object tvGetSort() { return sort; }", cc));
            } catch (NotFoundException e1) {
                try {
                    CtMethod sortMethod = cc.getDeclaredMethod("sort");
                    cc.addField(CtField.make("private Object tvSort;", cc));
                    insertBefore(sortMethod, "tvSort = $1;"); //patch the sort method to keep track of the value used
                    cc.addMethod(CtNewMethod.make("public Object tvGetSort() { return tvSort; }", cc));
                    
                } catch (NotFoundException e2) {
                    logger.warn("Cannot find any option to get sort/order value from MongoDB cursor, instrumentation will not report field sorting on cursor");
                    cc.addMethod(CtNewMethod.make("public Object tvGetSort() { return null; }", cc));
                }
            }
        }
        tagInterface(cc, Mongo2DbCursor.class.getName());
    }
    
    public static void startCursorOp(CursorOpInfo cursorOpInfo) {
        currentOpInfo.set(cursorOpInfo);
    }

    public static void endCursorOp() {
        currentOpInfo.remove();
    }

    public static CursorOpInfo getCurrentCursorOp() {
        return currentOpInfo.get();
    }

    /**
     * Contains various information of the DBCursor
     * @author pluk
     *
     */
    public static class CursorOpInfo {
        private String op;
        private Object query;
        private String databaseName;
        private String collectionName;
        private String sort;
        private int limit;
        private int batchSize;
        private int id;

        public CursorOpInfo(String op, Object query, String databaseName, String collectionName, String sort, int limit, int id) {
            super();
            this.id = id;
            this.op = op;
            this.query = query;
            this.databaseName = databaseName;
            this.collectionName = collectionName;
            this.sort = sort;
            this.limit = limit;
        }

        public void setBatchSize(int batchSize) {
            this.batchSize = batchSize;
        }

        public int getId() {
            return id;
        }

        public String getOp() {
            return op;
        }

        public Object getQuery() {
            return query;
        }

        public String getDatabaseName() {
            return databaseName;
        }

        public String getCollectionName() {
            return collectionName;
        }

        public String getSort() {
            return sort;
        }

        public int getLimit() {
            return limit;
        }

        public int getBatchSize() {
            return batchSize;
        }

    }
}