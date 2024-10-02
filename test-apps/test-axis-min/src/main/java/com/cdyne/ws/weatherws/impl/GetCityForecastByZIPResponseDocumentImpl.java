/*
 * An XML document type.
 * Localname: GetCityForecastByZIPResponse
 * Namespace: http://ws.cdyne.com/WeatherWS/
 * Java type: com.cdyne.ws.weatherws.GetCityForecastByZIPResponseDocument
 *
 * Automatically generated - do not modify.
 */
package com.cdyne.ws.weatherws.impl;
/**
 * A document containing one GetCityForecastByZIPResponse(@http://ws.cdyne.com/WeatherWS/) element.
 *
 * This is a complex type.
 */
public class GetCityForecastByZIPResponseDocumentImpl extends org.apache.xmlbeans.impl.values.XmlComplexContentImpl implements com.cdyne.ws.weatherws.GetCityForecastByZIPResponseDocument
{
    
    public GetCityForecastByZIPResponseDocumentImpl(org.apache.xmlbeans.SchemaType sType)
    {
        super(sType);
    }
    
    private static final javax.xml.namespace.QName GETCITYFORECASTBYZIPRESPONSE$0 = 
        new javax.xml.namespace.QName("http://ws.cdyne.com/WeatherWS/", "GetCityForecastByZIPResponse");
    
    
    /**
     * Gets the "GetCityForecastByZIPResponse" element
     */
    public com.cdyne.ws.weatherws.GetCityForecastByZIPResponseDocument.GetCityForecastByZIPResponse getGetCityForecastByZIPResponse()
    {
        synchronized (monitor())
        {
            check_orphaned();
            com.cdyne.ws.weatherws.GetCityForecastByZIPResponseDocument.GetCityForecastByZIPResponse target = null;
            target = (com.cdyne.ws.weatherws.GetCityForecastByZIPResponseDocument.GetCityForecastByZIPResponse)get_store().find_element_user(GETCITYFORECASTBYZIPRESPONSE$0, 0);
            if (target == null)
            {
                return null;
            }
            return target;
        }
    }
    
    /**
     * Sets the "GetCityForecastByZIPResponse" element
     */
    public void setGetCityForecastByZIPResponse(com.cdyne.ws.weatherws.GetCityForecastByZIPResponseDocument.GetCityForecastByZIPResponse getCityForecastByZIPResponse)
    {
        synchronized (monitor())
        {
            check_orphaned();
            com.cdyne.ws.weatherws.GetCityForecastByZIPResponseDocument.GetCityForecastByZIPResponse target = null;
            target = (com.cdyne.ws.weatherws.GetCityForecastByZIPResponseDocument.GetCityForecastByZIPResponse)get_store().find_element_user(GETCITYFORECASTBYZIPRESPONSE$0, 0);
            if (target == null)
            {
                target = (com.cdyne.ws.weatherws.GetCityForecastByZIPResponseDocument.GetCityForecastByZIPResponse)get_store().add_element_user(GETCITYFORECASTBYZIPRESPONSE$0);
            }
            target.set(getCityForecastByZIPResponse);
        }
    }
    
    /**
     * Appends and returns a new empty "GetCityForecastByZIPResponse" element
     */
    public com.cdyne.ws.weatherws.GetCityForecastByZIPResponseDocument.GetCityForecastByZIPResponse addNewGetCityForecastByZIPResponse()
    {
        synchronized (monitor())
        {
            check_orphaned();
            com.cdyne.ws.weatherws.GetCityForecastByZIPResponseDocument.GetCityForecastByZIPResponse target = null;
            target = (com.cdyne.ws.weatherws.GetCityForecastByZIPResponseDocument.GetCityForecastByZIPResponse)get_store().add_element_user(GETCITYFORECASTBYZIPRESPONSE$0);
            return target;
        }
    }
    /**
     * An XML GetCityForecastByZIPResponse(@http://ws.cdyne.com/WeatherWS/).
     *
     * This is a complex type.
     */
    public static class GetCityForecastByZIPResponseImpl extends org.apache.xmlbeans.impl.values.XmlComplexContentImpl implements com.cdyne.ws.weatherws.GetCityForecastByZIPResponseDocument.GetCityForecastByZIPResponse
    {
        
        public GetCityForecastByZIPResponseImpl(org.apache.xmlbeans.SchemaType sType)
        {
            super(sType);
        }
        
        private static final javax.xml.namespace.QName GETCITYFORECASTBYZIPRESULT$0 = 
            new javax.xml.namespace.QName("http://ws.cdyne.com/WeatherWS/", "GetCityForecastByZIPResult");
        
        
        /**
         * Gets the "GetCityForecastByZIPResult" element
         */
        public com.cdyne.ws.weatherws.ForecastReturn getGetCityForecastByZIPResult()
        {
            synchronized (monitor())
            {
                check_orphaned();
                com.cdyne.ws.weatherws.ForecastReturn target = null;
                target = (com.cdyne.ws.weatherws.ForecastReturn)get_store().find_element_user(GETCITYFORECASTBYZIPRESULT$0, 0);
                if (target == null)
                {
                    return null;
                }
                return target;
            }
        }
        
        /**
         * True if has "GetCityForecastByZIPResult" element
         */
        public boolean isSetGetCityForecastByZIPResult()
        {
            synchronized (monitor())
            {
                check_orphaned();
                return get_store().count_elements(GETCITYFORECASTBYZIPRESULT$0) != 0;
            }
        }
        
        /**
         * Sets the "GetCityForecastByZIPResult" element
         */
        public void setGetCityForecastByZIPResult(com.cdyne.ws.weatherws.ForecastReturn getCityForecastByZIPResult)
        {
            synchronized (monitor())
            {
                check_orphaned();
                com.cdyne.ws.weatherws.ForecastReturn target = null;
                target = (com.cdyne.ws.weatherws.ForecastReturn)get_store().find_element_user(GETCITYFORECASTBYZIPRESULT$0, 0);
                if (target == null)
                {
                    target = (com.cdyne.ws.weatherws.ForecastReturn)get_store().add_element_user(GETCITYFORECASTBYZIPRESULT$0);
                }
                target.set(getCityForecastByZIPResult);
            }
        }
        
        /**
         * Appends and returns a new empty "GetCityForecastByZIPResult" element
         */
        public com.cdyne.ws.weatherws.ForecastReturn addNewGetCityForecastByZIPResult()
        {
            synchronized (monitor())
            {
                check_orphaned();
                com.cdyne.ws.weatherws.ForecastReturn target = null;
                target = (com.cdyne.ws.weatherws.ForecastReturn)get_store().add_element_user(GETCITYFORECASTBYZIPRESULT$0);
                return target;
            }
        }
        
        /**
         * Unsets the "GetCityForecastByZIPResult" element
         */
        public void unsetGetCityForecastByZIPResult()
        {
            synchronized (monitor())
            {
                check_orphaned();
                get_store().remove_element(GETCITYFORECASTBYZIPRESULT$0, 0);
            }
        }
    }
}
