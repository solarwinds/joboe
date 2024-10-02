package com.tracelytics.test;

import java.io.IOException;
import java.io.StringWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPBodyElement;
import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPConnectionFactory;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

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

            req.setAttribute("clientAction", "SAMPLE API (SOAP)");
        } catch (Exception e) {
            req.setAttribute("failureMessage", e.getMessage() != null ? e.getMessage() : e.toString());
        }

        req.getRequestDispatcher("simple.jsp").forward(req, resp);
    }

    @LogMethod(layer = "testSampleSoap")
    private void testSampleSoap() throws SOAPException, TransformerException {
        TransformerFactory transFactory = TransformerFactory.newInstance();
        Transformer transformer = transFactory.newTransformer();

        SOAPConnectionFactory soapConnectionFactory = SOAPConnectionFactory.newInstance();
        SOAPConnection soapConnection = soapConnectionFactory.createConnection();

        SOAPMessage soapRequest = MessageFactory.newInstance().createMessage();

        soapRequest.getSOAPPart().getEnvelope().setAttribute("xmlns:com", "http://com.sample");

        SOAPBody body = soapRequest.getSOAPBody();
        SOAPBodyElement bodyElement = body.addBodyElement(new QName("http://com.sample", "getInt", "com"));
        SOAPElement intElement = bodyElement.addChildElement("arg0");
        intElement.setTextContent("5");

        StringWriter buffer = new StringWriter();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.transform(new DOMSource(soapRequest.getSOAPBody()), new StreamResult(buffer));
        System.out.println(buffer.toString());

        SOAPMessage soapResponse = soapConnection.call(soapRequest, "http://localhost:8888/ws/server");
        System.out.println(soapResponse.getSOAPBody().getFault());
        soapResponse.getSOAPBody();

        buffer = new StringWriter();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.transform(new DOMSource(soapResponse.getSOAPBody()),
                              new StreamResult(buffer));
        System.out.println(buffer.toString());
    }

}
