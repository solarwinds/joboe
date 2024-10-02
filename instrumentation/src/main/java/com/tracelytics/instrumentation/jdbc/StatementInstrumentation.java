package com.tracelytics.instrumentation.jdbc;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.tracelytics.ext.javassist.CannotCompileException;
import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtField;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.ext.javassist.CtNewMethod;
import com.tracelytics.ext.javassist.Modifier;
import com.tracelytics.ext.javassist.NotFoundException;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.MethodSignature;
import com.tracelytics.instrumentation.Module;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.Event;
import com.tracelytics.joboe.config.ConfigManager;
import com.tracelytics.joboe.config.ConfigProperty;

/**
 *  Applies instrumentation to all classes that implement java.sql.Statement (includes PreparedStatement, CallableStatement, etc.)
 *  Modifies the class to contain executed SQL and DB name so it can be reported.
 */
public class StatementInstrumentation extends BaseJDBCInstrumentation {
    public static final int DEFAULT_SQL_MAX_LENGTH = 128 * 1024; //control the max length of the SQL string to avoid BufferOverFlowException
    private static final int sqlMaxLength = ConfigManager.getConfigOptional(ConfigProperty.AGENT_SQL_QUERY_MAX_LENGTH, DEFAULT_SQL_MAX_LENGTH);
    public static final String CLASS_NAME = "com.tracelytics.instrumentation.jdbc.StatementInstrumentation";
    public static final String TLYS_SQL_FIELD = "tlysSQL";
    public static final String TLYS_DB_FIELD = "tlysDB";
    public static final String TLYS_HOST_FIELD = "tlysHost";
    public static final String TLYS_BATCH_SIZE_FIELD = "tlysBatchSize";
    
    private static final int DEFAULT_SANITIZE_MODE = SQLSanitizer.ENABLED_AUTO; 
    
    static final int SQL_MAX_PARAMETER_COUNT = 1024; //control the max count of SQL parameters to avoid BufferOverFlowException
    //ThreadLocal to make tracking feasible even if direct access to the statement instance is not available
    static ThreadLocal<Statement> statements = new ThreadLocal<Statement>();
    //ThreadLocal to detect recursive instrumentation https://github.com/librato/joboe/issues/615
    private static ThreadLocal<Boolean> executingInjectedCode = new ThreadLocal<Boolean>();
    
    //keep track of the method call depth to avoid nested instrumentation on the same operation
    private static ThreadLocal<Map<String, Integer>> depthThreadLocal = new ThreadLocal<Map<String,Integer>> () {
        protected java.util.Map<String,Integer> initialValue() {
            return new HashMap<String, Integer>();
        };
    };
    
    // Signatures for "execute" methods that directly take SQL
    static MethodSignature[] execMethods = {
        new MethodSignature("execute", "(Ljava/lang/String;[I)Z"),
        new MethodSignature("execute", "(Ljava/lang/String;I)Z"),
        new MethodSignature("execute", "(Ljava/lang/String;[Ljava/lang/String;)Z"),
        new MethodSignature("execute", "(Ljava/lang/String;)Z")
    };

    // Signatures for "executeUpdate" methods that directly take SQL
    static MethodSignature[] execUpdMethods = {
        new MethodSignature("executeUpdate", "(Ljava/lang/String;)I"),
        new MethodSignature("executeUpdate", "(Ljava/lang/String;[I)I"),
        new MethodSignature("executeUpdate", "(Ljava/lang/String;I)I"),
        new MethodSignature("executeUpdate", "(Ljava/lang/String;[Ljava/lang/String;)I"),
        new MethodSignature("executeUpdate", "(Ljava/lang/String;Z)I")
    };

    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {

        if (shouldInstrument(cc)) {
            try {
                addField(cc);
            } catch (Exception ex) {
                logger.error(ex.getMessage(), ex);
            }

            applyExecute(cc);
            applyExecuteUpdate(cc);
            applyExecuteQuery(cc);

            applyPreparedExecute(cc);
            applyPreparedExecuteUpdate(cc);
            applyPreparedExecuteQuery(cc);

            applyAddBatch(cc);
            applyClearBatch(cc);
            applyExecuteBatch(cc);

            // Add our interface so we can access the modified class during layer entry/exit:
            CtClass iface = classPool.getCtClass("com.tracelytics.instrumentation.jdbc.Statement");

            for (CtClass i : cc.getInterfaces()) {
                if (i.equals(iface)) {
                    return true;
                }
            }

            cc.addInterface(iface);
            return true;
        } else {
            return false;
        }

        
    }

