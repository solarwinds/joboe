package com.tracelytics.instrumentation.jdbc;

import com.google.auto.service.AutoService;
import com.tracelytics.ext.javassist.CannotCompileException;
import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtField;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.ext.javassist.CtNewMethod;
import com.tracelytics.ext.javassist.Modifier;
import com.tracelytics.ext.javassist.NotFoundException;
import com.tracelytics.instrumentation.*;
import com.tracelytics.joboe.config.ConfigManager;
import com.tracelytics.joboe.config.ConfigProperty;

/**
 *  Specific instrumentation for classes that implement PreparedStatement. It is used to track parameters on top of all the other elements being tracked by the
 *  parent instrumentation <code>StatementInstrumentation</code>
 *  
 *  Modifies the class to track parameters set by the setXXX (setInt, setString etc) and setObject methods of <code>PreparedStatement</code>
 */
@AutoService(ClassInstrumentation.class)
@Instrument(targetType = { "java.sql.PreparedStatement" },
        module = com.tracelytics.instrumentation.Module.JDBC,
        appLoaderPackage = { "com.appoptics.apploader.instrumenter.jdbc" })
public class PreparedStatementInstrumentation extends StatementInstrumentation {
    //Max character counts allowed by any parameter that has string representation as tracked value
    private static final String EVENT_VALUE_CONVERTER_CLASS = "com.appoptics.apploader.instrumenter.jdbc.JdbcEventValueConverter";
    
    //all the setXXX methods provided in java 1.6. Some methods might be missing in java 1.5, and those methods will be skipped
    private static MethodSignature[] setParameterMethods = {
        new MethodSignature("setArray", "(ILjava/sql/Array;)V"),
        new MethodSignature("setAsciiStream", "(ILjava/io/InputStream;)V"), //1.6 method
        new MethodSignature("setAsciiStream", "(ILjava/io/InputStream;I)V"),
        new MethodSignature("setAsciiStream", "(ILjava/io/InputStream;J)V"), //1.6 method
        new MethodSignature("setBigDecimal", "(ILjava/math/BigDecimal;)V"),
        new MethodSignature("setBinaryStream", "(ILjava/io/InputStream;)V"), //1.6 method
        new MethodSignature("setBinaryStream", "(ILjava/io/InputStream;I)V"),
        new MethodSignature("setBinaryStream", "(ILjava/io/InputStream;J)V"), //1.6 method
        new MethodSignature("setBlob", "(ILjava/sql/Blob;)V"),
        new MethodSignature("setBlob", "(ILjava/io/InputStream;)V"), //1.6 method
        new MethodSignature("setBlob", "(ILjava/io/InputStream;J)V"), //1.6 method
        new MethodSignature("setBoolean", "(IZ)V"),
        new MethodSignature("setByte", "(IB)V"),
        new MethodSignature("setBytes", "(I[B)V"),
        new MethodSignature("setCharacterStream", "(ILjava/io/Reader;)V"), //1.6 method
        new MethodSignature("setCharacterStream", "(ILjava/io/Reader;I)V"),
        new MethodSignature("setCharacterStream", "(ILjava/io/Reader;J)V"), //1.6 method
        new MethodSignature("setClob", "(ILjava/sql/Clob;)V"),
        new MethodSignature("setClob", "(ILjava/io/Reader;)V"), //1.6 method
        new MethodSignature("setClob", "(ILjava/io/Reader;J)V"), //1.6 method
        new MethodSignature("setDate", "(ILjava/sql/Date;)V"),
        new MethodSignature("setDate", "(ILjava/sql/Date;Ljava/util/Calendar;)V"),
        new MethodSignature("setDouble", "(ID)V"),
        new MethodSignature("setFloat", "(IF)V"),
        new MethodSignature("setInt", "(II)V"),
        new MethodSignature("setLong", "(IJ)V"),
        new MethodSignature("setNCharacterStream", "(ILjava/io/Reader;)V"), //1.6 method 
        new MethodSignature("setNCharacterStream", "(ILjava/io/Reader;J)V"), //1.6 method
        new MethodSignature("setNClob", "(ILjava/sql/NClob;)V"), //1.6 method
        new MethodSignature("setNClob", "(ILjava/io/Reader;)V"), //1.6 method
        new MethodSignature("setNClob", "(ILjava/io/Reader;J)V"), //1.6 method
        new MethodSignature("setNString", "(ILjava/lang/String;)V"), //1.6 method
        new MethodSignature("setObject", "(ILjava/lang/Object;)V"),
        new MethodSignature("setObject", "(ILjava/lang/Object;I)V"),
        new MethodSignature("setObject", "(ILjava/lang/Object;II)V"), //1.6 method
        new MethodSignature("setRef", "(ILjava/sql/Ref;)V"),
        new MethodSignature("setRowId", "(ILjava/sql/RowId;)V"), //1.6 method
        new MethodSignature("setShort", "(IS)V"),
        new MethodSignature("setSQLXML", "(ILjava/sql/SQLXML;)V"), //1.6 method
        new MethodSignature("setString", "(ILjava/lang/String;)V"),
        new MethodSignature("setTime", "(ILjava/sql/Time;)V"),
        new MethodSignature("setTime", "(ILjava/sql/Time;Ljava/util/Calendar;)V"),
        new MethodSignature("setTimestamp", "(ILjava/sql/Timestamp;)V"),
        new MethodSignature("setTimestamp", "(ILjava/sql/Timestamp;Ljava/util/Calendar;)V"),
        new MethodSignature("setUnicodeStream", "(ILjava/io/InputStream;I)V"),
        new MethodSignature("setURL", "(ILjava/net/URL;)V"),
    };
    
    
    private static MethodSignature[] setNullMethods = {
        new MethodSignature("setNull", "(II)V"),
        new MethodSignature("setNull", "(IILjava/lang/String;)V"),
    };
    
    
    private static final String TLYS_PARAMETERS_FIELD = "tlysParameters";
  
        
    @Override
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {
        if (!super.applyInstrumentation(cc, className, classBytes)) {
            return false;
        }

        Integer sanitizeFlag = (Integer) ConfigManager.getConfig(ConfigProperty.AGENT_SQL_SANITIZE);
        
        if (sanitizeFlag != null && sanitizeFlag == SQLSanitizer.DISABLED) { //Only report parameter instrumentation if sanitize is explicitly disabled
            addField(cc);
            applySetParameter(cc);
            applySetNull(cc);
            
            // Add our interface so we can access the modified class during layer exit when adding QueryArgs KV
            CtClass iface = classPool.getCtClass("com.tracelytics.instrumentation.jdbc.PreparedStatement");

            for (CtClass i : cc.getInterfaces()) {
                if (i.equals(iface)) {
                    return true;
                }
            }

            cc.addInterface(iface);
        }

        return true;
    }

