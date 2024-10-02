package com.tracelytics.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Request;
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
import javax.xml.transform.stream.StreamSource;
import javax.xml.ws.Dispatch;
import javax.xml.ws.Service;

import org.apache.cxf.frontend.ClientProxyFactoryBean;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.provider.json.JSONProvider;
import org.apache.cxf.jaxws.endpoint.dynamic.JaxWsDynamicClientFactory;
import org.apache.cxf.message.Message;

import sample.com.OperatorService;

import com.cdyne.cxf.ws.weatherws.ForecastReturn;
import com.cdyne.cxf.ws.weatherws.Weather;
import com.cdyne.cxf.ws.weatherws.WeatherReturn;
import com.cdyne.cxf.ws.weatherws.WeatherSoap;
import com.appoptics.api.ext.LogMethod;
import com.tracelyticstest.rs.Data;
import com.tracelyticstest.rs.SampleResult;
import com.tracelyticstest.rs.SampleServerClient;
import com.tracelyticstest.rs.TracelyticsClient;


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
            //REST CXF proxy
            testRestProxy();
            
            //REST CXF WebClient API
            testRestWebClient();
            
            req.setAttribute("clientAction", "TraceView API (REST)");
        } catch (Exception e) {
            req.setAttribute("failureMessage", e.getMessage() != null ? e.getMessage() : e.toString());
        }
        req.getRequestDispatcher("simple.jsp").forward(req, resp);
    }

  
    @LogMethod(layer = "testRestProxy")
    private void testRestProxy() {
        //REST Proxy-based API test
        TracelyticsClient client = JAXRSClientFactory.create("https://api.tracelytics.com/api-v1", TracelyticsClient.class, Collections.singletonList(new JSONProvider()));
        Data data = client.getResult("Default", "9de3ed4c-5e33-4e26-b0b7-82c20ea01532");
        System.out.println(data.average);
        System.out.println(data.count);
        System.out.println(data.latest);
    }

    @LogMethod(layer = "testRestWebClient")
    private void testRestWebClient() {
        WebClient webClient = WebClient.create("https://api.tracelytics.com/api-v1");
        webClient = webClient.path("latency/{app}/server/summary", "Default");
        webClient = webClient.query("key", "9de3ed4c-5e33-4e26-b0b7-82c20ea01532");
        webClient = webClient.accept("application/json");
        Data data = webClient.get(Data.class);
        
        System.out.println(data.average);
        System.out.println(data.count);
        System.out.println(data.latest);
        
        data = webClient.get(new GenericType<Data>(Data.class));
        System.out.println(data.average);
        System.out.println(data.count);
        System.out.println(data.latest);
    }
    
}
