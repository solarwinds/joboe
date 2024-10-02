package com.tracelytics.instrumentation.jdbc;

import com.tracelytics.ext.javassist.CannotCompileException;
import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.ext.javassist.NotFoundException;
import com.tracelytics.instrumentation.MethodSignature;
import com.tracelytics.joboe.config.ConfigManager;
import com.tracelytics.joboe.config.ConfigProperty;

/**
 * Applies instrumentation to all classes that implement java.sql.Connection
 *
 * Modifies the statement factory methods (prepareStatement, prepareCall, createStatement) to store SQL and DB name
 * in the statement, so that when that statement is executed, we can report on it.
 *
 */
public class ConnectionInstrumentation extends BaseJDBCInstrumentation {

    // Signatures for prepare* methods that take SQL and create either a prepared are callable statement
    MethodSignature prepMethods[] = {
        new MethodSignature( "prepareStatement", "(Ljava/lang/String;III)Ljava/sql/PreparedStatement;"),
        new MethodSignature( "prepareStatement", "(Ljava/lang/String;II)Ljava/sql/PreparedStatement;"),
        new MethodSignature( "prepareStatement", "(Ljava/lang/String;[I)Ljava/sql/PreparedStatement;"),
        new MethodSignature( "prepareStatement", "(Ljava/lang/String;I)Ljava/sql/PreparedStatement;"),
        new MethodSignature( "prepareStatement", "(Ljava/lang/String;[Ljava/lang/String;)Ljava/sql/PreparedStatement;"),
        new MethodSignature( "prepareStatement", "(Ljava/lang/String;)Ljava/sql/PreparedStatement;"),

        new MethodSignature( "prepareCall", "(Ljava/lang/String;III)Ljava/sql/CallableStatement;"),
        new MethodSignature( "prepareCall", "(Ljava/lang/String;II)Ljava/sql/CallableStatement"),
        new MethodSignature( "prepareCall", "(Ljava/lang/String;)Ljava/sql/CallableStatement;")
    };

    
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {

        if (shouldInstrument(cc)) {
            applyPrepare(cc);
        }

        return true;
    }

    private void applyPrepare(CtClass cc)
        throws CannotCompileException, NotFoundException {

        for (MethodSignature s: prepMethods) {
            CtMethod m = null;

            try {
                m = cc.getMethod(s.getName(), s.getSignature());
                if (!shouldModify(cc, m)) {
                    continue;
                }
            } catch(NotFoundException ex) {
                continue;
            }

            // Store the SQL used to create the statement in the statement so we can log it later:
            // Bypass context check, this should still get run, as the context can be set later on before the query starts
            insertAfter(m, CLASS_NAME + ".saveSQLInStatement((Object)$_, $1);", false, false);
            addSQLErrorReporting(m);
        }
    }


    // Callbacks from instrumented code:
    static public final String CLASS_NAME = "com.tracelytics.instrumentation.jdbc.ConnectionInstrumentation";

    /*
    Attach SQL to the Statement
    Originally tried doing this inline with   m.insertAfter("if ($_ != null) { $_.tlysSQL = $1; }"); but didn't work
    since the statement class was not modified yet...
     */
    public static void saveSQLInStatement(Object stmtObj, String sql) {
      //in some cases (for example Oracle wrapper, see <code>ClassMap.excluded</code>), the statement might not be wrapped. In those cases
      //special handling might be necessary, see <code>StatementInstrumentation.addField</code>
        if (!(stmtObj instanceof Statement)) { 
            return;
        }

        try {
            Statement s = (Statement) stmtObj;
            s.tlysSetSQL(sql);
        } catch(Throwable ex) {
            if (ConfigManager.getConfigOptional(ConfigProperty.AGENT_DEBUG, false)) {
                // Should never happen:
                logger.error(ex.getMessage(), ex);
            }
        }
    }
}
