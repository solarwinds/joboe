
package com.cdyne.ws.weatherws;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceFeature;


/**
 * This class was generated by the JAX-WS RI.
 * JAX-WS RI 2.1.6 in JDK 6
 * Generated source version: 2.1
 * 
 */
@WebServiceClient(name = "Weather", targetNamespace = "http://ws.cdyne.com/WeatherWS/", wsdlLocation = "classpath:wsdl/weather.wsdl")
public class Weather
    extends Service
{

    private final static URL WEATHER_WSDL_LOCATION;
    private final static Logger logger = Logger.getLogger(com.cdyne.ws.weatherws.Weather.class.getName());

    static {
URL url = Weather.class.getClassLoader().getResource("wsdl/weather.wsdl");
        
        if (url == null) {
            logger.warning("Failed to create URL for the wsdl Location: 'classpath:wsdl/weather.wsdl', retrying as a local file");
        }
        WEATHER_WSDL_LOCATION = url;
    }

    public Weather(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    public Weather() {
        super(WEATHER_WSDL_LOCATION, new QName("http://ws.cdyne.com/WeatherWS/", "Weather"));
    }

    /**
     * 
     * @return
     *     returns WeatherSoap
     */
    @WebEndpoint(name = "WeatherSoap")
    public WeatherSoap getWeatherSoap() {
        return super.getPort(new QName("http://ws.cdyne.com/WeatherWS/", "WeatherSoap"), WeatherSoap.class);
    }

    /**
     * 
     * @param features
     *     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns WeatherSoap
     */
    @WebEndpoint(name = "WeatherSoap")
    public WeatherSoap getWeatherSoap(WebServiceFeature... features) {
        return super.getPort(new QName("http://ws.cdyne.com/WeatherWS/", "WeatherSoap"), WeatherSoap.class, features);
    }

}
