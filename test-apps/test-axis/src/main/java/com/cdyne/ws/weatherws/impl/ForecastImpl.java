/*
 * XML Type:  Forecast
 * Namespace: http://ws.cdyne.com/WeatherWS/
 * Java type: com.cdyne.ws.weatherws.Forecast
 *
 * Automatically generated - do not modify.
 */
package com.cdyne.ws.weatherws.impl;
/**
 * An XML Forecast(@http://ws.cdyne.com/WeatherWS/).
 *
 * This is a complex type.
 */
public class ForecastImpl extends org.apache.xmlbeans.impl.values.XmlComplexContentImpl implements com.cdyne.ws.weatherws.Forecast
{
    
    public ForecastImpl(org.apache.xmlbeans.SchemaType sType)
    {
        super(sType);
    }
    
    private static final javax.xml.namespace.QName DATE$0 = 
        new javax.xml.namespace.QName("http://ws.cdyne.com/WeatherWS/", "Date");
    private static final javax.xml.namespace.QName WEATHERID$2 = 
        new javax.xml.namespace.QName("http://ws.cdyne.com/WeatherWS/", "WeatherID");
    private static final javax.xml.namespace.QName DESCIPTION$4 = 
        new javax.xml.namespace.QName("http://ws.cdyne.com/WeatherWS/", "Desciption");
    private static final javax.xml.namespace.QName TEMPERATURES$6 = 
        new javax.xml.namespace.QName("http://ws.cdyne.com/WeatherWS/", "Temperatures");
    private static final javax.xml.namespace.QName PROBABILITYOFPRECIPIATION$8 = 
        new javax.xml.namespace.QName("http://ws.cdyne.com/WeatherWS/", "ProbabilityOfPrecipiation");
    
    
    /**
     * Gets the "Date" element
     */
    public java.util.Calendar getDate()
    {
        synchronized (monitor())
        {
            check_orphaned();
            org.apache.xmlbeans.SimpleValue target = null;
            target = (org.apache.xmlbeans.SimpleValue)get_store().find_element_user(DATE$0, 0);
            if (target == null)
            {
                return null;
            }
            return target.getCalendarValue();
        }
    }
    
    /**
     * Gets (as xml) the "Date" element
     */
    public org.apache.xmlbeans.XmlDateTime xgetDate()
    {
        synchronized (monitor())
        {
            check_orphaned();
            org.apache.xmlbeans.XmlDateTime target = null;
            target = (org.apache.xmlbeans.XmlDateTime)get_store().find_element_user(DATE$0, 0);
            return target;
        }
    }
    
    /**
     * Sets the "Date" element
     */
    public void setDate(java.util.Calendar date)
    {
        synchronized (monitor())
        {
            check_orphaned();
            org.apache.xmlbeans.SimpleValue target = null;
            target = (org.apache.xmlbeans.SimpleValue)get_store().find_element_user(DATE$0, 0);
            if (target == null)
            {
                target = (org.apache.xmlbeans.SimpleValue)get_store().add_element_user(DATE$0);
            }
            target.setCalendarValue(date);
        }
    }
    
    /**
     * Sets (as xml) the "Date" element
     */
    public void xsetDate(org.apache.xmlbeans.XmlDateTime date)
    {
        synchronized (monitor())
        {
            check_orphaned();
            org.apache.xmlbeans.XmlDateTime target = null;
            target = (org.apache.xmlbeans.XmlDateTime)get_store().find_element_user(DATE$0, 0);
            if (target == null)
            {
                target = (org.apache.xmlbeans.XmlDateTime)get_store().add_element_user(DATE$0);
            }
            target.set(date);
        }
    }
    
    /**
     * Gets the "WeatherID" element
     */
    public short getWeatherID()
    {
        synchronized (monitor())
        {
            check_orphaned();
            org.apache.xmlbeans.SimpleValue target = null;
            target = (org.apache.xmlbeans.SimpleValue)get_store().find_element_user(WEATHERID$2, 0);
            if (target == null)
            {
                return 0;
            }
            return target.getShortValue();
        }
    }
    
    /**
     * Gets (as xml) the "WeatherID" element
     */
    public org.apache.xmlbeans.XmlShort xgetWeatherID()
    {
        synchronized (monitor())
        {
            check_orphaned();
            org.apache.xmlbeans.XmlShort target = null;
            target = (org.apache.xmlbeans.XmlShort)get_store().find_element_user(WEATHERID$2, 0);
            return target;
        }
    }
    
    /**
     * Sets the "WeatherID" element
     */
    public void setWeatherID(short weatherID)
    {
        synchronized (monitor())
        {
            check_orphaned();
            org.apache.xmlbeans.SimpleValue target = null;
            target = (org.apache.xmlbeans.SimpleValue)get_store().find_element_user(WEATHERID$2, 0);
            if (target == null)
            {
                target = (org.apache.xmlbeans.SimpleValue)get_store().add_element_user(WEATHERID$2);
            }
            target.setShortValue(weatherID);
        }
    }
    
    /**
     * Sets (as xml) the "WeatherID" element
     */
    public void xsetWeatherID(org.apache.xmlbeans.XmlShort weatherID)
    {
        synchronized (monitor())
        {
            check_orphaned();
            org.apache.xmlbeans.XmlShort target = null;
            target = (org.apache.xmlbeans.XmlShort)get_store().find_element_user(WEATHERID$2, 0);
            if (target == null)
            {
                target = (org.apache.xmlbeans.XmlShort)get_store().add_element_user(WEATHERID$2);
            }
            target.set(weatherID);
        }
    }
    
