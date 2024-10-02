package com.tracelytics.test;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import org.codehaus.jackson.jaxrs.JacksonJsonProvider;

import com.appoptics.api.ext.LogMethod;

/**
 * Really old impl using servlet using dispatcher!
 * @author Patson Luk
 * 
 */
public class SampleRestServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {

        try {
            //Start Rest test
            //Test Glassfish Jax-RS 2.0 compliant Jersey client on local sample server
            testWsRs();

            req.setAttribute("clientAction", "Sample API (REST)");
        } catch (Exception e) {
            req.setAttribute("failureMessage", e.getMessage() != null ? e.getMessage() : e.toString());
        }

        req.getRequestDispatcher("simple.jsp").forward(req, resp);
    }

  
    @LogMethod(layer = "testWsRs")
    private void testWsRs() throws InterruptedException, ExecutionException {
        //JAVAX.WS.RS client, which by default would use Glassfish jersey under the hood, but cannot locate in maven:
        //available in        
        //      <groupId>javax.ws.rs</groupId>
        //      <artifactId>javax.ws.rs-api</artifactId>
        //      <version>2.0-m16</version>

        //        javax.ws.rs.client.Client plainClient = ClientFactory.newClient(); //m10
        org.glassfish.jersey.client.ClientConfig config = new org.glassfish.jersey.client.ClientConfig().register(JacksonJsonProvider.class);
        javax.ws.rs.client.Client plainClient = ClientBuilder.newClient(config); //m16

        WebTarget target = plainClient.target("http://localhost:8080/test-rest-server").path("something/{param1}");
        target = target.resolveTemplate("param1", "dummy"); //m16
        target = target.queryParam("duration", 500);

        SampleResultContainer container;

        container = target.request(MediaType.APPLICATION_JSON).get(SampleResultContainer.class);
        System.out.println(container.result.method);

        container = target.request(MediaType.APPLICATION_JSON).delete(SampleResultContainer.class);
        System.out.println(container.result.method);

        container = target.request(MediaType.APPLICATION_JSON).post(Entity.text("dummy"), SampleResultContainer.class);
        System.out.println(container.result.method);

        container = target.request(MediaType.APPLICATION_JSON).put(Entity.text("dummy"), SampleResultContainer.class);
        System.out.println(container.result.method);
    }

}
