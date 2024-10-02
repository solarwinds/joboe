package com.tracelytics.instrumentation.jdbc;

import java.lang.reflect.Field;

import com.tracelytics.ext.javassist.CannotCompileException;
import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.ext.javassist.NotFoundException;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.MethodSignature;

/**
 * Special handling for Oracle host. Since Oracle accepts tnsnames.ora formatted URL, we cannot use the URL directly from the connection's metadata.
 * 
 * Instead we will intercept the <code>oracle.net.ns.NetOutputStream</code> which the JDBC Thin drivers use and extract host information from there.
 * 
 * We also make use of the ThreadLocal in the <code>StatementInstrumentation</code> to relate the outputStream to the Statement being instrumented.
 *  
 * @see <a href="http://docs.oracle.com/cd/E11882_01/java.112/e16548/urls.htm#JJDBC08200">Oracle Database URLs and Database Specifiers</a>
 * @see <a href="http://www.oracle.com/technetwork/database/enterprise-edition/jdbc-faq-090281.html#02_01">Oracle JDBC Drivers</a>
 * @author Patson Luk
 *
 */
public class OracleNetOutputStreamInstrumentation extends ClassInstrumentation {
    //Intercept the write method
    MethodSignature writeMethods[] = {
        new MethodSignature( "write", "([BII)V"),
    };

    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {

        applyWrite(cc);

        return true;
    }

    private void applyWrite(CtClass cc)
        throws CannotCompileException, NotFoundException {

        for (MethodSignature s: writeMethods) {
            CtMethod m = null;

            try {
                m = cc.getMethod(s.getName(), s.getSignature());
                if (!shouldModify(cc, m)) {
                    continue;
                }
            } catch(NotFoundException ex) {
                continue;
            }

            insertBefore(m, CLASS_NAME + ".saveHost(this.sAtts != null ? this.sAtts.getNTAdapter() : null, this.sAtts != null ? this.sAtts.cOption : null);");
        }
    }

    public static final String CLASS_NAME = "com.tracelytics.instrumentation.jdbc.OracleNetOutputStreamInstrumentation";

    /**
     * Extracts the host name from the adapter
     * @param adapter   The adapter used in the output stream
     */
    public static void saveHost(Object adapter, Object cOption) {
        
        
        Statement statement = StatementInstrumentation.statements.get();
        
      //isHostSet flag to avoid excessive processing as the write() method can be called very often
        if (statement != null && !statement.tlysIsHostSet() && adapter != null) {
            try {
                Field hostField = adapter.getClass().getDeclaredField("host");
                hostField.setAccessible(true);
                String host = (String) hostField.get(adapter);
                
//                Field portField = adapter.getClass().getDeclaredField("port");
//                portField.setAccessible(true);
//                int port = (Integer)portField.get(adapter);
                
                statement.tlysSetHost(host);
                
                //get the sid / service name 
                if (cOption != null) {
                    Field sidField = cOption.getClass().getField("sid");
                    String sid = (String)sidField.get(cOption);
                    
                    if (sid != null) {
                        statement.tlysSetDB(sid);
                    } else { //sid is null, now try service name
                        Field serviceNameField = cOption.getClass().getField("service_name");
                        String serviceName = (String)serviceNameField.get(cOption);
                        statement.tlysSetDB(serviceName);
                    }
                }
            } catch (IllegalArgumentException e) {
                logger.warn(e.getMessage(), e);
            } catch (IllegalAccessException e) {
                logger.warn(e.getMessage(), e);
            } catch (NoSuchFieldException e) {
                logger.warn(e.getMessage(), e);
            } catch (SecurityException e) {
                logger.warn(e.getMessage(), e);
            }
        }
        
    }

}
