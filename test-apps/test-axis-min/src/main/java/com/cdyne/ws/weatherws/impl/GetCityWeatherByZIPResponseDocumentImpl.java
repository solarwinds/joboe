/*
 * An XML document type.
 * Localname: GetCityWeatherByZIPResponse
 * Namespace: http://ws.cdyne.com/WeatherWS/
 * Java type: com.cdyne.ws.weatherws.GetCityWeatherByZIPResponseDocument
 *
 * Automatically generated - do not modify.
 */
package com.cdyne.ws.weatherws.impl;
/**
 * A document containing one GetCityWeatherByZIPResponse(@http://ws.cdyne.com/WeatherWS/) element.
 *
 * This is a complex type.
 */
public class GetCityWeatherByZIPResponseDocumentImpl extends org.apache.xmlbeans.impl.values.XmlComplexContentImpl implements com.cdyne.ws.weatherws.GetCityWeatherByZIPResponseDocument
{
    
    public GetCityWeatherByZIPResponseDocumentImpl(org.apache.xmlbeans.SchemaType sType)
    {
        super(sType);
    }
    
    private static final javax.xml.namespace.QName GETCITYWEATHERBYZIPRESPONSE$0 = 
        new javax.xml.namespace.QName("http://ws.cdyne.com/WeatherWS/", "GetCityWeatherByZIPResponse");
    
    
    /**
     * Gets the "GetCityWeatherByZIPResponse" element
     */
    public com.cdyne.ws.weatherws.GetCityWeatherByZIPResponseDocument.GetCityWeatherByZIPResponse getGetCityWeatherByZIPResponse()
    {
        synchronized (monitor())
        {
            check_orphaned();
            com.cdyne.ws.weatherws.GetCityWeatherByZIPResponseDocument.GetCityWeatherByZIPResponse target = null;
            target = (com.cdyne.ws.weatherws.GetCityWeatherByZIPResponseDocument.GetCityWeatherByZIPResponse)get_store().find_element_user(GETCITYWEATHERBYZIPRESPONSE$0, 0);
            if (target == null)
            {
                return null;
            }
            return target;
        }
    }
    
    /**
     * Sets the "GetCityWeatherByZIPResponse" element
     */
    public void setGetCityWeatherByZIPResponse(com.cdyne.ws.weatherws.GetCityWeatherByZIPResponseDocument.GetCityWeatherByZIPResponse getCityWeatherByZIPResponse)
    {
        synchronized (monitor())
        {
            check_orphaned();
            com.cdyne.ws.weatherws.GetCityWeatherByZIPResponseDocument.GetCityWeatherByZIPResponse target = null;
            target = (com.cdyne.ws.weatherws.GetCityWeatherByZIPResponseDocument.GetCityWeatherByZIPResponse)get_store().find_element_user(GETCITYWEATHERBYZIPRESPONSE$0, 0);
            if (target == null)
            {
                target = (com.cdyne.ws.weatherws.GetCityWeatherByZIPResponseDocument.GetCityWeatherByZIPResponse)get_store().add_element_user(GETCITYWEATHERBYZIPRESPONSE$0);
            }
            target.set(getCityWeatherByZIPResponse);
        }
    }
    
    /**
     * Appends and returns a new empty "GetCityWeatherByZIPResponse" element
     */
    public com.cdyne.ws.weatherws.GetCityWeatherByZIPResponseDocument.GetCityWeatherByZIPResponse addNewGetCityWeatherByZIPResponse()
    {
        synchronized (monitor())
        {
            check_orphaned();
            com.cdyne.ws.weatherws.GetCityWeatherByZIPResponseDocument.GetCityWeatherByZIPResponse target = null;
            target = (com.cdyne.ws.weatherws.GetCityWeatherByZIPResponseDocument.GetCityWeatherByZIPResponse)get_store().add_element_user(GETCITYWEATHERBYZIPRESPONSE$0);
            return target;
        }
    }
    /**
     * An XML GetCityWeatherByZIPResponse(@http://ws.cdyne.com/WeatherWS/).
     *
     * This is a complex type.
     */
    public static class GetCityWeatherByZIPResponseImpl extends org.apache.xmlbeans.impl.values.XmlComplexContentImpl implements com.cdyne.ws.weatherws.GetCityWeatherByZIPResponseDocument.GetCityWeatherByZIPResponse
    {
        
        public GetCityWeatherByZIPResponseImpl(org.apache.xmlbeans.SchemaType sType)
        {
            super(sType);
        }
        
        private static final javax.xml.namespace.QName GETCITYWEATHERBYZIPRESULT$0 = 
            new javax.xml.namespace.QName("http://ws.cdyne.com/WeatherWS/", "GetCityWeatherByZIPResult");
        
        
        /**
         * Gets the "GetCityWeatherByZIPResult" element
         */
        public com.cdyne.ws.weatherws.WeatherReturn getGetCityWeatherByZIPResult()
        {
            synchronized (monitor())
            {
                check_orphaned();
                com.cdyne.ws.weatherws.WeatherReturn target = null;
                target = (com.cdyne.ws.weatherws.WeatherReturn)get_store().find_element_user(GETCITYWEATHERBYZIPRESULT$0, 0);
                if (target == null)
                {
                    return null;
                }
                return target;
            }
        }
        
        /**
         * Sets the "GetCityWeatherByZIPResult" element
         */
        public void setGetCityWeatherByZIPResult(com.cdyne.ws.weatherws.WeatherReturn getCityWeatherByZIPResult)
        {
            synchronized (monitor())
            {
                check_orphaned();
                com.cdyne.ws.weatherws.WeatherReturn target = null;
                target = (com.cdyne.ws.weatherws.WeatherReturn)get_store().find_element_user(GETCITYWEATHERBYZIPRESULT$0, 0);
                if (target == null)
                {
                    target = (com.cdyne.ws.weatherws.WeatherReturn)get_store().add_element_user(GETCITYWEATHERBYZIPRESULT$0);
                }
                target.set(getCityWeatherByZIPResult);
            }
        }
        
        /**
         * Appends and returns a new empty "GetCityWeatherByZIPResult" element
         */
        public com.cdyne.ws.weatherws.WeatherReturn addNewGetCityWeatherByZIPResult()
        {
            synchronized (monitor())
            {
                check_orphaned();
                com.cdyne.ws.weatherws.WeatherReturn target = null;
                target = (com.cdyne.ws.weatherws.WeatherReturn)get_store().add_element_user(GETCITYWEATHERBYZIPRESULT$0);
                return target;
            }
        }
    }
}