    protected void addField(CtClass cc)
        throws CannotCompileException, NotFoundException {

        // These fields may have already been added (in a base class)::
        try {
            if (cc.getField(TLYS_SQL_FIELD) != null) {
                return;
            }
        } catch(NotFoundException ex) {
            // Continue
        }
        // The methods may have been added for some Wrapper implementation (WebLogic WrapperFactory etc)
        try {
            if (cc.getDeclaredMethod("tlysSetSQL") != null) {
                return;
            }
        } catch(NotFoundException ex) {
            // Continue
        }

        // Add a field so we can attached SQL to any statements we create
        CtClass strClass = classPool.get("java.lang.String");
        CtField f = new CtField(strClass, TLYS_SQL_FIELD, cc);
        f.setModifiers(Modifier.PUBLIC);
        cc.addField(f);

        CtMethod m;

        // Work around for new versions of Oracle JDBC: the use of "wrapper" classes makes it difficult for us
        // to access the actual SQL and maintain compatibility with older driver versions. However, they provide an
        // internal method for accessing the SQL so we just delegate to that.
        if (cc.getName().contains("oracle.jdbc") && hasMethod(cc, "getOriginalSql", "()Ljava/lang/String;")) {
            m = CtNewMethod.make( "public void tlysSetSQL(String sql) { }", cc);  // no-op
            cc.addMethod(m);

            m = CtNewMethod.make( "public String tlysGetSQL() { return this.getOriginalSql(); }", cc);
            cc.addMethod(m);
        } else {
            // Common case
            m = CtNewMethod.make( "public void tlysSetSQL(String sql) { " + TLYS_SQL_FIELD + " = sql; }", cc);
            cc.addMethod(m);

            m = CtNewMethod.make( "public String tlysGetSQL() { return " + TLYS_SQL_FIELD + "; }", cc);
            cc.addMethod(m);
        }
        
        // And another to store the database name (only used for special case when Connection.getCatalog() is not supported
        f = new CtField(strClass, TLYS_DB_FIELD, cc);
        f.setModifiers(Modifier.PUBLIC);
        cc.addField(f);

        m = CtNewMethod.make( "public void tlysSetDB(String db) { " + TLYS_DB_FIELD + " = db; }", cc);
        cc.addMethod(m);

        m = CtNewMethod.make( "public String tlysGetDB() { " +
                "                 if (" + TLYS_DB_FIELD + " != null) { " +  //we only set the db for Oracle instrumentation currently, since it requires special handling
                "                     return " + TLYS_DB_FIELD  + ";" +
                "                 } else if (getConnection() != null) {" +
                "                     return getConnection().getCatalog();" +
                "                 } else {" +
                "                     return null;" +
                "                 }" +
                "              }", cc);
        cc.addMethod(m);

        // To track DB host:
        f = new CtField(strClass, TLYS_HOST_FIELD, cc);
        f.setModifiers(Modifier.PUBLIC);
        cc.addField(f);

        m = CtNewMethod.make( "public void tlysSetHost(String host) { " + TLYS_HOST_FIELD + " = host; }", cc);
        cc.addMethod(m);
        
        m = CtNewMethod.make( "public boolean tlysIsHostSet() { return " + TLYS_HOST_FIELD + " != null; }", cc);
        cc.addMethod(m);

        m = CtNewMethod.make( "public String tlysGetHost() { " +
                "                 if (" + TLYS_HOST_FIELD + " != null) { " +  //we only set the host for Oracle instrumentation currently, since it requires special handling
                "                     return " + TLYS_HOST_FIELD  + ";" +
                "                 } else if (getConnection() != null && getConnection().getMetaData() != null) {" +
                "                     return " + CLASS_NAME + ".getHostFromURL(getConnection().getMetaData().getURL());" +
                "                 } else {" +
                "                     return null;" +
                "                 }" +
                "              }", cc);
        cc.addMethod(m);

        // To track batch size:
        f = new CtField(CtClass.intType, TLYS_BATCH_SIZE_FIELD, cc);
        f.setModifiers(Modifier.PUBLIC);
        cc.addField(f, CtField.Initializer.constant(0));

        m = CtNewMethod.make( "public int tlysIncBatchSize() { return ++" + TLYS_BATCH_SIZE_FIELD + "; }", cc);
        cc.addMethod(m);

        m = CtNewMethod.make( "public void tlysClearBatchSize() { " + TLYS_BATCH_SIZE_FIELD + " = 0; }", cc);
        cc.addMethod(m);

        m = CtNewMethod.make( "public int tlysGetBatchSize() { return " + TLYS_BATCH_SIZE_FIELD + "; }", cc);
        cc.addMethod(m);

    }

