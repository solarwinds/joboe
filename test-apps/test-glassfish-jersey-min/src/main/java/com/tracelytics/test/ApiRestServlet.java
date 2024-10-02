package com.tracelytics.test;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.ClientFactory;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.SslConfig;

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
            
            req.setAttribute("clientAction", "TraceView API (REST)");
        } catch (Exception e) {
            req.setAttribute("failureMessage", e.getMessage() != null ? e.getMessage() : e.toString());
            e.printStackTrace();
        }
        
        req.getRequestDispatcher("simple.jsp").forward(req, resp);
    }


    @LogMethod(layer = "testWsRs")
    private void testWsRs() throws NoSuchAlgorithmException {
        //JAVAX.WS.RS client, which by default would use Glassfish jersey under the hood,
  
        javax.ws.rs.client.Client plainClient = ClientFactory.newClient(); //m08
//        javax.ws.rs.client.Client plainClient = ClientBuilder.newClient(); //m16
        
        plainClient.configuration().setProperty(ClientProperties.SSL_CONFIG, new SslConfig(null, SSLContext.getDefault()));
        
        WebTarget target = plainClient.target("https://api.tracelytics.com/api-v1").path("latency/{app}/server/summary"); 
        target = target.resolveTemplate("app", "Default").queryParam("key", "9de3ed4c-5e33-4e26-b0b7-82c20ea01532"); 

        
        Response dataPlain = target.request(MediaType.APPLICATION_JSON).buildGet().invoke();
        System.out.println(dataPlain.getEntity());
        
        dataPlain = target.request(MediaType.APPLICATION_JSON).get();
        System.out.println(dataPlain.getEntity());
        
        
    }

}

