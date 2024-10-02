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
import org.apache.cxf.jaxrs.provider.JSONProvider;

import com.tracelyticstest.rs.SampleResult;
import com.tracelyticstest.rs.SampleServerClient;


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
            testSampleRest();
                   
            req.setAttribute("clientAction", "SAMPLE API (REST)");
        } catch (Exception e) {
            req.setAttribute("failureMessage", e.getMessage() != null ? e.getMessage() : e.toString());
        }
        
        req.getRequestDispatcher("simple.jsp").forward(req, resp);
    }

    //    @LogMethod(layer = "testSampleRest")
    private void testSampleRest() {
        //REST Proxy-based API test on local server
        SampleServerClient client = JAXRSClientFactory.create("http://localhost:8080/test-rest-server", SampleServerClient.class, Collections.singletonList(new JSONProvider()));
        SampleResult result;
        
        result = client.getResult("1", "2");
//        System.out.println(result.method);
//        System.out.println(result.path);
        
        result = client.putResult("3", "4");
//        System.out.println(result.method);
//        System.out.println(result.path);
        
        result = client.postResult("5", "6");
//        System.out.println(result.method);
//        System.out.println(result.path);
        
        result = client.deleteResult("7", "8");
//        System.out.println(result.method);
//        System.out.println(result.path);
        
        WebClient webClient = WebClient.create("http://localhost:8080/test-rest-server");
        webClient = webClient.path("dummyPath");
        webClient = webClient.query("duration", "500");
        webClient = webClient.accept("application/json");
        result= webClient.get(SampleResult.class);
        
    }
    
}
