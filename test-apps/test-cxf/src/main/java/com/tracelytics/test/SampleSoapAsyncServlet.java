package com.tracelytics.test;

import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamSource;
import javax.xml.ws.Dispatch;
import javax.xml.ws.Response;
import javax.xml.ws.Service;

import org.apache.cxf.endpoint.ClientCallback;
import org.apache.cxf.frontend.ClientProxyFactoryBean;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.jaxws.endpoint.dynamic.JaxWsDynamicClientFactory;

import sample.com.OperatorService;
import sample.com.SampleSoap;

import com.appoptics.api.ext.LogMethod;


/**
 * Tests sample rest server deployed locally, please see README.txt
 * @author Patson Luk
 *
 */
public class SampleSoapAsyncServlet extends HttpServlet {


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {
        
              
        //test sample SOAP Asynchronous calls (own server with x-trace id in response header). Require starting the local SOAP server, please see README.TXT
        try {
            //JAX-WS Dispatch
            testJaxWsDispatcher();
            
            //Dynamic Client
            testJaxWsDynamicClient();
            
            
            req.setAttribute("clientAction", "SAMPLE API (SOAP Async)");
        } catch (Exception e) {
            req.setAttribute("failureMessage", e.getMessage() != null ? e.getMessage() : e.toString());
        }
        
        req.getRequestDispatcher("simple.jsp").forward(req, resp);
    }

  
    
    @LogMethod(layer = "testJaxWsDispatcher")
    private void testJaxWsDispatcher() throws Exception {
        Service service = Service.create(new URL("http://localhost:8888/ws/server?wsdl"), new QName("http://com.sample", "OperatorService"));
        Dispatch<StreamSource> dispatch = service.createDispatch(new QName("http://com.sample", "sample-soapPort"), StreamSource.class, Service.Mode.PAYLOAD);
//        Dispatch<DOMSource> dispatch = service.createDispatch(new QName("http://com.sample", "sample-soapPort"), DOMSource.class, Service.Mode.PAYLOAD);
        
        StreamSource request1 = new StreamSource(new StringReader("<com:getInt xmlns:com=\"http://com.sample\"><arg0>20</arg0></com:getInt>"));
        StreamSource request2 = new StreamSource(new StringReader("<com:getInt xmlns:com=\"http://com.sample\"><arg0>20</arg0></com:getInt>"));
        
//        DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
//        Document requestDoc = db.newDocument();
//        Element root = requestDoc.createElementNS("http://com.sample", "com:getInt");
//        Element arg = requestDoc.createElement("arg0");
//        arg.setNodeValue("20");
//        root.appendChild(arg);
//        requestDoc.appendChild(root);
//        DOMSource request = new DOMSource(requestDoc);
        
        dispatch.getRequestContext().put(Dispatch.SOAPACTION_USE_PROPERTY, new Boolean(true));
        dispatch.getRequestContext().put(Dispatch.SOAPACTION_URI_PROPERTY, "http://com.sample/getInt");
        
          
        List<Response<StreamSource>> asyncResponses = new ArrayList<Response<StreamSource>>();
        
        asyncResponses.add(dispatch.invokeAsync(request1));
        asyncResponses.add(dispatch.invokeAsync(request2));
        
        for (Response<StreamSource> asyncResponse : asyncResponses) {
            System.out.println(asyncResponse.get());
        }
        
    }
  
   
    @LogMethod(layer = "testJaxWsDynamicClient")
    private void testJaxWsDynamicClient() throws MalformedURLException {
        JaxWsDynamicClientFactory dcf = JaxWsDynamicClientFactory.newInstance();
        org.apache.cxf.endpoint.Client dynamicClient = dcf.createClient(new URL("http://localhost:8888/ws/server?wsdl"));
        
        try {
            ClientCallback callback1 = new ClientCallback();
            dynamicClient.invoke(callback1, "getInt", 30);
            ClientCallback callback2 = new ClientCallback();
            dynamicClient.invoke(callback2, "getInt", 30);
            System.out.println("Echo response (async): " + callback1.get()[0]);
            System.out.println("Echo response (async): " + callback2.get()[0]);
        } catch (Exception e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
    }
    
}
