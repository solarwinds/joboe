package com.tracelytics.test;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;

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

    @LogMethod(layer = "testSampleRest")
    private void testSampleRest() {
        //REST JERSEY API test
        ClientConfig clientConfig = new DefaultClientConfig();
        clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, true);
        Client client = ApacheHttpClient.create(clientConfig); //try Apache Http Client

        try {
            //test synchronous call
            WebResource webResource = client.resource("http://localhost:8080/test-rest-server");
            ClientResponse response;
            response = webResource.path("test").queryParam("duration", "500").accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
            System.out.println(response.getEntity(SampleResultContainer.class).result.path);
            response = webResource.path("test").queryParam("duration", "600").accept(MediaType.APPLICATION_JSON).delete(ClientResponse.class);
            System.out.println(response.getEntity(SampleResultContainer.class).result.path);

           
        } catch (ClientHandlerException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (UniformInterfaceException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }
}
