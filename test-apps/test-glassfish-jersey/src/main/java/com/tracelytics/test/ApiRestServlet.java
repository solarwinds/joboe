package com.tracelytics.test;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.codehaus.jackson.jaxrs.JacksonJsonProvider;
import org.glassfish.jersey.client.JerseyClient;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.glassfish.jersey.client.JerseyWebTarget;

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
            //Test glassfish Jersey JAX-rs 2.0 compliant client on TraceView public API
            testWsRs();
            testGlassfishJersey();
            
            req.setAttribute("clientAction", "TraceView API (REST)");
        } catch (Exception e) {
            req.setAttribute("failureMessage", e.getMessage() != null ? e.getMessage() : e.toString());
        }
        
        req.getRequestDispatcher("simple.jsp").forward(req, resp);
    }


    @LogMethod(layer = "testWsRs")
    private void testWsRs() {
        //JAVAX.WS.RS client, which by default would use Glassfish jersey under the hood, but cannot locate in maven:
      //available in        
//      <groupId>javax.ws.rs</groupId>
//      <artifactId>javax.ws.rs-api</artifactId>
//      <version>2.0-m16</version>
  
//        javax.ws.rs.client.Client plainClient = ClientFactory.newClient(); //m10
        javax.ws.rs.client.Client plainClient = ClientBuilder.newClient(); //m16
         
        WebTarget target = plainClient.target("https://api.tracelytics.com/api-v1").path("latency/{app}/server/summary");
        target = target.resolveTemplate("app", "Default"); //m16
        
//        target.pathParam("app", "Default"); //m10
        
        target = target.queryParam("key", "9de3ed4c-5e33-4e26-b0b7-82c20ea01532");
        
        Response dataPlain = target.request(MediaType.APPLICATION_JSON).buildGet().invoke();
        System.out.println(dataPlain.getEntity());
        
        dataPlain = target.request(MediaType.APPLICATION_JSON).get();
        System.out.println(dataPlain.getEntity());
        
        
    }
    
    @LogMethod(layer = "testGlassfishJersey")
    private Data testGlassfishJersey() {
        //REST JERSEY API test
        JerseyClient client = new JerseyClientBuilder().build();

        client.getConfiguration().register(JacksonJsonProvider.class);

        JerseyWebTarget target = client.target("https://api.tracelytics.com/api-v1").path("latency/Default/server/summary").queryParam("key", "9de3ed4c-5e33-4e26-b0b7-82c20ea01532");

        Data data = target.request(MediaType.APPLICATION_JSON_TYPE).get(DataContainer.class).data;

        System.out.println(data.average);
        System.out.println(data.count);
        System.out.println(data.latest);
        return data;
    }
    
    

}

