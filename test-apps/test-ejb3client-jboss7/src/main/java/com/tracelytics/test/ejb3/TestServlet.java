/*
 * Servlet that calls a remote service (an EJB3 on Jboss 7.x)
 * JBoss 7.x completely changed how remote calls are done.
 *
 *  https://docs.jboss.org/author/display/AS71/EJB+invocations+from+a+remote+client+using+JNDI
 *  https://community.jboss.org/thread/196054
 */
package com.tracelytics.test.ejb3;
 
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Properties;

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

        // note that this requires a user to be added on jboss :
        Properties props = new Properties();

        String JBOSS_CONTEXT="org.jboss.naming.remote.client.InitialContextFactory";;
        props.put(Context.INITIAL_CONTEXT_FACTORY, JBOSS_CONTEXT);
        props.put(Context.PROVIDER_URL, "remote://localhost:4447");
        props.put(Context.SECURITY_PRINCIPAL, "testuser1");
        props.put(Context.SECURITY_CREDENTIALS, "testpass1");
        props.put("jboss.naming.client.ejb.context", true);
        props.put("jboss.naming.client.connect.options.org.xnio.Options.SASL_POLICY_NOPLAINTEXT", "false");   

        Context ctx = new InitialContext(props);
        Object obj = ctx.lookup("testejb3-ear-1.0/testejb3-ejb/TestEJB3!com.tracelytics.test.ejb3.TestEJB3Remote");
        TestEJB3Remote remote = (TestEJB3Remote) obj;

        return remote.testOp(v);
    }

}
