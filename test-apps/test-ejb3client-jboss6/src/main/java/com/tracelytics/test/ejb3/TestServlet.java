/*
 * Servlet that calls a remote service (an EJB3 on Jboss)
 */
package com.tracelytics.test.ejb3;
 
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Properties;
import java.util.Random;

import javax.naming.*;
import javax.ejb.*;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

 
public class TestServlet extends HttpServlet {
 
    public void doGet(HttpServletRequest request, HttpServletResponse response)
    throws IOException{
        PrintWriter out = response.getWriter();
        out.println("<html>");
        out.println("<body>");
        out.println("<h1>EJB3 Client Servlet</h1>");


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

        // From https://community.jboss.org/wiki/EJBJMSAndJNDIOverHTTPWithUnifiedInvoker
        // https://community.jboss.org/message/184133#184133
        Properties props = new Properties();
        props.put(Context.INITIAL_CONTEXT_FACTORY, "org.jboss.naming.HttpNamingContextFactory");
        //props.put(Context.PROVIDER_URL, "http://127.0.0.1:8080/invoker/JMXInvokerServlet"); // incorrect docs??
        props.put(Context.PROVIDER_URL, "http://127.0.0.1:8081/invoker/JNDIFactory");
//        props.put(Context.PROVIDER_URL, "http://localvm:8080/invoker/JNDIFactory");
        props.put(Context.URL_PKG_PREFIXES, "org.jboss.naming:org.jnp.interfaces");
        
        props.put(Context.SECURITY_PRINCIPAL, "testuser1");
        props.put(Context.SECURITY_CREDENTIALS, "testpass1");
//        props.put("jboss.naming.client.ejb.context", true);
//        props.put("jboss.naming.client.connect.options.org.xnio.Options.SASL_POLICY_NOPLAINTEXT", "false");
//        
        
        Context ctx = new InitialContext(props);
        Object obj = ctx.lookup("TestEJB3/remote");
        TestEJB3Remote remote = (TestEJB3Remote) obj;

        Random rnd = new Random();

        String result;
        if ( (rnd.nextInt() % 10) < 3 ) {
            result = remote.testOp(v);
        } else {
            result = remote.anotherOp(v);
        }

        return result;
    }

}
