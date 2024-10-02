package com.tracelytics.test;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;

import com.sun.jersey.api.client.AsyncWebResource;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.client.apache.ApacheHttpClient;
import com.appoptics.api.ext.LogMethod;


/**
 * Really old impl using servlet using dispatcher!
 * @author Patson Luk
 * 
 */
public class ApiRestServlet extends HttpServlet {


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {
       
        try {
            //Start Rest test
            //REST Jersey client
            testTraceViewApi();
            
            req.setAttribute("clientAction", "TraceView API (REST)");
        } catch (Exception e) {
            req.setAttribute("failureMessage", e.getMessage() != null ? e.getMessage() : e.toString());
        }
        req.getRequestDispatcher("simple.jsp").forward(req, resp);
    }

   
    @LogMethod(layer = "testTraceViewApi")
    private Data testTraceViewApi() {
        //REST JERSEY API test
        ClientConfig clientConfig = new DefaultClientConfig();
        clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, true);
        
        //Client client = ApacheHttpClient.create(clientConfig);
        Client client = Client.create(clientConfig); //try HttpURLConnection client
        
        WebResource webResource = client.resource("https://api.tracelytics.com/api-v1");
        
        ClientResponse response = webResource.path("latency/Default/server/summary").queryParam("key", "9de3ed4c-5e33-4e26-b0b7-82c20ea01532").accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
        
        Data data = response.getEntity(DataContainer.class).data;
//                
        System.out.println(data.average);
        System.out.println(data.count);
        System.out.println(data.latest);
        return data;
    }

}

