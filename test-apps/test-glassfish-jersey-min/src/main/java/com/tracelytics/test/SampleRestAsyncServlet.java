package com.tracelytics.test;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.ClientFactory;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.appoptics.api.ext.LogMethod;

/**
 * Really old impl using servlet using dispatcher!
 * @author Patson Luk
 * 
 */
public class SampleRestAsyncServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {

        try {
            //Start Rest test
            //Test Glassfish Jax-RS 2.0 compliant Jersey client on local sample server
            testWsRs();

            req.setAttribute("clientAction", "Sample API (REST Async)");
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

                javax.ws.rs.client.Client plainClient = ClientFactory.newClient(); //m01
//        javax.ws.rs.client.Client plainClient = ClientBuilder.newClient(config); //m16

        WebTarget target = plainClient.target("http://localhost:8080/test-rest-server").path("something/{param1}"); //m16
        target = target.resolveTemplate("param1", "dummy"); //m16
        target = target.queryParam("duration", 500); //m16
        
       
        //test asynchronous calls
        Future<Response> future = target.request().async().get(); 
        System.out.println("async " + future.get().getStatus());     

      //test asynchronous calls with failure
        target = target.queryParam("exception", true);
        future = target.request().async().get();
        System.out.println("async " + future.get().getStatus());
    }

}
