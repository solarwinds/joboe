package com.tracelytics.test;

import java.io.IOException;
import java.util.Collections;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.provider.JSONProvider;

import com.appoptics.api.ext.LogMethod;
import com.tracelyticstest.rs.Data;
import com.tracelyticstest.rs.TracelyticsClient;


/**
 * Test on TV API rest
 * @author Patson Luk
 *
 */
public class ApiRestServlet extends HttpServlet {


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {
        
        try {
            //Start Rest test
            //REST CXF proxy
            testRestProxy();
            
            //REST CXF WebClient API
            testRestWebClient();
            
            req.setAttribute("clientAction", "TraceView API (REST)");
        } catch (Exception e) {
            req.setAttribute("failureMessage", e.getMessage() != null ? e.getMessage() : e.toString());
        }
        req.getRequestDispatcher("simple.jsp").forward(req, resp);
    }

  
    @LogMethod(layer = "testRestProxy")
    private void testRestProxy() {
        //REST Proxy-based API test
        TracelyticsClient client = JAXRSClientFactory.create("https://api.tracelytics.com/api-v1", TracelyticsClient.class, Collections.singletonList(new JSONProvider()));
        Data data = client.getResult("Default", "9de3ed4c-5e33-4e26-b0b7-82c20ea01532");
        System.out.println(data.average);
        System.out.println(data.count);
        System.out.println(data.latest);
    }

    @LogMethod(layer = "testRestWebClient")
    private void testRestWebClient() {
        WebClient webClient = WebClient.create("https://api.tracelytics.com/api-v1");
        webClient = webClient.path("latency/Default/server/summary");
        webClient = webClient.query("key", "9de3ed4c-5e33-4e26-b0b7-82c20ea01532");
        webClient = webClient.accept("application/json");
        Data data2 = webClient.get(Data.class);
        
        System.out.println(data2.average);
        System.out.println(data2.count);
        System.out.println(data2.latest);
    }
    
}
