package com.tracelytics.test;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;


import com.cdyne.cxf.ws.weatherws.Forecast;
import com.cdyne.cxf.ws.weatherws.ForecastReturn;
import com.cdyne.cxf.ws.weatherws.POP;
import com.cdyne.cxf.ws.weatherws.Temp;
import com.cdyne.cxf.ws.weatherws.Weather;
import com.cdyne.cxf.ws.weatherws.WeatherSoap;

public class SoapClient {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("EEEE, MMMM d yyyy");
 
    private static WeatherSoap weatherSoap = new Weather().getWeatherSoap();
    
    // An alternate way to get the SOAP service interface; includes logging interceptors.
    // JaxWsProxyFactoryBean factory = new org.apache.cxf.jaxws.JaxWsProxyFactoryBean();
    // factory.setServiceClass(WeatherSoap.class);
    // factory.setAddress("http://ws.cdyne.com/WeatherWS/Weather.asmx");
    // factory.getInInterceptors().add(new org.apache.cxf.interceptor.LoggingInInterceptor());
    // factory.getOutInterceptors().add(new org.apache.cxf.interceptor.LoggingOutInterceptor());
    // WeatherSoap weatherSoap = (WeatherSoap) factory.create();

    
    public static void main(String[] args) {
        try {
            System.out.println("Creating weather service instance (Note: Weather = Service subclass)...");
            long start = new Date().getTime();
            
            long end = new Date().getTime();
            System.out.println("Time required to initialize weather service interface: {} seconds" + (end - start) / 1000f);
 
            // Send a SOAP weather request for zip code 94025 (Menlo Park, CA, USA).
            System.out.println("weatherSoap instance: {}" + weatherSoap);
            start = new Date().getTime();
            ForecastReturn forecastReturn = weatherSoap.getCityForecastByZIP("94025");
            end = new Date().getTime();
            System.out.println("Time required to invoke 'getCityForecastByZIP': {} seconds"+ (end - start) / 1000f);
            System.out.println("forecastReturn: {}"+ forecastReturn);
            System.out.println("forecastReturn city: {}"+ forecastReturn.getCity());
            System.out.println("forecastReturn state: {}"+ forecastReturn.getState());
            System.out.println("forecastReturn result: {}"+ forecastReturn.getForecastResult());
            System.out.println("forecastReturn response text: {}"+ forecastReturn.getResponseText());
            System.out.println("");
            List<Forecast> forecasts = forecastReturn.getForecastResult().getForecast();
            for (Forecast forecast : forecasts) {
                System.out.println("  forecast date: {}"+ DATE_FORMAT.format(forecast.getDate().toGregorianCalendar().getTime()));
                System.out.println("  forecast description: {}"+ forecast.getDesciption());
                Temp temps = forecast.getTemperatures();
                System.out.println("  forecast temperature high: {}"+ temps.getDaytimeHigh());
                System.out.println(temps.getDaytimeHigh());
                System.out.println("  forecast temperature low: {}"+ temps.getMorningLow());
                POP pop = forecast.getProbabilityOfPrecipiation();
                System.out.println("  forecast precipitation day: {}%"+ pop.getDaytime());
                System.out.println("  forecast precipitation night: {}%"+ pop.getNighttime());
                System.out.println("");
            }
            System.out.println("Program complete, exiting");
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static ForecastReturn getForecasts(String zip) {
        return weatherSoap.getCityForecastByZIP(zip);
    }
}
