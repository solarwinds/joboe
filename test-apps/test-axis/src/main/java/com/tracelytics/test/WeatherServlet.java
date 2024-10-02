package com.tracelytics.test;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Arrays;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.axis2.AxisFault;

import com.cdyne.axis2.ws.adb.WeatherStub;
import com.cdyne.axis2.ws.adb.WeatherStub.ForecastReturn;
import com.cdyne.axis2.ws.adb.WeatherStub.GetCityForecastByZIP;
import com.cdyne.ws.weatherws.WeatherReturn;


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
        if (zip == null) {
            req.getRequestDispatcher("index.jsp").forward(req, resp);
            return;
        }
        
        //WSDL2Java 
        testWeatherWsdl2Java(req, zip);
        
        //XMLBeans
        testWeatherXmlBeans(zip);
        
        req.getRequestDispatcher("index.jsp").forward(req, resp);
    }

    
    
    
    
    protected OMElement createEnvelope() {
        OMFactory fac = OMAbstractFactory.getOMFactory();
        OMNamespace omNs = fac.createOMNamespace("", "");
        OMElement rpcWrapEle = fac.createOMElement("echoOM", omNs);
        OMElement data = fac.createOMElement("data", omNs);
        OMElement data1 = fac.createOMElement("data", omNs);
        String expectedString = "my json string";
        String expectedString1 = "my second json string";
        data.setText(expectedString);
        data1.setText(expectedString1);
        rpcWrapEle.addChild(data);
        rpcWrapEle.addChild(data1);
        return rpcWrapEle;
    }

    private void testWeatherXmlBeans(String zip)
        throws AxisFault, RemoteException {
        com.cdyne.ws.WeatherStub beanStub = new com.cdyne.ws.WeatherStub();
        com.cdyne.ws.weatherws.GetCityWeatherByZIPDocument beanRequest = com.cdyne.ws.weatherws.GetCityWeatherByZIPDocument.Factory.newInstance();
        beanRequest.addNewGetCityWeatherByZIP().setZIP(zip);
        WeatherReturn beanResult = beanStub.getCityWeatherByZIP(beanRequest).getGetCityWeatherByZIPResponse().getGetCityWeatherByZIPResult();
        
        if (beanResult != null && beanResult.getSuccess()) {
            System.out.println(beanResult.getCity());
        }
    }

    private void testWeatherWsdl2Java(HttpServletRequest req, String zip)
        throws AxisFault, RemoteException {
        WeatherStub stub = new WeatherStub();
        GetCityForecastByZIP request = new GetCityForecastByZIP();
        request.setZIP(zip);
        ForecastReturn result = stub.getCityForecastByZIP(request).getGetCityForecastByZIPResult();
        
        if (result != null && result.getSuccess()) {
            req.setAttribute("city", result.getCity());
            req.setAttribute("forecasts", Arrays.asList(result.getForecastResult().getForecast()));
        }
    }
    
    

}
