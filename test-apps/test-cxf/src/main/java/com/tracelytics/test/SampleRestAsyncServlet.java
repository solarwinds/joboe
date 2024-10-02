package com.tracelytics.test;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.provider.json.JSONProvider;

import com.tracelyticstest.rs.SampleResult;
import com.tracelyticstest.rs.SampleServerClient;


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
            testWebClient();
                   
            req.setAttribute("clientAction", "SAMPLE API (REST Async)");
        } catch (Exception e) {
            req.setAttribute("failureMessage", e.getMessage() != null ? e.getMessage() : e.toString());
        }
        
        req.getRequestDispatcher("simple.jsp").forward(req, resp);
    }

    public void testWebClient() {
        WebClient webClient = WebClient.create("http://localhost:8080/test-rest-server");
        webClient = webClient.path("wait", "Default");
        webClient = webClient.query("duration", "1000");
        webClient = webClient.accept("application/json");
        Future<SampleResult> future1 = webClient.async().get(SampleResult.class);
        Future<SampleResult> future2 = webClient.async().get(SampleResult.class);
        try {
            System.out.println(future1.get().method);
            System.out.println(future2.get().method);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ExecutionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
}
