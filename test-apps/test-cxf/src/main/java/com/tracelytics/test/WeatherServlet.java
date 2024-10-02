package com.tracelytics.test;

import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamSource;
import javax.xml.ws.Dispatch;
import javax.xml.ws.Service;

import org.apache.cxf.frontend.ClientProxyFactoryBean;
import org.apache.cxf.jaxws.endpoint.dynamic.JaxWsDynamicClientFactory;

import com.cdyne.cxf.ws.weatherws.ForecastReturn;
import com.cdyne.cxf.ws.weatherws.Weather;
import com.cdyne.cxf.ws.weatherws.WeatherReturn;
import com.cdyne.cxf.ws.weatherws.WeatherSoap;
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
        testWsdl2Java(req, zip);
        
        
        //JAX-WS Proxy
        Service service = testJaxWsProxy(zip);
        
        
        //JAX-WS Dispatch
        testJaxWsDispatcher(zip, service);
        
        
        
        //Simple Frontend client proxy (does not work 1.2)
        testSimpleFrontendClient(zip);
        
        
        //Dynamic Client
        testJaxWsDynamicClient(zip);
        
        
        req.getRequestDispatcher("index.jsp").forward(req, resp);
    }

    @LogMethod(layer = "testWsdl2Java")
    private void testWsdl2Java(HttpServletRequest req, String zip) {
        if (zip != null) {
            WeatherSoap weatherSoap = new Weather().getWeatherSoap();
            ForecastReturn result = weatherSoap.getCityForecastByZIP(zip);
            if (result != null && result.isSuccess()) {
                req.setAttribute("city", result.getCity());
                req.setAttribute("forecasts", result.getForecastResult().getForecast());
            }
            
            WeatherReturn weatherReturn = weatherSoap.getCityWeatherByZIP(zip);
            
            System.out.println(weatherReturn.getCity());
        }
    }

    @LogMethod(layer = "testJaxWsProxy")
    private Service testJaxWsProxy(String zip) throws MalformedURLException {
        Service service = Service.create(Weather.class.getClassLoader().getResource("wsdl/weather.wsdl"), new QName("http://ws.cdyne.com/WeatherWS/", "Weather"));
        System.out.println(service.getPort(WeatherSoap.class).getCityWeatherByZIP(zip).getCity());
        return service;
    }

    @LogMethod(layer = "testJaxWsDispatcher")
    private void testJaxWsDispatcher(String zip, Service service) {
        Dispatch<StreamSource> dispatch = service.createDispatch(new QName("http://ws.cdyne.com/WeatherWS/", "WeatherSoap"), StreamSource.class, Service.Mode.PAYLOAD);
            
        StreamSource request = new StreamSource(new StringReader("<GetCityWeatherByZIP xmlns=\"http://ws.cdyne.com/WeatherWS/\"><ZIP>" + zip + "</ZIP></GetCityWeatherByZIP>"));
        
        dispatch.getRequestContext().put(Dispatch.SOAPACTION_USE_PROPERTY, new Boolean(true));
        dispatch.getRequestContext().put(Dispatch.SOAPACTION_URI_PROPERTY, "http://ws.cdyne.com/WeatherWS/GetCityWeatherByZIP");
        // Dispatch disp created previously
        StreamSource response = dispatch.invoke(request);
        System.out.println(response);
    }

    @LogMethod(layer = "testSimpleFrontendClient")
    private void testSimpleFrontendClient(String zip) {
        ClientProxyFactoryBean simpleFrontendFactory = new ClientProxyFactoryBean();
        simpleFrontendFactory.setServiceClass(WeatherSoap.class);
        simpleFrontendFactory.setAddress("http://wsf.cdyne.com/WeatherWS/Weather.asmx");
        
        try {
            System.out.println("Simple FE: " + ((WeatherSoap)simpleFrontendFactory.create()).getCityForecastByZIP(zip));
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    @LogMethod(layer = "testJaxWsDynamicClient")
    private void testJaxWsDynamicClient(String zip) {
        JaxWsDynamicClientFactory dcf = JaxWsDynamicClientFactory.newInstance();
        org.apache.cxf.endpoint.Client dynamicClient = dcf.createClient(Weather.class.getClassLoader().getResource("wsdl/weather.wsdl"));
    
        Object[] res;
        try {
            res = dynamicClient.invoke("GetCityWeatherByZIP", zip);
            System.out.println("Echo response: " + res[0]);
        } catch (Exception e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
    }
  
}
