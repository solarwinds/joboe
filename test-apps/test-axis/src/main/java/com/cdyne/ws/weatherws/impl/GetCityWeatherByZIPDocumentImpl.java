/*
 * An XML document type.
 * Localname: GetCityWeatherByZIP
 * Namespace: http://ws.cdyne.com/WeatherWS/
 * Java type: com.cdyne.ws.weatherws.GetCityWeatherByZIPDocument
 *
 * Automatically generated - do not modify.
 */
package com.cdyne.ws.weatherws.impl;
/**
 * A document containing one GetCityWeatherByZIP(@http://ws.cdyne.com/WeatherWS/) element.
 *
 * This is a complex type.
 */
public class GetCityWeatherByZIPDocumentImpl extends org.apache.xmlbeans.impl.values.XmlComplexContentImpl implements com.cdyne.ws.weatherws.GetCityWeatherByZIPDocument
{
    
    public GetCityWeatherByZIPDocumentImpl(org.apache.xmlbeans.SchemaType sType)
    {
        super(sType);
    }
    
    private static final javax.xml.namespace.QName GETCITYWEATHERBYZIP$0 = 
        new javax.xml.namespace.QName("http://ws.cdyne.com/WeatherWS/", "GetCityWeatherByZIP");
    
    
    /**
     * Gets the "GetCityWeatherByZIP" element
     */
    public com.cdyne.ws.weatherws.GetCityWeatherByZIPDocument.GetCityWeatherByZIP getGetCityWeatherByZIP()
    {
        synchronized (monitor())
        {
            check_orphaned();
            com.cdyne.ws.weatherws.GetCityWeatherByZIPDocument.GetCityWeatherByZIP target = null;
            target = (com.cdyne.ws.weatherws.GetCityWeatherByZIPDocument.GetCityWeatherByZIP)get_store().find_element_user(GETCITYWEATHERBYZIP$0, 0);
            if (target == null)
            {
                return null;
            }
            return target;
        }
    }
    
    /**
     * Sets the "GetCityWeatherByZIP" element
     */
    public void setGetCityWeatherByZIP(com.cdyne.ws.weatherws.GetCityWeatherByZIPDocument.GetCityWeatherByZIP getCityWeatherByZIP)
    {
        synchronized (monitor())
        {
            check_orphaned();
            com.cdyne.ws.weatherws.GetCityWeatherByZIPDocument.GetCityWeatherByZIP target = null;
            target = (com.cdyne.ws.weatherws.GetCityWeatherByZIPDocument.GetCityWeatherByZIP)get_store().find_element_user(GETCITYWEATHERBYZIP$0, 0);
            if (target == null)
            {
                target = (com.cdyne.ws.weatherws.GetCityWeatherByZIPDocument.GetCityWeatherByZIP)get_store().add_element_user(GETCITYWEATHERBYZIP$0);
            }
            target.set(getCityWeatherByZIP);
        }
    }
    
    /**
     * Appends and returns a new empty "GetCityWeatherByZIP" element
     */
    public com.cdyne.ws.weatherws.GetCityWeatherByZIPDocument.GetCityWeatherByZIP addNewGetCityWeatherByZIP()
    {
        synchronized (monitor())
        {
            check_orphaned();
            com.cdyne.ws.weatherws.GetCityWeatherByZIPDocument.GetCityWeatherByZIP target = null;
            target = (com.cdyne.ws.weatherws.GetCityWeatherByZIPDocument.GetCityWeatherByZIP)get_store().add_element_user(GETCITYWEATHERBYZIP$0);
            return target;
        }
    }
    /**
     * An XML GetCityWeatherByZIP(@http://ws.cdyne.com/WeatherWS/).
     *
     * This is a complex type.
     */
    public static class GetCityWeatherByZIPImpl extends org.apache.xmlbeans.impl.values.XmlComplexContentImpl implements com.cdyne.ws.weatherws.GetCityWeatherByZIPDocument.GetCityWeatherByZIP
    {
        
        public GetCityWeatherByZIPImpl(org.apache.xmlbeans.SchemaType sType)
        {
            super(sType);
        }
        
        private static final javax.xml.namespace.QName ZIP$0 = 
            new javax.xml.namespace.QName("http://ws.cdyne.com/WeatherWS/", "ZIP");
        
        
        /**
         * Gets the "ZIP" element
         */
        public java.lang.String getZIP()
        {
            synchronized (monitor())
            {
                check_orphaned();
                org.apache.xmlbeans.SimpleValue target = null;
                target = (org.apache.xmlbeans.SimpleValue)get_store().find_element_user(ZIP$0, 0);
                if (target == null)
                {
                    return null;
                }
                return target.getStringValue();
            }
        }
        
        /**
         * Gets (as xml) the "ZIP" element
         */
        public org.apache.xmlbeans.XmlString xgetZIP()
        {
            synchronized (monitor())
            {
                check_orphaned();
                org.apache.xmlbeans.XmlString target = null;
                target = (org.apache.xmlbeans.XmlString)get_store().find_element_user(ZIP$0, 0);
                return target;
            }
        }
        
        /**
         * True if has "ZIP" element
         */
        public boolean isSetZIP()
        {
            synchronized (monitor())
            {
                check_orphaned();
                return get_store().count_elements(ZIP$0) != 0;
            }
        }
        
        /**
         * Sets the "ZIP" element
         */
        public void setZIP(java.lang.String zip)
        {
            synchronized (monitor())
            {
                check_orphaned();
                org.apache.xmlbeans.SimpleValue target = null;
                target = (org.apache.xmlbeans.SimpleValue)get_store().find_element_user(ZIP$0, 0);
                if (target == null)
                {
                    target = (org.apache.xmlbeans.SimpleValue)get_store().add_element_user(ZIP$0);
                }
                target.setStringValue(zip);
            }
        }
        
        /**
         * Sets (as xml) the "ZIP" element
         */
        public void xsetZIP(org.apache.xmlbeans.XmlString zip)
        {
            synchronized (monitor())
            {
                check_orphaned();
                org.apache.xmlbeans.XmlString target = null;
                target = (org.apache.xmlbeans.XmlString)get_store().find_element_user(ZIP$0, 0);
                if (target == null)
                {
                    target = (org.apache.xmlbeans.XmlString)get_store().add_element_user(ZIP$0);
                }
                target.set(zip);
            }
        }
        
        /**
         * Unsets the "ZIP" element
         */
        public void unsetZIP()
        {
            synchronized (monitor())
            {
                check_orphaned();
                get_store().remove_element(ZIP$0, 0);
            }
        }
    }
}
