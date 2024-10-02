/*
 * Servlet that calls a remote service (an EJB3 on Jboss 7.x)
 * JBoss 7.x completely changed how remote calls are done.
 *
 *  https://docs.jboss.org/author/display/AS71/EJB+invocations+from+a+remote+client+using+JNDI
 *  https://community.jboss.org/thread/196054
 */
package com.appoptics.test.ejb3;
 
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jboss.ejb.client.EJBClient;

import com.tracelytics.test.ejb3.TestEJB3Remote;

 
public class TestAsyncServlet extends HttpServlet {
 
    public void doGet(HttpServletRequest request, HttpServletResponse response)
    throws IOException{
        PrintWriter out = response.getWriter();
        out.println("<html>");
        out.println("<body>");
        out.println("<h1>EJB3 Client Asynchronous Servlet</h1>");


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

        // note that this requires a user to be added on jboss :
//        Properties props = new Properties();
//
//        String JBOSS_CONTEXT="org.jboss.naming.remote.client.InitialContextFactory";;
//        props.put(Context.INITIAL_CONTEXT_FACTORY, JBOSS_CONTEXT);
//        props.put(Context.PROVIDER_URL, "remote://localhost:8081");
//        props.put(Context.SECURITY_PRINCIPAL, "testuser1");
//        props.put(Context.SECURITY_CREDENTIALS, "testpass1");
//        props.put("jboss.naming.client.ejb.context", true);
//        props.put("jboss.naming.client.connect.options.org.xnio.Options.SASL_POLICY_NOPLAINTEXT", "false");   
        
        
        final Properties ejbProperties = new Properties();
        ejbProperties.put("remote.connectionprovider.create.options.org.xnio.Options.SSL_ENABLED", "false");
        ejbProperties.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
        ejbProperties.put("remote.connections", "1");
        ejbProperties.put("remote.connection.1.host", "localhost");
        ejbProperties.put("remote.connection.1.port", Integer.toString(8081));
        //ejbProperties.put("remote.connection.1.connect.options.org.xnio.Options.SASL_DISALLOWED_MECHANISMS", "JBOSS-LOCAL-USER"); // needed for forcing authentication over remoting (i.e. if you have a custom login module)
        //ejbProperties.put("remote.connection.default.connect.options.org.xnio.Options.SASL_POLICY_NOPLAINTEXT", "false"); // needed for a login module that requires the password in plaintext
        ejbProperties.put("remote.connection.1.username", "testuser1");
        ejbProperties.put("remote.connection.1.password", "testpass1");
//        ejbProperties.put("remote.connection.1.connect.eager", "false");
        
        //ejbProperties.put("org.jboss.ejb.client.scoped.context", "true"); // Not needed when EJBClientContext.setSelector is called programatically

//        final EJBClientConfiguration ejbClientConfiguration = new PropertiesBasedEJBClientConfiguration(ejbProperties);
//        final ConfigBasedEJBClientContextSelector selector = new ConfigBasedEJBClientContextSelector(ejbClientConfiguration);
//        EJBClientContext.setSelector(selector);
        
        
        Context ctx = new InitialContext(ejbProperties);
        Object obj = ctx.lookup("ejb:testejb3-ear-1.0/testejb3-ejb/TestEJB3!com.tracelytics.test.ejb3.TestEJB3Remote");
        
        TestEJB3Remote remote = EJBClient.asynchronous((TestEJB3Remote) obj);
        
        remote.testOp(v);
        
        return (String) EJBClient.getFutureResult().get();
//        return "fireAndForget";
    }

}
