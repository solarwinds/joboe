/*
 * An XML document type.
 * Localname: WeatherReturn
 * Namespace: http://ws.cdyne.com/WeatherWS/
 * Java type: com.cdyne.ws.weatherws.WeatherReturnDocument
 *
 * Automatically generated - do not modify.
 */
package com.cdyne.ws.weatherws.impl;
/**
 * A document containing one WeatherReturn(@http://ws.cdyne.com/WeatherWS/) element.
 *
 * This is a complex type.
 */
public class WeatherReturnDocumentImpl extends org.apache.xmlbeans.impl.values.XmlComplexContentImpl implements com.cdyne.ws.weatherws.WeatherReturnDocument
{
    
    public WeatherReturnDocumentImpl(org.apache.xmlbeans.SchemaType sType)
    {
        super(sType);
    }
    
    private static final javax.xml.namespace.QName WEATHERRETURN$0 = 
        new javax.xml.namespace.QName("http://ws.cdyne.com/WeatherWS/", "WeatherReturn");
    
    
    /**
     * Gets the "WeatherReturn" element
     */
    public com.cdyne.ws.weatherws.WeatherReturn getWeatherReturn()
    {
        synchronized (monitor())
        {
            check_orphaned();
            com.cdyne.ws.weatherws.WeatherReturn target = null;
            target = (com.cdyne.ws.weatherws.WeatherReturn)get_store().find_element_user(WEATHERRETURN$0, 0);
            if (target == null)
            {
                return null;
            }
            return target;
        }
    }
    
    /**
     * Sets the "WeatherReturn" element
     */
    public void setWeatherReturn(com.cdyne.ws.weatherws.WeatherReturn weatherReturn)
    {
        synchronized (monitor())
        {
            check_orphaned();
            com.cdyne.ws.weatherws.WeatherReturn target = null;
            target = (com.cdyne.ws.weatherws.WeatherReturn)get_store().find_element_user(WEATHERRETURN$0, 0);
            if (target == null)
            {
                target = (com.cdyne.ws.weatherws.WeatherReturn)get_store().add_element_user(WEATHERRETURN$0);
            }
            target.set(weatherReturn);
        }
    }
    
    /**
     * Appends and returns a new empty "WeatherReturn" element
     */
    public com.cdyne.ws.weatherws.WeatherReturn addNewWeatherReturn()
    {
        synchronized (monitor())
        {
            check_orphaned();
            com.cdyne.ws.weatherws.WeatherReturn target = null;
            target = (com.cdyne.ws.weatherws.WeatherReturn)get_store().add_element_user(WEATHERRETURN$0);
            return target;
        }
    }
}
