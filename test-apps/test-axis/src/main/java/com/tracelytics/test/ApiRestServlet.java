package com.tracelytics.test;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.axiom.om.OMElement;
import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.axis2.transport.http.HTTPConstants;


/**
 * Test on TV API rest
 * @author Patson Luk
 *
 */
public class ApiRestServlet extends HttpServlet {


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {
        
        try {
            //Start Rest test
            testTracelyticsRest();
            
            req.setAttribute("clientAction", "TraceView API (REST)");
        } catch (Exception e) {
            req.setAttribute("failureMessage", e.getMessage() != null ? e.getMessage() : e.toString());
        }
        req.getRequestDispatcher("simple.jsp").forward(req, resp);
    }

  
    private void testTracelyticsRest() throws AxisFault {
        ConfigurationContext context = ConfigurationContextFactory.createConfigurationContextFromFileSystem(null, null);
        context.getAxisConfiguration().addMessageFormatter("application/json", new org.apache.axis2.json.JSONMessageFormatter());
        context.getAxisConfiguration().addMessageBuilder("application/json", new org.apache.axis2.json.JSONOMBuilder());
        
        ServiceClient restSender = new ServiceClient(context, null);
        
        Options options = new Options();
        options.setTo(new EndpointReference("https://api.tracelytics.com/api-v1/latency/Default/server/summary?key=9de3ed4c-5e33-4e26-b0b7-82c20ea01532"));
        
        System.out.println(options.getProperty(org.apache.axis2.Constants.Configuration.HTTP_METHOD));
        
        options.setProperty(org.apache.axis2.Constants.Configuration.HTTP_METHOD, HTTPConstants.HTTP_METHOD_GET);
        options.setProperty(org.apache.axis2.Constants.Configuration.ENABLE_REST, org.apache.axis2.Constants.VALUE_TRUE);
        options.setProperty(org.apache.axis2.Constants.Configuration.MESSAGE_TYPE, "application/json");
        restSender.setOptions(options);
        
//        System.out.println(options.getProperty(org.apache.axis2.Constants.Configuration.ENABLE_REST).getClass().getName());
        
        OMElement element = restSender.sendReceive(null);
        
        System.out.println(element);
    }
    
}
