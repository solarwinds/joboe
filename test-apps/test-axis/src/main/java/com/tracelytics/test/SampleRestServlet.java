package com.tracelytics.test;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.axis2.transport.http.HTTPConstants;


/**
 * Tests sample rest server deployed locally, please see README.txt
 * @author Patson Luk
 *
 */
public class SampleRestServlet extends HttpServlet {


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {
        
        try {
            testLocalRest();
                   
            req.setAttribute("clientAction", "SAMPLE API (REST)");
        } catch (Exception e) {
            req.setAttribute("failureMessage", e.getMessage() != null ? e.getMessage() : e.toString());
            e.printStackTrace();
        }
        
        req.getRequestDispatcher("simple.jsp").forward(req, resp);
    }

    private void testLocalRest() throws Exception {
        ConfigurationContext context = ConfigurationContextFactory.createConfigurationContextFromFileSystem(null, null);
        context.getAxisConfiguration().addMessageFormatter("application/json", new org.apache.axis2.json.JSONMessageFormatter());
        context.getAxisConfiguration().addMessageBuilder("application/json", new org.apache.axis2.json.JSONOMBuilder());
        
        ServiceClient restSender = new ServiceClient(context, null);
//        ServiceClient restSender = new ServiceClient(context, null);
//        restSender.engageModule(org.apache.axis2.Constants.MODULE_ADDRESSING);
        
        Options options = new Options();
        restSender.setOptions(options);
        
        options.setTo(new EndpointReference("http://localhost:8080/test-rest-server/something/test?duration=2000"));
        options.setProperty(org.apache.axis2.Constants.Configuration.ENABLE_REST, org.apache.axis2.Constants.VALUE_TRUE);
        options.setProperty(org.apache.axis2.Constants.Configuration.MESSAGE_TYPE, "application/json");
        options.setTransportInProtocol(Constants.TRANSPORT_HTTP);
        
        //try Get
        options.setProperty(org.apache.axis2.Constants.Configuration.HTTP_METHOD, HTTPConstants.HTTP_METHOD_GET);
        System.out.println(restSender.sendReceive(null));
        
        //try POST, cxf rest only support GET and POST
//        options.setProperty(org.apache.axis2.Constants.Configuration.HTTP_METHOD, HTTPConstants.HTTP_METHOD_POST);
//        
//        System.out.println(restSender.sendReceive(createEnvelope()));
    }
}
