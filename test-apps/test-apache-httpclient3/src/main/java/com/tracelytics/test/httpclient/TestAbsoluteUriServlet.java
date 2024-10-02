package com.tracelytics.test.httpclient;
 
import java.io.IOException;
import java.util.LinkedHashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
 
public class TestAbsoluteUriServlet extends HttpServlet {
    private static final String[] ENDPOINT_URLS = new String[] {
                                                                "http://localhost:8080/test-httpclient3/test.html",
                                                                "https://www.random.org/integers/?num=100&min=1&max=6&col=1&base=10&format=plain&rnd=new",
                                                                "http://127.0.0.1:65000/fail/"
                                                                }; 
     
    
 
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        LinkedHashMap<Endpoint, String> result = new LinkedHashMap<Endpoint, String>();
        
        EndpointClient endpointClient = new AbsoluteEndpointClient(); 
        
        for (String endpointUrl : ENDPOINT_URLS) {
            AbsoluteEndpoint endpoint = new AbsoluteEndpoint(endpointUrl);
            result.put(endpoint, endpointClient.getURL(endpointUrl));
        }
        
        request.setAttribute("result", result);
        try {
            request.getRequestDispatcher("/index.jsp").forward(request, response);
        } catch (ServletException e) {
            e.printStackTrace();
        }
    }

}
