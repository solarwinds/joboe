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
            testSampleRest();

            req.setAttribute("clientAction", "SAMPLE API (REST Async)");
        } catch (Exception e) {
            req.setAttribute("failureMessage", e.getMessage() != null ? e.getMessage() : e.toString());
        }

        req.getRequestDispatcher("simple.jsp").forward(req, resp);
    }

    @LogMethod(layer = "testSampleRest")
    private void testSampleRest() {
        //REST JERSEY API test
        ClientConfig clientConfig = new DefaultClientConfig();
        //clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, true);
        clientConfig.getFeatures().put("com.sun.jersey.api.json.POJOMappingFeature", true);
        
        Client client = ApacheHttpClient.create(clientConfig); //try Apache Http Client

        try {
            //test asynchronous call
            AsyncWebResource asyncWebResource = client.asyncResource("http://localhost:8080/test-rest-server");
            Future<ClientResponse> asyncResponse;
            asyncResponse = asyncWebResource.path("test").queryParam("duration", "400").accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
            System.out.println(asyncResponse.get().getEntity(SampleResultContainer.class).result.path);
            asyncResponse = asyncWebResource.path("test").queryParam("duration", "300").accept(MediaType.APPLICATION_JSON).delete(ClientResponse.class);
            System.out.println(asyncResponse.get().getEntity(SampleResultContainer.class).result.path);
        } catch (ClientHandlerException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (UniformInterfaceException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ExecutionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }
}
