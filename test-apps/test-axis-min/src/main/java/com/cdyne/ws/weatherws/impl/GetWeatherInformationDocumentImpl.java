/*
 * An XML document type.
 * Localname: GetWeatherInformation
 * Namespace: http://ws.cdyne.com/WeatherWS/
 * Java type: com.cdyne.ws.weatherws.GetWeatherInformationDocument
 *
 * Automatically generated - do not modify.
 */
package com.cdyne.ws.weatherws.impl;
/**
 * A document containing one GetWeatherInformation(@http://ws.cdyne.com/WeatherWS/) element.
 *
 * This is a complex type.
 */
public class GetWeatherInformationDocumentImpl extends org.apache.xmlbeans.impl.values.XmlComplexContentImpl implements com.cdyne.ws.weatherws.GetWeatherInformationDocument
{
    
    public GetWeatherInformationDocumentImpl(org.apache.xmlbeans.SchemaType sType)
    {
        super(sType);
    }
    
    private static final javax.xml.namespace.QName GETWEATHERINFORMATION$0 = 
        new javax.xml.namespace.QName("http://ws.cdyne.com/WeatherWS/", "GetWeatherInformation");
    
    
    /**
     * Gets the "GetWeatherInformation" element
     */
    public com.cdyne.ws.weatherws.GetWeatherInformationDocument.GetWeatherInformation getGetWeatherInformation()
    {
        synchronized (monitor())
        {
            check_orphaned();
            com.cdyne.ws.weatherws.GetWeatherInformationDocument.GetWeatherInformation target = null;
            target = (com.cdyne.ws.weatherws.GetWeatherInformationDocument.GetWeatherInformation)get_store().find_element_user(GETWEATHERINFORMATION$0, 0);
            if (target == null)
            {
                return null;
            }
            return target;
        }
    }
    
    /**
     * Sets the "GetWeatherInformation" element
     */
    public void setGetWeatherInformation(com.cdyne.ws.weatherws.GetWeatherInformationDocument.GetWeatherInformation getWeatherInformation)
    {
        synchronized (monitor())
        {
            check_orphaned();
            com.cdyne.ws.weatherws.GetWeatherInformationDocument.GetWeatherInformation target = null;
            target = (com.cdyne.ws.weatherws.GetWeatherInformationDocument.GetWeatherInformation)get_store().find_element_user(GETWEATHERINFORMATION$0, 0);
            if (target == null)
            {
                target = (com.cdyne.ws.weatherws.GetWeatherInformationDocument.GetWeatherInformation)get_store().add_element_user(GETWEATHERINFORMATION$0);
            }
            target.set(getWeatherInformation);
        }
    }
    
    /**
     * Appends and returns a new empty "GetWeatherInformation" element
     */
    public com.cdyne.ws.weatherws.GetWeatherInformationDocument.GetWeatherInformation addNewGetWeatherInformation()
    {
        synchronized (monitor())
        {
            check_orphaned();
            com.cdyne.ws.weatherws.GetWeatherInformationDocument.GetWeatherInformation target = null;
            target = (com.cdyne.ws.weatherws.GetWeatherInformationDocument.GetWeatherInformation)get_store().add_element_user(GETWEATHERINFORMATION$0);
            return target;
        }
    }
    /**
     * An XML GetWeatherInformation(@http://ws.cdyne.com/WeatherWS/).
     *
     * This is a complex type.
     */
    public static class GetWeatherInformationImpl extends org.apache.xmlbeans.impl.values.XmlComplexContentImpl implements com.cdyne.ws.weatherws.GetWeatherInformationDocument.GetWeatherInformation
    {
        
        public GetWeatherInformationImpl(org.apache.xmlbeans.SchemaType sType)
        {
            super(sType);
        }
        
        
    }
}
