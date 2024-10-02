package com.tracelytics.test.httpclient;

import org.eclipse.jetty.client.api.ContentResponse;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static com.tracelytics.test.httpclient.Target.Method.*;
 
public class TestServlet extends HttpServlet {
    private final Target[] TARGETS = new Target[] {
            new RelativeTarget("test.html", GET),
            new RelativeTarget("test.html", POST),
            new RelativeTarget("test.html", HEAD),
            new RelativeTarget("test.html", DELETE),
            new AbsoluteTarget("https", "www.random.org", null, "/integers/?num=100&min=1&max=6&col=1&base=10&format=plain&rnd=new", GET),
            new AbsoluteTarget("http", "127.0.0.1", 65000, "/fail/", GET),
            new AbsoluteTarget("http", "127.0.0.1", 65000, "/fail/", POST)};


    public void doGet(HttpServletRequest request, HttpServletResponse response) {

        
        try (EndpointClient endpointClient = new EndpointClient()) {

            Map<Target, String> result;
            if ("sync".equals(request.getParameter("type"))) {
                Map<Target, ContentResponse> responses = new LinkedHashMap<>();
                for (Target target : TARGETS) {
                    responses.put(target, endpointClient.execute(target.getMethod(), target.getFullUrl(request)));
                }
                result = buildResultsFromResponses(responses);
            } else {
                Map<Target, Future<EndpointClient.ResultWithContents>> responseFutures = new LinkedHashMap<>();
                for (Target target : TARGETS) {
                    responseFutures.put(target, endpointClient.asyncExecute(target.getMethod(), target.getFullUrl(request)));
                }
                result = buildResultsFromFutures(responseFutures);
            }

            
            request.setAttribute("result", result);
        
            request.getRequestDispatcher("/index.jsp").forward(request, response);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Map<Target, String> buildResultsFromResponses(Map<Target, ContentResponse> responses) {
        Map<Target, String> results = new LinkedHashMap<>();

        for (Entry<Target, ContentResponse> responseFutureEntry : responses.entrySet()) {
            ContentResponse response = responseFutureEntry.getValue();
            String resultString;
            if (response != null) {
                resultString = "Status: " + response.getStatus();
                resultString += " Contents: " + response.getContentAsString();
            } else {
                resultString = "error";
            }

            results.put(responseFutureEntry.getKey(), resultString);
        }
        return results;
    }

    private Map<Target, String> buildResultsFromFutures(Map<Target, Future<EndpointClient.ResultWithContents>> responseFutures) {
        Map<Target, String> results = new LinkedHashMap<Target, String>();
        
        for (Entry<Target, Future<EndpointClient.ResultWithContents>> responseFutureEntry : responseFutures.entrySet()) {
            EndpointClient.ResultWithContents result;
            String resultString;
            try {
                result = responseFutureEntry.getValue().get();
                resultString = "Status: " + result.getResult().getResponse().getStatus();

                resultString += " Contents: " + new String(result.getContents().array());
                
            } catch (InterruptedException e) {
                resultString = e.getMessage();
            } catch (ExecutionException e) {
                resultString = e.getMessage();
            }
            
            results.put(responseFutureEntry.getKey(), resultString);
        }
        return results;
    }

}
