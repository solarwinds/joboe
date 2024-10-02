package com.tracelytics.test.httpclient;
 
import java.io.IOException;
import java.util.LinkedHashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
 
public class TestRelativeUriServlet extends HttpServlet {
    public static final RelativeEndpoint[] ENDPOINTS = { new RelativeEndpoint("localhost", 8080, "/test-httpclient4.4/test.html", "http"),
                                                 new RelativeEndpoint("www.random.org", null, "/integers/?num=100&min=1&max=6&col=1&base=10&format=plain&rnd=new", "https"),
                                                 new RelativeEndpoint("localhost", 65000, "/fail", "http")}; 
    
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        LinkedHashMap<Endpoint, String> result = new LinkedHashMap<Endpoint, String>();
        
        boolean modifyClient = request.getParameter("modifyClient") != null ? Boolean.valueOf(request.getParameter("modifyClient")) : false;
        
        for (RelativeEndpoint endpoint : ENDPOINTS) {
            EndpointClient endpointClient = new RelativeEndpointClient(modifyClient, endpoint.getHost(), endpoint.getPort(), endpoint.getScheme());
            result.put(endpoint, endpointClient.getURL(endpoint.getUri()));
        }
        
        request.setAttribute("result", result);
        try {
            request.getRequestDispatcher("/index.jsp").forward(request, response);
        } catch (ServletException e) {
            e.printStackTrace();
        }
    }
    
    

}
