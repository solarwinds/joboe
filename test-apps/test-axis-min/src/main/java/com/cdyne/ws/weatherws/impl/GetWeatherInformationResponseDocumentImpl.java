/*
 * An XML document type.
 * Localname: GetWeatherInformationResponse
 * Namespace: http://ws.cdyne.com/WeatherWS/
 * Java type: com.cdyne.ws.weatherws.GetWeatherInformationResponseDocument
 *
 * Automatically generated - do not modify.
 */
package com.cdyne.ws.weatherws.impl;
/**
 * A document containing one GetWeatherInformationResponse(@http://ws.cdyne.com/WeatherWS/) element.
 *
 * This is a complex type.
 */
public class GetWeatherInformationResponseDocumentImpl extends org.apache.xmlbeans.impl.values.XmlComplexContentImpl implements com.cdyne.ws.weatherws.GetWeatherInformationResponseDocument
{
    
    public GetWeatherInformationResponseDocumentImpl(org.apache.xmlbeans.SchemaType sType)
    {
        super(sType);
    }
    
    private static final javax.xml.namespace.QName GETWEATHERINFORMATIONRESPONSE$0 = 
        new javax.xml.namespace.QName("http://ws.cdyne.com/WeatherWS/", "GetWeatherInformationResponse");
    
    
    /**
     * Gets the "GetWeatherInformationResponse" element
     */
    public com.cdyne.ws.weatherws.GetWeatherInformationResponseDocument.GetWeatherInformationResponse getGetWeatherInformationResponse()
    {
        synchronized (monitor())
        {
            check_orphaned();
            com.cdyne.ws.weatherws.GetWeatherInformationResponseDocument.GetWeatherInformationResponse target = null;
            target = (com.cdyne.ws.weatherws.GetWeatherInformationResponseDocument.GetWeatherInformationResponse)get_store().find_element_user(GETWEATHERINFORMATIONRESPONSE$0, 0);
            if (target == null)
            {
                return null;
            }
            return target;
        }
    }
    
    /**
     * Sets the "GetWeatherInformationResponse" element
     */
    public void setGetWeatherInformationResponse(com.cdyne.ws.weatherws.GetWeatherInformationResponseDocument.GetWeatherInformationResponse getWeatherInformationResponse)
    {
        synchronized (monitor())
        {
            check_orphaned();
            com.cdyne.ws.weatherws.GetWeatherInformationResponseDocument.GetWeatherInformationResponse target = null;
            target = (com.cdyne.ws.weatherws.GetWeatherInformationResponseDocument.GetWeatherInformationResponse)get_store().find_element_user(GETWEATHERINFORMATIONRESPONSE$0, 0);
            if (target == null)
            {
                target = (com.cdyne.ws.weatherws.GetWeatherInformationResponseDocument.GetWeatherInformationResponse)get_store().add_element_user(GETWEATHERINFORMATIONRESPONSE$0);
            }
            target.set(getWeatherInformationResponse);
        }
    }
    
    /**
     * Appends and returns a new empty "GetWeatherInformationResponse" element
     */
    public com.cdyne.ws.weatherws.GetWeatherInformationResponseDocument.GetWeatherInformationResponse addNewGetWeatherInformationResponse()
    {
        synchronized (monitor())
        {
            check_orphaned();
            com.cdyne.ws.weatherws.GetWeatherInformationResponseDocument.GetWeatherInformationResponse target = null;
            target = (com.cdyne.ws.weatherws.GetWeatherInformationResponseDocument.GetWeatherInformationResponse)get_store().add_element_user(GETWEATHERINFORMATIONRESPONSE$0);
            return target;
        }
    }
    /**
     * An XML GetWeatherInformationResponse(@http://ws.cdyne.com/WeatherWS/).
     *
     * This is a complex type.
     */
    public static class GetWeatherInformationResponseImpl extends org.apache.xmlbeans.impl.values.XmlComplexContentImpl implements com.cdyne.ws.weatherws.GetWeatherInformationResponseDocument.GetWeatherInformationResponse
    {
        
        public GetWeatherInformationResponseImpl(org.apache.xmlbeans.SchemaType sType)
        {
            super(sType);
        }
        
        private static final javax.xml.namespace.QName GETWEATHERINFORMATIONRESULT$0 = 
            new javax.xml.namespace.QName("http://ws.cdyne.com/WeatherWS/", "GetWeatherInformationResult");
        
        
        /**
         * Gets the "GetWeatherInformationResult" element
         */
        public com.cdyne.ws.weatherws.ArrayOfWeatherDescription getGetWeatherInformationResult()
        {
            synchronized (monitor())
            {
                check_orphaned();
                com.cdyne.ws.weatherws.ArrayOfWeatherDescription target = null;
                target = (com.cdyne.ws.weatherws.ArrayOfWeatherDescription)get_store().find_element_user(GETWEATHERINFORMATIONRESULT$0, 0);
                if (target == null)
                {
                    return null;
                }
                return target;
            }
        }
        
        /**
         * True if has "GetWeatherInformationResult" element
         */
        public boolean isSetGetWeatherInformationResult()
        {
            synchronized (monitor())
            {
                check_orphaned();
                return get_store().count_elements(GETWEATHERINFORMATIONRESULT$0) != 0;
            }
        }
        
        /**
         * Sets the "GetWeatherInformationResult" element
         */
        public void setGetWeatherInformationResult(com.cdyne.ws.weatherws.ArrayOfWeatherDescription getWeatherInformationResult)
        {
            synchronized (monitor())
            {
                check_orphaned();
                com.cdyne.ws.weatherws.ArrayOfWeatherDescription target = null;
                target = (com.cdyne.ws.weatherws.ArrayOfWeatherDescription)get_store().find_element_user(GETWEATHERINFORMATIONRESULT$0, 0);
                if (target == null)
                {
                    target = (com.cdyne.ws.weatherws.ArrayOfWeatherDescription)get_store().add_element_user(GETWEATHERINFORMATIONRESULT$0);
                }
                target.set(getWeatherInformationResult);
            }
        }
        
        /**
         * Appends and returns a new empty "GetWeatherInformationResult" element
         */
        public com.cdyne.ws.weatherws.ArrayOfWeatherDescription addNewGetWeatherInformationResult()
        {
            synchronized (monitor())
            {
                check_orphaned();
                com.cdyne.ws.weatherws.ArrayOfWeatherDescription target = null;
                target = (com.cdyne.ws.weatherws.ArrayOfWeatherDescription)get_store().add_element_user(GETWEATHERINFORMATIONRESULT$0);
                return target;
            }
        }
        
        /**
         * Unsets the "GetWeatherInformationResult" element
         */
        public void unsetGetWeatherInformationResult()
        {
            synchronized (monitor())
            {
                check_orphaned();
                get_store().remove_element(GETWEATHERINFORMATIONRESULT$0, 0);
            }
        }
    }
}
