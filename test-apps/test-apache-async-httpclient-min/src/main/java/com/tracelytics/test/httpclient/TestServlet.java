package com.tracelytics.test.httpclient;
 
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpResponse;

import static com.tracelytics.test.httpclient.TestServlet.Target.Method.*;
 
public class TestServlet extends HttpServlet {
    private static final Target[] TARGETS = new Target[] { 
            new Target("http", "localhost", 8080, "/test-apache-async-httpclient/test.html", GET),
            new Target("http", "localhost", 8080, "/test-apache-async-httpclient/test.html", POST),
            new Target("http", "localhost", 8080, "/test-apache-async-httpclient/test.html", PUT),
            new Target("http", "localhost", 8080, "/test-apache-async-httpclient/test.html", HEAD),
            new Target("http", "localhost", 8080, "/test-apache-async-httpclient/test.html", DELETE),
            new Target("https", "www.random.org", null, "/integers/?num=100&min=1&max=6&col=1&base=10&format=plain&rnd=new", GET),
            new Target("http", "127.0.0.1", 65000, "/fail/", GET),
            new Target("http", "127.0.0.1", 65000, "/fail/", POST)}; 
    
    
    static class Target {
        String protocol;
        String host;
        Integer port;
        String uri;
        Method method;
        
        public Target(String protocol, String host, Integer port, String uri, Method method) {
            super();
            this.protocol = protocol;
            this.host = host;
            this.port = port;
            this.uri = uri;
            this.method = method;
        }
        
        String getFullUrl() {
            return protocol + "://" + host + (port != null ? (":" + port) : "") + uri;
        } 
        
        enum Method { GET, POST, PUT, DELETE, HEAD }
    }
    
    
    
    
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        EndpointClient endpointClient = new EndpointClient();
        
        try {
            Map<Endpoint, Future<HttpResponse>> responseFutures = new LinkedHashMap<Endpoint, Future<HttpResponse>>();
            
            for (Target target : TARGETS) {
                //use full URL
                FullUrlEndpoint fullUrlEndpoint = new FullUrlEndpoint(target);
                responseFutures.put(fullUrlEndpoint, endpointClient.getURL(fullUrlEndpoint));
                
                HttpHostEndpoint httpHostEndpoint = new HttpHostEndpoint(target);
                responseFutures.put(httpHostEndpoint, endpointClient.getURL(httpHostEndpoint));
            }
            Map<Endpoint, String> result = buildResults(responseFutures);
            
            request.setAttribute("result", result);
        
            request.getRequestDispatcher("/index.jsp").forward(request, response);
        } finally {
            if (endpointClient != null) {
                endpointClient.close(); //JDK 1.6....
            }
        }
    }
    
    private Map<Endpoint, String> buildResults(Map<Endpoint, Future<HttpResponse>> responseFutures) {
        Map<Endpoint, String> results = new LinkedHashMap<Endpoint, String>();
        
        for (Entry<Endpoint, Future<HttpResponse>> responseFutureEntry : responseFutures.entrySet()) {
            HttpResponse response;
            String result;
            try {
                response = responseFutureEntry.getValue().get();
                result = "Status: " + response.getStatusLine().getStatusCode();
                
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                
                if (response.getEntity() != null) {
                    response.getEntity().writeTo(baos);
                }
                
                result += " Contents: " + baos.toString();
                
            } catch (InterruptedException e) {
                result = e.getMessage();
            } catch (ExecutionException e) {
                result = e.getMessage();
            } catch (IOException e) {
                result = e.getMessage();
            }
            
            results.put(responseFutureEntry.getKey(), result);
        }
        return results;
    }

}