    private void applyExecute(CtClass cc)
        throws CannotCompileException, NotFoundException {
        
        for (MethodSignature s: execMethods) {
            CtMethod m = null;
            
            try {
                m = cc.getMethod(s.getName(), s.getSignature());
                if (!shouldModify(cc, m)) {
                    continue;
                }

            } catch(NotFoundException ex) {
                continue;
            }

            addSQLErrorReporting(m);
            insertBefore(m, CLASS_NAME + ".layerExecuteEntry(\"" + layerName + "\", this, \"" + s.getName() + "\");");
            insertAfter(m, CLASS_NAME + ".layerExecuteExit(\"" + layerName + "\",\"" + flavor + "\", this, \"" + s.getName() + "\", $1, $_);", true);
        }
    }
    
    private void applyExecuteUpdate(CtClass cc)
        throws CannotCompileException, NotFoundException {
        
        for (MethodSignature s: execUpdMethods) {
            CtMethod m = null;
            
            try {
                m = cc.getMethod(s.getName(), s.getSignature());
                if (!shouldModify(cc, m)) {
                    continue;
                }
            } catch(NotFoundException ex) {
                continue;
            }

            addSQLErrorReporting(m);
            insertBefore(m, CLASS_NAME + ".layerExecuteUpdateEntry(\"" + layerName + "\", this, \"" + s.getName() + "\");");
            insertAfter(m, CLASS_NAME + ".layerExecuteUpdateExit(\"" + layerName + "\",\"" + flavor + "\", this, \"" + s.getName() + "\", $1, $_);", true);
        }
    }
   
    
    private void applyExecuteQuery(CtClass cc)
            throws CannotCompileException, NotFoundException {
        CtMethod m;
        try {
            m = cc.getMethod("executeQuery", "(Ljava/lang/String;)Ljava/sql/ResultSet;");
            if (!shouldModify(cc, m)) {
                return;
            }
        } catch(NotFoundException ex) {
            return;
        }
        
        addSQLErrorReporting(m);
        insertBefore(m, CLASS_NAME + ".layerExecuteQueryEntry(\"" + layerName + "\", this, \"executeQuery\");");
        insertAfter(m, CLASS_NAME + ".layerExecuteQueryExit(\"" + layerName + "\",\"" + flavor + "\", this, \"executeQuery\", $1, (Object)$_);", true);
    }


    /* Handles prepared and callable statements: */

    private void applyPreparedExecute(CtClass cc) 
        throws CannotCompileException, NotFoundException {
        CtMethod m;

        try {
            m = cc.getMethod("execute", "()Z" );
            if (!shouldModify(cc, m)) {
                return;
            }
        } catch(NotFoundException ex) {
            return;
        }

        addSQLErrorReporting(m);
        insertBefore(m, CLASS_NAME + ".layerExecuteEntry(\"" + layerName + "\", this, \"execute\");");
        insertAfter(m, CLASS_NAME + ".layerExecuteExit(\"" + layerName + "\",\"" + flavor + "\", this, \"execute\", null, $_);", true);
    }

