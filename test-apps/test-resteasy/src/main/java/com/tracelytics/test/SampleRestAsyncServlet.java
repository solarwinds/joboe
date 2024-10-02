package com.tracelytics.test;

import java.io.IOException;
import java.util.concurrent.Future;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.client.ClientRequest;
import org.jboss.resteasy.client.ClientResponse;
import org.jboss.resteasy.client.ProxyFactory;

/**
 * Tests sample rest server deployed locally, please see README.txt
 * @author Patson Luk
 *
 */
public class SampleRestAsyncServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {

        //REST CXF Proxy on our own sample server
        try {
            testJaxRsSampleClient();

            req.setAttribute("clientAction", "SAMPLE API (REST Async)");
        } catch (Exception e) {
            req.setAttribute("failureMessage", e.getMessage() != null ? e.getMessage() : e.toString());
        }

        req.getRequestDispatcher("simple.jsp").forward(req, resp);
    }

        
   
    
    /**
    * Test on the sample RESTful server deployed locally (Jax-rs 2.0 Client API compliance, code is not resteasy specific). New in Resteasy 3.0
    * @return
    * @throws Exception
    */
    private Data testJaxRsSampleClient() throws Exception {
        Client client = ClientBuilder.newClient();
        
        WebTarget target = client.target("http://localhost:8080/test-rest-server/{param}").resolveTemplate("param", "Default").queryParam("duration", 100);

        Future<SampleResultContainer> future = target.request().async().get(SampleResultContainer.class);
        System.out.println(future.get().result.method);
        
        return null;
    }
}