    @Override
    protected void addField(CtClass cc)
        throws CannotCompileException, NotFoundException {
        super.addField(cc);
        
        // Check if these fields may have already been added (in a base class)::
        try {
            if (cc.getField(TLYS_PARAMETERS_FIELD) != null) {
                return;
            }
        } catch(NotFoundException ex) {
            // Continue
        }
        
        // The methods may have been added for some Wrapper implementation (WebLogic WrapperFactory etc)
        try {
            if (cc.getDeclaredMethod("tlysSetParameter") != null) {
                return;
            }
        } catch(NotFoundException ex) {
            // Continue
        }

        CtField f;
        CtMethod m;
        
        // To track parameters, use sortedMap so the converted param array will be ordered by the index (key in this map)
        f = new CtField(classPool.getCtClass("java.util.SortedMap"), TLYS_PARAMETERS_FIELD, cc);
        f.setModifiers(Modifier.PRIVATE);
        
        cc.addField(f, "new java.util.TreeMap()");
        
        //set parameter method
        m = CtNewMethod.make("public void tlysSetParameter(int index, Object parameter) { " + TLYS_PARAMETERS_FIELD + ".put(Integer.valueOf(index), parameter); }", cc);
        cc.addMethod(m);
        
        //get parameters method
        m = CtNewMethod.make("public java.util.SortedMap tlysGetParameters() { return " + TLYS_PARAMETERS_FIELD + "; }", cc);
        cc.addMethod(m);
    }

   
    private void applySetParameter(CtClass cc) throws CannotCompileException, NotFoundException {
        for (MethodSignature s: setParameterMethods) {
            CtMethod m = null;
            
            try {
                m = cc.getMethod(s.getName(), s.getSignature());
                if (!shouldModify(cc, m)) {
                    continue;
                }
                
            } catch(NotFoundException ex) {
                continue;
            }

            insertBefore(m, PreparedStatementInstrumentation.class.getName() + ".saveParameterInStatement(this, $1, " + EVENT_VALUE_CONVERTER_CLASS + ".convert($args[1]));"); //use $args[1] as $2 could be primitive type and cannot be cast to Object
            
            addSQLErrorReporting(m);
        }
    }
    
    private void applySetNull(CtClass cc) throws CannotCompileException, NotFoundException {
        for (MethodSignature s: setNullMethods) {
            CtMethod m = null;
            
            try {
                m = cc.getMethod(s.getName(), s.getSignature());
                if (!shouldModify(cc, m)) {
                    continue;
                }
                
            } catch(NotFoundException ex) {
                continue;
            }

            insertBefore(m, PreparedStatementInstrumentation.class.getName() + ".saveParameterInStatement(this, $1, null);");  
            
            addSQLErrorReporting(m);
        }
    }

    /**
     * Saves the parameter to the internal field so we can get an array of parameters later.
     * @param stmtObj
     * @param index
     * @param parameter
     */
    public static void saveParameterInStatement(Object stmtObj, int index, Object processedParameter) {
        try {
            PreparedStatement s = (PreparedStatement) stmtObj;
            
            s.tlysSetParameter(index, processedParameter); //safe to put directly to the map even if it's null

        } catch(Throwable ex) {
            logger.warn(ex.getMessage());
        }
    }
}