    /**
     * Gets the "Desciption" element
     */
    public java.lang.String getDesciption()
    {
        synchronized (monitor())
        {
            check_orphaned();
            org.apache.xmlbeans.SimpleValue target = null;
            target = (org.apache.xmlbeans.SimpleValue)get_store().find_element_user(DESCIPTION$4, 0);
            if (target == null)
            {
                return null;
            }
            return target.getStringValue();
        }
    }
    
    /**
     * Gets (as xml) the "Desciption" element
     */
    public org.apache.xmlbeans.XmlString xgetDesciption()
    {
        synchronized (monitor())
        {
            check_orphaned();
            org.apache.xmlbeans.XmlString target = null;
            target = (org.apache.xmlbeans.XmlString)get_store().find_element_user(DESCIPTION$4, 0);
            return target;
        }
    }
    
    /**
     * True if has "Desciption" element
     */
    public boolean isSetDesciption()
    {
        synchronized (monitor())
        {
            check_orphaned();
            return get_store().count_elements(DESCIPTION$4) != 0;
        }
    }
    
    /**
     * Sets the "Desciption" element
     */
    public void setDesciption(java.lang.String desciption)
    {
        synchronized (monitor())
        {
            check_orphaned();
            org.apache.xmlbeans.SimpleValue target = null;
            target = (org.apache.xmlbeans.SimpleValue)get_store().find_element_user(DESCIPTION$4, 0);
            if (target == null)
            {
                target = (org.apache.xmlbeans.SimpleValue)get_store().add_element_user(DESCIPTION$4);
            }
            target.setStringValue(desciption);
        }
    }
    
    /**
     * Sets (as xml) the "Desciption" element
     */
    public void xsetDesciption(org.apache.xmlbeans.XmlString desciption)
    {
        synchronized (monitor())
        {
            check_orphaned();
            org.apache.xmlbeans.XmlString target = null;
            target = (org.apache.xmlbeans.XmlString)get_store().find_element_user(DESCIPTION$4, 0);
            if (target == null)
            {
                target = (org.apache.xmlbeans.XmlString)get_store().add_element_user(DESCIPTION$4);
            }
            target.set(desciption);
        }
    }
    
    /**
     * Unsets the "Desciption" element
     */
    public void unsetDesciption()
    {
        synchronized (monitor())
        {
            check_orphaned();
            get_store().remove_element(DESCIPTION$4, 0);
        }
    }
    
    /**
     * Gets the "Temperatures" element
     */
    public com.cdyne.ws.weatherws.Temp getTemperatures()
    {
        synchronized (monitor())
        {
            check_orphaned();
            com.cdyne.ws.weatherws.Temp target = null;
            target = (com.cdyne.ws.weatherws.Temp)get_store().find_element_user(TEMPERATURES$6, 0);
            if (target == null)
            {
                return null;
            }
            return target;
        }
    }
    
    /**
     * Sets the "Temperatures" element
     */
    public void setTemperatures(com.cdyne.ws.weatherws.Temp temperatures)
    {
        synchronized (monitor())
        {
            check_orphaned();
            com.cdyne.ws.weatherws.Temp target = null;
            target = (com.cdyne.ws.weatherws.Temp)get_store().find_element_user(TEMPERATURES$6, 0);
            if (target == null)
            {
                target = (com.cdyne.ws.weatherws.Temp)get_store().add_element_user(TEMPERATURES$6);
            }
            target.set(temperatures);
        }
    }
    
    /**
     * Appends and returns a new empty "Temperatures" element
     */
    public com.cdyne.ws.weatherws.Temp addNewTemperatures()
    {
        synchronized (monitor())
        {
            check_orphaned();
            com.cdyne.ws.weatherws.Temp target = null;
            target = (com.cdyne.ws.weatherws.Temp)get_store().add_element_user(TEMPERATURES$6);
            return target;
        }
    }
    
    /**
     * Gets the "ProbabilityOfPrecipiation" element
     */
    public com.cdyne.ws.weatherws.POP getProbabilityOfPrecipiation()
    {
        synchronized (monitor())
        {
            check_orphaned();
            com.cdyne.ws.weatherws.POP target = null;
            target = (com.cdyne.ws.weatherws.POP)get_store().find_element_user(PROBABILITYOFPRECIPIATION$8, 0);
            if (target == null)
            {
                return null;
            }
            return target;
        }
    }
    
    /**
     * Sets the "ProbabilityOfPrecipiation" element
     */
    public void setProbabilityOfPrecipiation(com.cdyne.ws.weatherws.POP probabilityOfPrecipiation)
    {
        synchronized (monitor())
        {
            check_orphaned();
            com.cdyne.ws.weatherws.POP target = null;
            target = (com.cdyne.ws.weatherws.POP)get_store().find_element_user(PROBABILITYOFPRECIPIATION$8, 0);
            if (target == null)
            {
                target = (com.cdyne.ws.weatherws.POP)get_store().add_element_user(PROBABILITYOFPRECIPIATION$8);
            }
            target.set(probabilityOfPrecipiation);
        }
    }
    
    /**
     * Appends and returns a new empty "ProbabilityOfPrecipiation" element
     */
    public com.cdyne.ws.weatherws.POP addNewProbabilityOfPrecipiation()
    {
        synchronized (monitor())
        {
            check_orphaned();
            com.cdyne.ws.weatherws.POP target = null;
            target = (com.cdyne.ws.weatherws.POP)get_store().add_element_user(PROBABILITYOFPRECIPIATION$8);
            return target;
        }
    }
}
