package com.tracelytics.test;

import java.io.IOException;
import java.rmi.RemoteException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.axis2.AxisFault;

import sample.com.GetIntDocument;
import sample.com.GetIntResponse;
import sample.com.OperatorServiceStub;
import sample.com.OperatorServiceStub.GetInt;
import sample.com.OperatorServiceStub.GetIntE;

import com.cdyne.ws.weatherws.WeatherReturn;
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
            //Wsdl2Java
            testLocalWsdl2Java();

            //XMLBeans
            testLocalXmlBeans();

            req.setAttribute("clientAction", "SAMPLE API (SOAP)");
        } catch (Exception e) {
            req.setAttribute("failureMessage", e.getMessage() != null ? e.getMessage() : e.toString());
        }

        req.getRequestDispatcher("simple.jsp").forward(req, resp);
    }

    @LogMethod(layer = "testSampleSoapWsdl2Java")
    private void testLocalWsdl2Java()
        throws AxisFault, RemoteException {
        OperatorServiceStub stub = new OperatorServiceStub();

        GetIntE requestContainer = new GetIntE();
        GetInt request = new GetInt();
        request.setArg0(20);
        requestContainer.setGetInt(request);

        System.out.println(stub.getInt(requestContainer).getGetIntResponse().get_return());
    }
    
    @LogMethod(layer = "testSampleSoapXmlBeans")
    private void testLocalXmlBeans()
            throws AxisFault, RemoteException {
        sample.com.xmlbeans.OperatorServiceStub stub = new sample.com.xmlbeans.OperatorServiceStub();
        
        
        GetIntDocument document = GetIntDocument.Factory.newInstance();
        sample.com.GetInt beanRequest = sample.com.GetInt.Factory.newInstance();
        
        beanRequest.setArg0(15);
        document.setGetInt(beanRequest);
        
        GetIntResponse response = stub.getInt(document).getGetIntResponse();
            
        System.out.println(response.getReturn());
    }

}