    private void applyPreparedExecuteUpdate(CtClass cc)
        throws CannotCompileException, NotFoundException {
        CtMethod m;

        try {
            m = cc.getMethod("executeUpdate", "()I" );
            if (!shouldModify(cc, m)) {
                return;
            }
        } catch(NotFoundException ex) {
            return;
        }

        addSQLErrorReporting(m);
        insertBefore(m, CLASS_NAME + ".layerExecuteUpdateEntry(\"" + layerName + "\", this, \"executeUpdate\");");
        insertAfter(m, CLASS_NAME + ".layerExecuteUpdateExit(\"" + layerName + "\",\"" + flavor + "\", this, \"executeUpdate\", null, $_);", true);
    }


    private void applyPreparedExecuteQuery(CtClass cc)
            throws CannotCompileException, NotFoundException {
        CtMethod m;
        try {
            m = cc.getMethod("executeQuery", "()Ljava/sql/ResultSet;");
            if (!shouldModify(cc, m)) {
                return;
            }
        } catch(NotFoundException ex) {
            return;
        }

        addSQLErrorReporting(m);
        insertBefore(m, CLASS_NAME + ".layerExecuteQueryEntry(\"" + layerName + "\", this, \"executeQuery\");");
        insertAfter(m, CLASS_NAME + ".layerExecuteQueryExit(\"" + layerName + "\",\"" + flavor + "\", this, \"executeQuery\", null, (Object)$_);", true);
    }

    
    private void applyAddBatch(CtClass cc)
            throws CannotCompileException, NotFoundException {
        CtMethod m;
        try {
            m = cc.getMethod("addBatch", "(Ljava/lang/String;)V");
            if (!shouldModify(cc, m)) {
                return;
            }
        } catch(NotFoundException ex) {
            return;
        }

        insertBefore(m, CLASS_NAME + ".saveBatchInStatement(this, $1);");
    }

    private void applyClearBatch(CtClass cc)
        throws CannotCompileException, NotFoundException {
        CtMethod m;
        try {
            m = cc.getMethod("clearBatch", "()V");
            if (!shouldModify(cc, m)) {
                return;
            }
        } catch(NotFoundException ex) {
            return;
        }

        insertBefore(m, "this.tlysClearBatchSize();");
    }

    private void applyExecuteBatch(CtClass cc)
            throws CannotCompileException, NotFoundException {
        CtMethod m;
        try {
            m = cc.getMethod("executeBatch", "()[I");
            if (!shouldModify(cc, m)) {
                return;
            }
        } catch(NotFoundException ex) {
            return;
        }

        addSQLErrorReporting(m);
        insertBefore(m, CLASS_NAME + ".layerExecuteEntry(\"" + layerName + "\", this, \"executeBatch\");");
        insertAfter(m, CLASS_NAME + ".layerExecuteExit(\"" + layerName + "\",\"" + flavor + "\", this, \"executeBatch\", null, false);", true);
    }

    /**
     * Callbacks from instrumented classes
     */
    public static void layerExecuteEntry(String layer, Object caller, String method) {
        doEntry(layer, caller, method);
    }
    
    public static void layerExecuteExit(String layer, String flavor, Object caller, String method, String sql, boolean exetRet) {
        doExit(layer, flavor, caller, method, sql);
    }
    
    public static void layerExecuteUpdateEntry(String layer, Object caller, String method) {
        doEntry(layer, caller, method);
    }
    
    public static void layerExecuteUpdateExit(String layer, String flavor, Object caller, String method, String sql, int execRet) {
        doExit(layer, flavor, caller, method, sql);
    }
    
    public static void layerExecuteQueryEntry(String layer, Object caller, String method) {
        doEntry(layer, caller, method);
    }
    
    public static void layerExecuteQueryExit(String layer, String flavor, Object caller, String method, String sql, Object resultSet) {
        doExit(layer, flavor, caller, method, sql);
    }

