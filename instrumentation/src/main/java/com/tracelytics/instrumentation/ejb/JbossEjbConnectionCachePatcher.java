package com.tracelytics.instrumentation.ejb;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.MethodMatcher;

/**
 * Intercepts the <code>get</code> method of <code>org.jboss.naming.remote.client.cache.ConnectionCache</code> such that the Connection retrieved will
 * have host and port value set to it. This makes retrieving host/port values from Connection much easier later on
 * 
 * Take note that <code>org.jboss.naming.remote.client.cache.ConnectionCache</code> is only for JBoss 7(jboss-remote-naming-1.x). For JBoss 8, this is replaced
 * by <code>org.jboss.ejb.client.remoting.RemotingConnectionManager</code> from <code>jboss-ejb-client-2.x</code> 
 * 
 * @author Patson Luk
 *
 */
public class JbossEjbConnectionCachePatcher extends ClassInstrumentation {
    private static String CLASS_NAME = JbossEjbConnectionCachePatcher.class.getName();
    
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<Object>> methodMatchers = Arrays.asList(new MethodMatcher<Object>("get", 
            new String[] { "org.jboss.remoting3.Endpoint", "java.net.URI" }, "org.jboss.remoting3.Connection"));
    
    
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {
        
        for (CtMethod method : findMatchingMethods(cc, methodMatchers).keySet()) {
            insertAfter(method, CLASS_NAME + ".tagHost($2, $_);", true);
        }
        
        return true;
    }

    public static void tagHost(URI destination, Object connectionObject) {
        if (connectionObject instanceof JbossConnection && destination != null) {
            ((JbossConnection)connectionObject).tvSetHost(destination.getHost());
            ((JbossConnection)connectionObject).tvSetPort(destination.getPort());
        }
    }
}
