package com.tracelytics.test;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import org.jboss.resteasy.client.ProxyFactory;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.plugins.providers.RegisterBuiltin;
import org.jboss.resteasy.spi.ResteasyProviderFactory;



/**
 * Really old impl using servlet using dispatcher!
 * @author Patson Luk
 *
 */
public class ApiRestServlet extends HttpServlet {
    static {
        RegisterBuiltin.register(ResteasyProviderFactory.getInstance());
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {
        try {
          
            Data data;
          
          //Proxy of RESTEasy (deprecated in RestEasy 3)
            data = testOldProxyClient();
            
            data = testJaxRsProxyClient();
            data = testResteasyProxyClient();
             
            req.setAttribute("clientAction", "SAMPLE API (REST)");
        } catch (Exception e) {
            req.setAttribute("failureMessage", e.getMessage() != null ? e.getMessage() : e.toString());
        }

        req.getRequestDispatcher("simple.jsp").forward(req, resp);
    }

    /**
     * Test on the TV public API
     * @return
     */
    private Data testOldProxyClient() {
        Data data;
        TracelyticsClient client = ProxyFactory.create(TracelyticsClient.class, "https://api.tracelytics.com/api-v1");
        
        data = client.getResult("Default", "9de3ed4c-5e33-4e26-b0b7-82c20ea01532").data;
        
        System.out.println(data.average);
        System.out.println(data.count);
        System.out.println(data.latest);
        return data;
    }
    /**
     * New in resteasy 3.0
     * @return
     */
    private Data testJaxRsProxyClient() {
        Data data;
        Client client = ClientBuilder.newClient();
        ResteasyWebTarget target = (ResteasyWebTarget) client.target("https://api.tracelytics.com/api-v1");
        
        TracelyticsClient tracelyticsClient = target.proxy(TracelyticsClient.class);
        
        data = tracelyticsClient.getResult("Default", "9de3ed4c-5e33-4e26-b0b7-82c20ea01532").data;
        
        System.out.println(data.average);
        System.out.println(data.count);
        System.out.println(data.latest);
        return data;
    }
    
    /**
     * New in resteasy 3.0
     * @return
     */
    private Data testResteasyProxyClient() {
        ResteasyClient client = new ResteasyClientBuilder().build();
        ResteasyWebTarget target = client.target("https://api.tracelytics.com/api-v1");
        
        TracelyticsClient tracelyticsClient = target.proxy(TracelyticsClient.class);
        
        Data data = tracelyticsClient.getResult("Default", "9de3ed4c-5e33-4e26-b0b7-82c20ea01532").data;
        
        System.out.println(data.average);
        System.out.println(data.count);
        System.out.println(data.latest);
        return data;
    }

    

}