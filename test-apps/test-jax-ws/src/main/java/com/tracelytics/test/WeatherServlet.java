package com.tracelytics.test;

import java.io.IOException;
import java.net.MalformedURLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;

import com.cdyne.ws.weatherws.ForecastReturn;
import com.cdyne.ws.weatherws.Weather;
import com.cdyne.ws.weatherws.WeatherSoap;
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
            
        
        
      //WSDL2Java
        String zip = req.getParameter("zip");
        
        if (zip == null) {
            req.getRequestDispatcher("index.jsp").forward(req, resp);
            return;
        }
        
        testWsdl2Java(req, zip);
        
        
        //JAX-WS Proxy
        testJaxWsProxy(zip);
        
        req.getRequestDispatcher("index.jsp").forward(req, resp);
    }

    @LogMethod(layer = "testWsdl2Java")
    private void testWsdl2Java(HttpServletRequest req, String zip) {
        if (zip != null) {
            WeatherSoap soap = new Weather().getWeatherSoap();
            ForecastReturn result = soap.getCityForecastByZIP(zip);
            if (result != null && result.isSuccess()) {
                req.setAttribute("city", result.getCity());
                req.setAttribute("forecasts", result.getForecastResult().getForecast());
            }
        }
    }

    @LogMethod(layer = "testJaxWsProxy")
    private void testJaxWsProxy(String zip) throws MalformedURLException {
        Service service = Service.create(Weather.class.getClassLoader().getResource("wsdl/weather.wsdl"), new QName("http://ws.cdyne.com/WeatherWS/", "Weather"));
        System.out.println(service.getPort(WeatherSoap.class).getCityWeatherByZIP(zip).getCity());
    }
}

