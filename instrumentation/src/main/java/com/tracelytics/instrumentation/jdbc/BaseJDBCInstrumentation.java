/**
 * Base class for JDBC instrumentation: common methods for error handling,  layer identification, etc.
 */
package com.tracelytics.instrumentation.jdbc;

import com.tracelytics.ext.javassist.CannotCompileException;
import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.ext.javassist.NotFoundException;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.joboe.config.ConfigManager;
import com.tracelytics.joboe.config.ConfigProperty;
import com.tracelytics.logging.Logger;
import com.tracelytics.logging.LoggerFactory;

public abstract class BaseJDBCInstrumentation extends ClassInstrumentation
{
    protected static final Logger logger = LoggerFactory.getLogger();
    
    protected String flavor = "";
    
    protected boolean shouldInstrument(CtClass cc) {
        if (ConfigManager.getConfigOptional(ConfigProperty.AGENT_JDBC_INST_ALL, false)) {
            return true;
        }

        DriverVendor driverVendor = DriverVendor.fromPackageName(cc.getPackageName());
         
        if (driverVendor.isWrapper) {
            logger.debug("Not instrumenting JDBC class: " + cc.getName() + " as it is a wrapper driver");
            return false;
        } else {
            return true;
        }
    }
    
     /**
     * Returns name of JDBC layer (associated with specific driver)
      * If a driver isn't recognized, we return "jdbc".
     * @return
     */
    public String getLayerName(CtClass cc) {
        flavor = JdbcDriverUtil.getFlavorName(cc.getPackageName());
        
        // Generic JDBC layer: count not determine specific DB
        if (flavor.equals("jdbc")) {
            return flavor;    
        }
        
        return "jdbc_" + flavor;
    }



    /**
     * Applies error reporting to java.sql.SQLExceptions returned by most JDBC methods.
     */
    protected void addSQLErrorReporting(CtMethod method)
            throws CannotCompileException, NotFoundException {
        addErrorReporting(method, "java.sql.SQLException", layerName, classPool);
    }
}
