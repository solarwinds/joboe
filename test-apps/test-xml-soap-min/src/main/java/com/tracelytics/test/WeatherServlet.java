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
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.lang.StringEscapeUtils;

import com.appoptics.api.ext.LogMethod;


/**
 * Really old impl using servlet using dispatcher!
 * @author Patson Luk
 *
 */
public class WeatherServlet extends HttpServlet {


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {
        
        String zip = req.getParameter("zip");
        
        //Plain Soap
        req.setAttribute("response", testPlainSoap(zip));
        
        req.getRequestDispatcher("index.jsp").forward(req, resp);
    }

   
    @LogMethod(layer = "testPlainSoap")
    private String testPlainSoap(String zip)
            throws TransformerFactoryConfigurationError {
        try {
            SOAPConnectionFactory soapConnectionFactory = SOAPConnectionFactory.newInstance();
            SOAPConnection soapConnection = soapConnectionFactory.createConnection();
            
            SOAPMessage soapRequest = MessageFactory.newInstance().createMessage();
            
//            soapMessage.getSOAPPart().getEnvelope().setAttribute("xmlns:soap", "")
            SOAPBody body = soapRequest.getSOAPBody();
            SOAPBodyElement bodyElement = body.addBodyElement(new QName("http://ws.cdyne.com/WeatherWS/", "GetCityForecastByZIP"));
            SOAPElement zipElement = bodyElement.addChildElement("ZIP");
            zipElement.setTextContent(zip);
            
            SOAPMessage soapResponse = soapConnection.call(soapRequest, "http://wsf.cdyne.com/WeatherWS/Weather.asmx");
            
            TransformerFactory transFactory = TransformerFactory.newInstance();
            Transformer transformer = transFactory.newTransformer();
            StringWriter buffer = new StringWriter();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.transform(new DOMSource(soapResponse.getSOAPBody()),
                  new StreamResult(buffer));
            return StringEscapeUtils.escapeXml(buffer.toString());
            
            
        } catch (SOAPException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        } catch (TransformerException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        return null;
    }



    
    
}
