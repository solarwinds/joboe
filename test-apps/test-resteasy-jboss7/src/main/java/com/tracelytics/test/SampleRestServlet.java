package com.tracelytics.test;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jboss.resteasy.client.ClientRequest;
import org.jboss.resteasy.client.ClientResponse;
import org.jboss.resteasy.client.ProxyFactory;

/**
 * Tests sample rest server deployed locally, please see README.txt
 * @author Patson Luk
 *
 */
public class SampleRestServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {

        //REST CXF Proxy on our own sample server
        try {
            testManualSampleClient();
            testProxySampleClient();

            req.setAttribute("clientAction", "SAMPLE API (REST)");
        } catch (Exception e) {
            req.setAttribute("failureMessage", e.getMessage() != null ? e.getMessage() : e.toString());
        }

        req.getRequestDispatcher("simple.jsp").forward(req, resp);
    }

    /**
     * Test on the sample RESTful server deployed locally
     * @return
     * @throws Exception
     */
    private Data testManualSampleClient() throws Exception {
        ClientRequest request = new ClientRequest("http://localhost:8080/test-rest-server/{param}");
        request.pathParameter("param", "Default").queryParameter("duration", 100);
        
        ClientResponse<SampleResultContainer> response;
        response = request.get(SampleResultContainer.class);

        System.out.println(response.getEntity().result.method);
        
        return null;
    }
    
    /**
     * Test on the sample RESTful server deployed locally
     * @return
     */
    private Data testProxySampleClient() {
        SampleResult result;
        SampleClient client = ProxyFactory.create(SampleClient.class, "http://localhost:8080/test-rest-server");
        
        result = client.getResult("dummy", 100).result;
        System.out.println(result.path);
        
        result = client.putResult("dummy", 100).result;
        System.out.println(result.path);
        
        result = client.deleteResult("dummy", 100).result;
        System.out.println(result.path);
        
        result = client.postResult("dummy", 100).result;
        System.out.println(result.path);
        
        return null;
    }
}    
