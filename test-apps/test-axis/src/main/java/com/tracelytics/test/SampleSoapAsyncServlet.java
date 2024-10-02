package com.tracelytics.test;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.client.async.AxisCallback;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.databinding.utils.ConverterUtil;

/**
 * Tests sample rest server deployed locally, please see README.txt
 * @author Patson Luk
 *
 */
public class SampleSoapAsyncServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {

        //test sample SOAP (own server with x-trace id in response header). Require starting the local SOAP server, please see README.TXT
        try {

            ServiceClient client = new ServiceClient();
            Options opts = new Options();
            opts.setTo(new EndpointReference("http://localhost:8888/ws/server"));
            opts.setAction("urn:getInt");
            client.setOptions(opts);
            client.sendReceiveNonBlocking(createPayLoad(), (AxisCallback)null);
            client.sendReceiveNonBlocking(createPayLoad(), new TestCallback());
            
            Thread.sleep(2000);
            
            req.setAttribute("clientAction", "SAMPLE API (SOAP async)");
        } catch (Exception e) {
            req.setAttribute("failureMessage", e.getMessage() != null ? e.getMessage() : e.toString());
        }

        req.getRequestDispatcher("simple.jsp").forward(req, resp);
    }

    private static OMElement createPayLoad() {
        OMFactory fac = OMAbstractFactory.getOMFactory();
        OMNamespace omNs = fac.createOMNamespace(
                                                 "http://com.sample", "com");
        OMElement method = fac.createOMElement("getInt", omNs);
        OMElement value = fac.createOMElement("arg0", null);
        method.addChild(value);
        value.setText(ConverterUtil.convertToString("25"));
        return method;
    }
    
    private class TestCallback implements AxisCallback {

        @Override
        public void onMessage(MessageContext msgContext) {
            System.out.println(msgContext.getEnvelope().getBody());
            
        }

        @Override
        public void onFault(MessageContext msgContext) {
            System.out.println("FAULT " + msgContext.getEnvelope().getBody());
            
        }

        @Override
        public void onError(Exception e) {
            System.out.println("ERROR " + e.getMessage());
            
        }

        @Override
        public void onComplete() {
            System.out.println("COMPLETE");
            
        }
        
    }

   

}