    private static void doEntry(String layer, Object caller, String method) {
        boolean isRecursiveCallFromInstrumentation = Boolean.TRUE.equals(executingInjectedCode.get());
        if (isRecursiveCallFromInstrumentation) {
            logger.debug("Do not trigger entry instrumentation as this is a recursive call triggered by our own injected code");
            return;
        }

        executingInjectedCode.set(true);
        try {
            if (shouldStartExtent(layer)) {
                logger.debug("JDBC Entry: " + layer + " " + method);
                
                Statement stmt = (Statement)caller;
                Event event = Context.createEvent();
                event.addInfo("Layer", layer,
                              "Label", "entry",
                              "Spec", "query",
                              "JDBC-Class", caller.getClass().getName(),
                              "JDBC-Method", method);
        
                if (stmt.tlysGetBatchSize() > 0 ) {
                    // Need to report batch size in entry event since JDBC drivers may clear it by the time exit is called.
                    event.addInfo("BatchSize" , stmt.tlysGetBatchSize());
                }
                ClassInstrumentation.addBackTrace(event, 2, Module.JDBC);
        
                event.report();
                
                statements.set(stmt); //set the statement to make tracking feasible even if direct access to statement is not available
            }
        } finally {
            executingInjectedCode.remove();            
        }
    }
    
    private static void doExit(String layer, String flavor, Object caller, String method, String sql) {
        boolean isRecursiveCallFromInstrumentation = Boolean.TRUE.equals(executingInjectedCode.get());
        if (isRecursiveCallFromInstrumentation) {
            logger.debug("Do not trigger exit instrumentation as this is a recursive call triggered by our own injected code");
            return;
        }
        
        executingInjectedCode.set(true);
        try {
            if (shouldEndExtent(layer)) {
                logger.debug("JDBC Exit: " + layer + " " + method + " SQL: " + sql);
                
                Statement stmt = (Statement)caller;
                String db = null;
                String host = null;
                try {
                    if (sql == null) {
                        sql = stmt.tlysGetSQL();
                    }
                    db = stmt.tlysGetDB();
                    host = stmt.tlysGetHost();
                } catch (Exception e) {
                    logger.debug("Caught exception while extracting JDBC statement information, statement is probably closed. Message : " + e.getMessage());
                }
                
                Event event = Context.createEvent();
                event.addInfo("Layer", layer,
                              "Label", "exit",
                              "Spec", "query",
                              "Flavor", flavor);
        
                if (sql != null) {
                    int sanitizeFlag;
                    
                    Integer sanitizeFlagObject = (Integer) ConfigManager.getConfig(ConfigProperty.AGENT_SQL_SANITIZE);
                    if (sanitizeFlagObject != null) {
                        sanitizeFlag = sanitizeFlagObject;
                    } else {
                        sanitizeFlag = DEFAULT_SANITIZE_MODE;
                    }
                    
                    SQLSanitizer sanitizer = SQLSanitizer.getSanitizer(sanitizeFlag, caller.getClass().getName());
                    if (sanitizer != null) { //then sanitizing is enabled
                        sql = sanitizer.sanitizeQuery(sql); 
                    }
                    
                    if (sql.length() > sqlMaxLength) {
                        sql = sql.substring(0, sqlMaxLength);
                        event.addInfo("QueryTruncated", true);
                        logger.debug("SQL Query trimmed as its length [" + sql.length() + "] exceeds max [" + sqlMaxLength + "]");
                    }
                    
                    event.addInfo("Query", sql);
                }
        
                if (db != null) {
                    event.addInfo("Database" , db);
                }
        
                if (host != null) {
                    event.addInfo("RemoteHost", host);
                }
                
                if (stmt instanceof PreparedStatement) {
                    PreparedStatement preparedStatement  =  (PreparedStatement)stmt;
                    if (!preparedStatement.tlysGetParameters().isEmpty()) {
                        Object[] parameterArray;
                        
                        Map<Integer, Object> parameters = preparedStatement.tlysGetParameters(); 
                        if (parameters.size() > SQL_MAX_PARAMETER_COUNT) { 
                            logger.debug("SQL Parameter collection with size [" + parameters.size() + "] is too large, Trimming it to size " + SQL_MAX_PARAMETER_COUNT);
                            parameterArray = new Object[SQL_MAX_PARAMETER_COUNT];
                            Iterator<Object> parameterIterator = parameters.values().iterator();
                            for (int i = 0 ; i < SQL_MAX_PARAMETER_COUNT ; i ++) {
                                parameterArray[i] = parameterIterator.next();
                            }
                        } else {
                            parameterArray = parameters.values().toArray();
                        }
                        
                        event.addInfo("QueryArgs", parameterArray);
                    }
                }
        
                event.report();
                
                statements.remove(); //remove the statement, tracking is no longer needed as it's exiting
            }
        } finally {
            executingInjectedCode.remove();
        }
    }

