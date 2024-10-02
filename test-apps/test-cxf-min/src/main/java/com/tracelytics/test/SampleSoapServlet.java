package com.tracelytics.test;

import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamSource;
import javax.xml.ws.Dispatch;
import javax.xml.ws.Service;

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
public class SampleSoapServlet extends HttpServlet {


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {
        
              
        //test sample SOAP (own server with x-trace id in response header). Require starting the local SOAP server, please see README.TXT
        try {
            testWsdl2Java(); 
           
          //JAX-WS Proxy
            testJaxWsProxy();
            
            //JAX-WS Dispatch
            testJaxWsDispatcher();
            
            //Simple Frontend client proxy (does not work 1.2), test on Weather servlet for this
//            testSimpleFrontendClient();
            
            //Proxy Factory Bean
            testProxyFactoryBean();
            
            //Dynamic Client
            testJaxWsDynamicClient();
            
            
            req.setAttribute("clientAction", "SAMPLE API (SOAP)");
        } catch (Exception e) {
            req.setAttribute("failureMessage", e.getMessage() != null ? e.getMessage() : e.toString());
        }
        
        req.getRequestDispatcher("simple.jsp").forward(req, resp);
    }

    @LogMethod(layer = "testWsdl2Java")
    private void testWsdl2Java() {
        System.out.println(new OperatorService().getSampleSoapPort().getInt(5));
    }
    
    @LogMethod(layer = "testJaxWsProxy")
    private void testJaxWsProxy() throws MalformedURLException {
        Service service = Service.create(new URL("http://localhost:8888/ws/server?wsdl"), new QName("http://com.sample", "OperatorService"));
        System.out.println(service.getPort(SampleSoap.class).getInt(10));
        
    }
    
    @LogMethod(layer = "testJaxWsDispatcher")
    private void testJaxWsDispatcher() throws Exception {
        Service service = Service.create(new URL("http://localhost:8888/ws/server?wsdl"), new QName("http://com.sample", "OperatorService"));
        Dispatch<StreamSource> dispatch = service.createDispatch(new QName("http://com.sample", "sample-soapPort"), StreamSource.class, Service.Mode.PAYLOAD);
//        Dispatch<DOMSource> dispatch = service.createDispatch(new QName("http://com.sample", "sample-soapPort"), DOMSource.class, Service.Mode.PAYLOAD);
        
        StreamSource request = new StreamSource(new StringReader("<com:getInt xmlns:com=\"http://com.sample\"><arg0>20</arg0></com:getInt>"));
        
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
        
        
        // Dispatch disp created previously
        Object response = dispatch.invoke(request);
        System.out.println(response);
        
    }
    
    @LogMethod(layer = "testSimpleFrontendClient")
    private void testSimpleFrontendClient() {
        ClientProxyFactoryBean simpleFrontendFactory = new ClientProxyFactoryBean();
        
        simpleFrontendFactory.setServiceClass(SampleSoap.class);
        simpleFrontendFactory.setAddress("http://localhost:8888/ws/server");
        
        try {
            System.out.println("Simple FE: " + ((SampleSoap)simpleFrontendFactory.create()).getInt(25));
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
        
    }
    
    @LogMethod(layer = "testProxyFactoryBean")
    private void testProxyFactoryBean() {
        JaxWsProxyFactoryBean proxyFactoryBean = new JaxWsProxyFactoryBean();
        
        proxyFactoryBean.setServiceClass(SampleSoap.class);
        proxyFactoryBean.setAddress("http://localhost:8888/ws/server");
        
        try {
            System.out.println("proxy Factory Bean: " + ((SampleSoap)proxyFactoryBean.create()).getInt(25));
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
        
    }

    @LogMethod(layer = "testJaxWsDynamicClient")
    private void testJaxWsDynamicClient() throws MalformedURLException {
        JaxWsDynamicClientFactory dcf = JaxWsDynamicClientFactory.newInstance();
        org.apache.cxf.endpoint.Client dynamicClient = dcf.createClient(new URL("http://localhost:8888/ws/server?wsdl"));
    
        Object[] res;
        try {
            res = dynamicClient.invoke("getInt", 30);
            System.out.println("Echo response: " + res[0]);
        } catch (Exception e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
    }
    
}
