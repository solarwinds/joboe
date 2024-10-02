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

import com.appoptics.api.ext.LogMethod;
import com.tracelyticstest.rs.Data;
import com.tracelyticstest.rs.SampleResult;
import com.tracelyticstest.rs.SampleServerClient;


/**
 * Tests sample rest server deployed locally, please see README.txt
 * @author Patson Luk
 *
 */
public class ExceptionRestServlet extends HttpServlet {


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {
        
        //REST CXF Proxy on our own sample server
        try {
            testNotFoundProxyClient();
        } catch (Exception e) {
            req.setAttribute("failureMessage", e.getMessage() != null ? e.getMessage() : e.toString());
        }
        
        try {
            testNotFoundWebClient();
        } catch (Exception e) {
            req.setAttribute("failureMessage", e.getMessage() != null ? e.getMessage() : e.toString());
        }
        
        try {
            testInternalServerErrorWebClient();
        } catch (Exception e) {
            req.setAttribute("failureMessage", e.getMessage() != null ? e.getMessage() : e.toString());
        }
        
        req.getRequestDispatcher("simple.jsp").forward(req, resp);
    }

    @LogMethod(layer = "notFoundProxy", reportExceptions=false)
    private void testNotFoundProxyClient() {
        //REST Proxy-based API test on local server
        SampleServerClient client = JAXRSClientFactory.create("http://localhost:1111/test-rest-server", SampleServerClient.class, Collections.singletonList(new JSONProvider()));
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
    }
    
    @LogMethod(layer = "notFoundWebClient", reportExceptions=false)
    private void testNotFoundWebClient() {
        WebClient webClient = WebClient.create("http://localhost:1111/test-rest-server");
        webClient = webClient.path("wait", "Default");
        webClient = webClient.query("duration", "10000");
        webClient = webClient.accept("application/json");
        webClient.get(SampleResult.class);
    }
    
    @LogMethod(layer = "internalServerErrorWebClient", reportExceptions=false)
    private void testInternalServerErrorWebClient() {
        WebClient webClient = WebClient.create("http://localhost:8080/test-rest-server");
        webClient = webClient.query("exception", "true");
        webClient = webClient.accept("application/json");
        webClient.get(SampleResult.class);
    }
    
}
