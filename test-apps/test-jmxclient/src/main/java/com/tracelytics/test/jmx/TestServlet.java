/*
 * Servlet that calls a remote service (an MBean on Jboss) through HTTP Invocation
 * Remote invoke code from From http://www.hsc.fr/ressources/outils/jisandwis/download/
 */
package com.tracelytics.test.jmx;
 
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.management.ObjectName;;

import org.jboss.console.remote.RemoteMBeanAttributeInvocation;
import org.jboss.console.remote.RemoteMBeanInvocation;
 
public class TestServlet extends HttpServlet {
 
    public void doGet(HttpServletRequest request, HttpServletResponse response)
    throws IOException{
        PrintWriter out = response.getWriter();
        out.println("<html>");
        out.println("<body>");
        out.println("<h1>JMX Client Servlet</h1>");


        try {
            out.println("<b>Results:" + callRemote(1234) + "</b>"); 
        } catch(Exception ex) {
            ex.printStackTrace(out);            
        }

        out.println("</body>");
        out.println("</html>"); 
    }


    private String callRemote(Integer v)
        throws Exception {
        Proxy proxy = ProxyFactory.createProxy("http://localhost:8080/invoker/JMXInvokerServlet");
        return proxy.invoke((RemoteMBeanInvocation) remoteMBeanInvocation("tracelytics.com:service=Test", "testOp", "java.lang.Integer", ""+v));
    }

    private RemoteMBeanInvocation remoteMBeanInvocation(String mbean, String invokeMethod, String invokeSignature, String arg)
        throws Exception {
        Object[] invokeParamsArray = null;
        String[] invokeSignatureArray = null;
        String[] invokeParamsStringArray = new String[1];

        invokeParamsStringArray[0] = arg;

        // Default signature is java.lang.String
        if (invokeSignature == null) {
            invokeSignatureArray = new String[invokeParamsStringArray.length];
            for (int i=0; i<invokeParamsStringArray.length; i++) {
                invokeSignatureArray[i] = "java.lang.String";
            }
        } else {
            invokeSignatureArray = invokeSignature.split(";");
        }

        // Object conversion
        invokeParamsArray = TypeConvertor.convertObjectsFromString(invokeParamsStringArray, invokeSignatureArray);
        return new RemoteMBeanInvocation(new ObjectName(mbean), invokeMethod, invokeParamsArray, invokeSignatureArray);
    }


}
