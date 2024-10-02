package com.tracelytics.test;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebServiceException;

import sample.com.OperatorService;
import sample.com.SampleSoap;

import com.appoptics.api.ext.LogMethod;


/**
 * Tests sample rest server deployed locally, please see README.txt
 * @author Patson Luk
 *
 */
public class SampleSoapServlet extends HttpServlet {


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {
        
              
        //test sample SOAP (own server with x-trace id in response header). Require starting the local SOAP server, please see README.TXT
        try {
            testWsdl2Java(); 
            
            testJaxWsProxy();
           
            req.setAttribute("clientAction", "Sample API (SOAP)");
            
        } catch (WebServiceException e) {
            req.setAttribute("failureMessage", e.getMessage());
        }
        
        req.getRequestDispatcher("sample.jsp").forward(req, resp);
    }

    @LogMethod(layer = "testWsdl2Java")
    private void testWsdl2Java() throws WebServiceException {
        System.out.println(new OperatorService().getSampleSoapPort().getInt(5));
    }
    
    @LogMethod(layer = "testJaxWsProxy")
    private void testJaxWsProxy() throws MalformedURLException {
        Service service = Service.create(new URL("http://localhost:8888/ws/server?wsdl"), new QName("http://com.sample", "OperatorService"));
        System.out.println(service.getPort(SampleSoap.class).getInt(15));
    }
    
}
