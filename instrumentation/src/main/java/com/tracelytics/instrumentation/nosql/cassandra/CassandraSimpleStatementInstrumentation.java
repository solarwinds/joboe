package com.tracelytics.instrumentation.nosql.cassandra;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtConstructor;
import com.tracelytics.ext.javassist.NotFoundException;
import com.tracelytics.instrumentation.jdbc.SQLSanitizer;
import com.tracelytics.joboe.config.ConfigManager;
import com.tracelytics.joboe.config.ConfigProperty;

/**
 * Instruments Cassandra's <code>SimpleStatement</code>, in newer version, it allows instantiation of <code>SimpleStatement</code> with query parameters.
 * Therefore we will need to capture the parameters used in the ctor of <code>SimpleStatement</code>
 * 
 * @author pluk
 *
 */
public class CassandraSimpleStatementInstrumentation extends CassandraStatementInstrumentation {
    @Override
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {
        super.applyInstrumentation(cc, className, classBytes);
        
        CtClass simpleStatementClass = classPool.get("com.datastax.driver.core.SimpleStatement");
        
        if (!simpleStatementClass.equals(cc)) { //we only want to modify the constructor of com.datastax.driver.core.SimpleStatement
            return false;
        }
        
        Integer sanitizeFlag = (Integer) ConfigManager.getConfig(ConfigProperty.AGENT_SQL_SANITIZE);
        
        if (sanitizeFlag != null && sanitizeFlag == SQLSanitizer.DISABLED) { //Only report parameter instrumentation if sanitize is explicitly disabled
            tagAsStatementWithParameters(cc);
            
            //modify ctor to capture parameters used in the statement
            try {
                CtConstructor constructor = cc.getConstructor("(Ljava/lang/String;[Ljava/lang/Object;)V");
                insertAfter(constructor,
                            "if ($2 != null) {" +
                            "    for (int i = 0; i < $2.length; i ++) {" +
                            "        tvSetParameter(i, " + STATEMENT_INSTRUMENTATION_CLASS_NAME + ".getBsonValue($2[i]));" +
                            "    }" +
                            "}",
                            true);
            } catch (NotFoundException e) {
                //ok...probably running older version that does not take parameters in ctor
                logger.debug("Cannot find SimpleStatement ctor that takes in query parameters. Probably running an older version");
            }
        }

        return true;
    }
}
