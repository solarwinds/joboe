/*
 * An XML document type.
 * Localname: GetCityForecastByZIP
 * Namespace: http://ws.cdyne.com/WeatherWS/
 * Java type: com.cdyne.ws.weatherws.GetCityForecastByZIPDocument
 *
 * Automatically generated - do not modify.
 */
package com.cdyne.ws.weatherws.impl;
/**
 * A document containing one GetCityForecastByZIP(@http://ws.cdyne.com/WeatherWS/) element.
 *
 * This is a complex type.
 */
public class GetCityForecastByZIPDocumentImpl extends org.apache.xmlbeans.impl.values.XmlComplexContentImpl implements com.cdyne.ws.weatherws.GetCityForecastByZIPDocument
{
    
    public GetCityForecastByZIPDocumentImpl(org.apache.xmlbeans.SchemaType sType)
    {
        super(sType);
    }
    
    private static final javax.xml.namespace.QName GETCITYFORECASTBYZIP$0 = 
        new javax.xml.namespace.QName("http://ws.cdyne.com/WeatherWS/", "GetCityForecastByZIP");
    
    
    /**
     * Gets the "GetCityForecastByZIP" element
     */
    public com.cdyne.ws.weatherws.GetCityForecastByZIPDocument.GetCityForecastByZIP getGetCityForecastByZIP()
    {
        synchronized (monitor())
        {
            check_orphaned();
            com.cdyne.ws.weatherws.GetCityForecastByZIPDocument.GetCityForecastByZIP target = null;
            target = (com.cdyne.ws.weatherws.GetCityForecastByZIPDocument.GetCityForecastByZIP)get_store().find_element_user(GETCITYFORECASTBYZIP$0, 0);
            if (target == null)
            {
                return null;
            }
            return target;
        }
    }
    
    /**
     * Sets the "GetCityForecastByZIP" element
     */
    public void setGetCityForecastByZIP(com.cdyne.ws.weatherws.GetCityForecastByZIPDocument.GetCityForecastByZIP getCityForecastByZIP)
    {
        synchronized (monitor())
        {
            check_orphaned();
            com.cdyne.ws.weatherws.GetCityForecastByZIPDocument.GetCityForecastByZIP target = null;
            target = (com.cdyne.ws.weatherws.GetCityForecastByZIPDocument.GetCityForecastByZIP)get_store().find_element_user(GETCITYFORECASTBYZIP$0, 0);
            if (target == null)
            {
                target = (com.cdyne.ws.weatherws.GetCityForecastByZIPDocument.GetCityForecastByZIP)get_store().add_element_user(GETCITYFORECASTBYZIP$0);
            }
            target.set(getCityForecastByZIP);
        }
    }
    
    /**
     * Appends and returns a new empty "GetCityForecastByZIP" element
     */
    public com.cdyne.ws.weatherws.GetCityForecastByZIPDocument.GetCityForecastByZIP addNewGetCityForecastByZIP()
    {
        synchronized (monitor())
        {
            check_orphaned();
            com.cdyne.ws.weatherws.GetCityForecastByZIPDocument.GetCityForecastByZIP target = null;
            target = (com.cdyne.ws.weatherws.GetCityForecastByZIPDocument.GetCityForecastByZIP)get_store().add_element_user(GETCITYFORECASTBYZIP$0);
            return target;
        }
    }
    /**
     * An XML GetCityForecastByZIP(@http://ws.cdyne.com/WeatherWS/).
     *
     * This is a complex type.
     */
    public static class GetCityForecastByZIPImpl extends org.apache.xmlbeans.impl.values.XmlComplexContentImpl implements com.cdyne.ws.weatherws.GetCityForecastByZIPDocument.GetCityForecastByZIP
    {
        
        public GetCityForecastByZIPImpl(org.apache.xmlbeans.SchemaType sType)
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
