package com.tracelytics.test;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.ws.WebServiceException;

import sample.com.OperatorService;

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
            testSampleSoap(); 
           
            req.setAttribute("clientAction", "Sample API (SOAP)");
            
        } catch (WebServiceException e) {
            req.setAttribute("failureMessage", e.getMessage());
        }
        
        req.getRequestDispatcher("sample.jsp").forward(req, resp);
    }

    @LogMethod(layer = "testSampleSoap")
    private void testSampleSoap() throws WebServiceException {
        System.out.println(new OperatorService().getSampleSoapPort().getInt(5));
    }
    
}
