package com.tracelytics.instrumentation.ejb;

import java.util.Arrays;
import java.util.List;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.MethodMatcher;

/**
 * Intercepts the <code>getConnection</code> method of <code>org.jboss.ejb.client.remoting.RemotingConnectionManager</code> such that the Connection retrieved will
 * have host and port value set to it. This makes retrieving host/port values from Connection much easier later on
 * 
 * Take note that <code>org.jboss.ejb.client.remoting.RemotingConnectionManager</code> from <code>jboss-ejb-client-2.x</code> is only for JBoss 8. This is preceded by 
 * <code>org.jboss.naming.remote.client.cache.ConnectionCache</code> from JBoss 7(jboss-remote-naming-1.x).
 * 
 * @author Patson Luk
 *
 */

public class JbossEjbConnectionManagerPatcher extends ClassInstrumentation {
    private static String CLASS_NAME = JbossEjbConnectionManagerPatcher.class.getName();
    
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<Object>> methodMatchers = Arrays.asList(new MethodMatcher<Object>("getConnection", 
            new String[] { "org.jboss.remoting3.Endpoint", "java.lang.String", "java.lang.String", "int" }, "org.jboss.remoting3.Connection"));
    
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {
        
        for (CtMethod method : findMatchingMethods(cc, methodMatchers).keySet()) {
            insertAfter(method, CLASS_NAME + ".tagHost($3, $4, $_);", true);
        }
        
        return true;
    }

    public static void tagHost(String host, int port, Object connectionObject) {
        if (connectionObject instanceof JbossConnection) {
            ((JbossConnection)connectionObject).tvSetHost(host);
            ((JbossConnection)connectionObject).tvSetPort(port);
        }
    }
}