    public static void saveBatchInStatement(Object stmtObj, String sql) {

        try {
            Statement s = (Statement) stmtObj;
            if (s.tlysIncBatchSize() == 1) {
                s.tlysSetSQL(sql); // we only save the first SQL statement in a batch to avoid overhead.
            }
        } catch(Throwable ex) {
            if (ConfigManager.getConfigOptional(ConfigProperty.AGENT_DEBUG, false)) {
                // Should never happen:
                logger.error(ex.getMessage(), ex);
            }
        }
    }
    
    /* Extract host portion from database URL
     *  URL formats are jdbc:mysql://host:port/database?options  jdbc:postgresql://host:port/database,
     *  jdbc:oracle:thin:@//localhost:1521/orcl  jdbc:oracle:oci8:@hostname_orcl
     *  jdbc:db2//server:port/database jdbc:db2//server/database jdbc:db2:database
     *
     */
   public static String getHostFromURL(String url) {
        String host = null;

        // Handles common mysql and postgres case:
        int startHost = url.indexOf("://");
        int hostOffset = 0;

        if (startHost != -1) {
            hostOffset = 3;

        } else if (url.startsWith("jdbc:oracle:")) {
            // For jdbc:oracle:thin:@//localhost:1521/orcl
            startHost = url.indexOf(":@//");
            if (startHost != -1) {
                hostOffset = 4;
            } else {
                // For jdbc:oracle:oci8:@hostname_orcl
                startHost = url.indexOf(":@");
                if (startHost != -1) {
                    hostOffset = 2;
                }
            }

        } else if (url.startsWith("jdbc:db2")) {
            // For jdbc:db2//server/database
            startHost = url.indexOf("//");
            if (startHost != -1) {
                hostOffset = 2;
            } else {
                // For jdbc:db2:database
                startHost = url.indexOf("jdbc:db2:");
                if (startHost != -1) {
                    hostOffset = 9;
                }
            }
        }

        if (startHost != -1) {
            startHost += hostOffset;
            int endHost = url.indexOf("/", startHost);
            if (endHost == -1) {
                host = url.substring(startHost);
            } else {
                host = url.substring(startHost, endHost);
            }

            // The "host" may have a port in it, so strip it off
            int startPort = host.lastIndexOf(':');
            if (startPort != -1) {
                host = host.substring(0, startPort);
            }
        }

        return host;
    }

   /**
    * Checks whether the current instrumentation should start a new extent. If there is already an active extent, then do not start one
    * @return
    */
   protected static boolean shouldStartExtent(String layerName) {
       Map<String, Integer> depthMap = depthThreadLocal.get();
       int currentDepth = depthMap.containsKey(layerName) ? depthMap.get(layerName) : 0 ;
       depthMap.put(layerName, currentDepth + 1);

       if (currentDepth == 0) {
           return true;
       } else {
           return false;
       }
   }

   /**
    * Checks whether the current instrumentation should end the current extent. If this is the active extent being traced, then ends it
    * @return
    */
   protected static boolean shouldEndExtent(String layerName) {
       Map<String, Integer> depthMap = depthThreadLocal.get();
       int currentDepth = depthMap.containsKey(layerName) ? depthMap.get(layerName) : 0 ;
       depthMap.put(layerName, currentDepth - 1);

       if (currentDepth == 1) {
           depthMap.remove(layerName);
           return true;
       } else {
           return false;
       }
   }
}
