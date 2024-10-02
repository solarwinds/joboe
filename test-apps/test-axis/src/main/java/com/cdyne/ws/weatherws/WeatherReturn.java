/*
 * XML Type:  WeatherReturn
 * Namespace: http://ws.cdyne.com/WeatherWS/
 * Java type: com.cdyne.ws.weatherws.WeatherReturn
 *
 * Automatically generated - do not modify.
 */
package com.cdyne.ws.weatherws;


/**
 * An XML WeatherReturn(@http://ws.cdyne.com/WeatherWS/).
 *
 * This is a complex type.
 */
public interface WeatherReturn extends org.apache.xmlbeans.XmlObject
{
    public static final org.apache.xmlbeans.SchemaType type = (org.apache.xmlbeans.SchemaType)
        org.apache.xmlbeans.XmlBeans.typeSystemForClassLoader(WeatherReturn.class.getClassLoader(), "schemaorg_apache_xmlbeans.system.s9BF98892DFFBF58E7682C993B0C16982").resolveHandle("weatherreturna5b1type");
    
    /**
     * Gets the "Success" element
     */
    boolean getSuccess();
    
    /**
     * Gets (as xml) the "Success" element
     */
    org.apache.xmlbeans.XmlBoolean xgetSuccess();
    
    /**
     * Sets the "Success" element
     */
    void setSuccess(boolean success);
    
    /**
     * Sets (as xml) the "Success" element
     */
    void xsetSuccess(org.apache.xmlbeans.XmlBoolean success);
    
    /**
     * Gets the "ResponseText" element
     */
    java.lang.String getResponseText();
    
    /**
     * Gets (as xml) the "ResponseText" element
     */
    org.apache.xmlbeans.XmlString xgetResponseText();
    
    /**
     * True if has "ResponseText" element
     */
    boolean isSetResponseText();
    
    /**
     * Sets the "ResponseText" element
     */
    void setResponseText(java.lang.String responseText);
    
    /**
     * Sets (as xml) the "ResponseText" element
     */
    void xsetResponseText(org.apache.xmlbeans.XmlString responseText);
    
    /**
     * Unsets the "ResponseText" element
     */
    void unsetResponseText();
    
    /**
     * Gets the "State" element
     */
    java.lang.String getState();
    
    /**
     * Gets (as xml) the "State" element
     */
    org.apache.xmlbeans.XmlString xgetState();
    
    /**
     * True if has "State" element
     */
    boolean isSetState();
    
    /**
     * Sets the "State" element
     */
    void setState(java.lang.String state);
    
    /**
     * Sets (as xml) the "State" element
     */
    void xsetState(org.apache.xmlbeans.XmlString state);
    
    /**
     * Unsets the "State" element
     */
    void unsetState();
    
    /**
     * Gets the "City" element
     */
    java.lang.String getCity();
    
    /**
     * Gets (as xml) the "City" element
     */
    org.apache.xmlbeans.XmlString xgetCity();
    
    /**
     * True if has "City" element
     */
    boolean isSetCity();
    
    /**
     * Sets the "City" element
     */
    void setCity(java.lang.String city);
    
    /**
     * Sets (as xml) the "City" element
     */
    void xsetCity(org.apache.xmlbeans.XmlString city);
    
    /**
     * Unsets the "City" element
     */
    void unsetCity();
    
    /**
     * Gets the "WeatherStationCity" element
     */
    java.lang.String getWeatherStationCity();
    
    /**
     * Gets (as xml) the "WeatherStationCity" element
     */
    org.apache.xmlbeans.XmlString xgetWeatherStationCity();
    
    /**
     * True if has "WeatherStationCity" element
     */
    boolean isSetWeatherStationCity();
    
    /**
     * Sets the "WeatherStationCity" element
     */
    void setWeatherStationCity(java.lang.String weatherStationCity);
    
    /**
     * Sets (as xml) the "WeatherStationCity" element
     */
    void xsetWeatherStationCity(org.apache.xmlbeans.XmlString weatherStationCity);
    
    /**
     * Unsets the "WeatherStationCity" element
     */
    void unsetWeatherStationCity();
    
    /**
     * Gets the "WeatherID" element
     */
    short getWeatherID();
    
    /**
     * Gets (as xml) the "WeatherID" element
     */
    org.apache.xmlbeans.XmlShort xgetWeatherID();
    
    /**
     * Sets the "WeatherID" element
     */
    void setWeatherID(short weatherID);
    
    /**
     * Sets (as xml) the "WeatherID" element
     */
    void xsetWeatherID(org.apache.xmlbeans.XmlShort weatherID);
    
    /**
     * Gets the "Description" element
     */
    java.lang.String getDescription();
    
    /**
     * Gets (as xml) the "Description" element
     */
    org.apache.xmlbeans.XmlString xgetDescription();
    
    /**
     * True if has "Description" element
     */
    boolean isSetDescription();
    
    /**
     * Sets the "Description" element
     */
    void setDescription(java.lang.String description);
    
    /**
     * Sets (as xml) the "Description" element
     */
    void xsetDescription(org.apache.xmlbeans.XmlString description);
    
    /**
     * Unsets the "Description" element
     */
    void unsetDescription();
    
    /**
     * Gets the "Temperature" element
     */
    java.lang.String getTemperature();
    
    /**
     * Gets (as xml) the "Temperature" element
     */
    org.apache.xmlbeans.XmlString xgetTemperature();
    
    /**
     * True if has "Temperature" element
     */
    boolean isSetTemperature();
    
    /**
     * Sets the "Temperature" element
     */
    void setTemperature(java.lang.String temperature);
    
    /**
     * Sets (as xml) the "Temperature" element
     */
    void xsetTemperature(org.apache.xmlbeans.XmlString temperature);
    
    /**
     * Unsets the "Temperature" element
     */
    void unsetTemperature();
    
    /**
     * Gets the "RelativeHumidity" element
     */
    java.lang.String getRelativeHumidity();
    
    /**
     * Gets (as xml) the "RelativeHumidity" element
     */
    org.apache.xmlbeans.XmlString xgetRelativeHumidity();
    
    /**
     * True if has "RelativeHumidity" element
     */
    boolean isSetRelativeHumidity();
    
    /**
     * Sets the "RelativeHumidity" element
     */
    void setRelativeHumidity(java.lang.String relativeHumidity);
    
    /**
     * Sets (as xml) the "RelativeHumidity" element
     */
    void xsetRelativeHumidity(org.apache.xmlbeans.XmlString relativeHumidity);
    
    /**
     * Unsets the "RelativeHumidity" element
     */
    void unsetRelativeHumidity();
    
    /**
     * Gets the "Wind" element
     */
    java.lang.String getWind();
    
    /**
     * Gets (as xml) the "Wind" element
     */
    org.apache.xmlbeans.XmlString xgetWind();
    
    /**
     * True if has "Wind" element
     */
    boolean isSetWind();
    
    /**
     * Sets the "Wind" element
     */
    void setWind(java.lang.String wind);
    
    /**
     * Sets (as xml) the "Wind" element
     */
    void xsetWind(org.apache.xmlbeans.XmlString wind);
    
    /**
     * Unsets the "Wind" element
     */
    void unsetWind();
    
    /**
     * Gets the "Pressure" element
     */
    java.lang.String getPressure();
    
    /**
     * Gets (as xml) the "Pressure" element
     */
    org.apache.xmlbeans.XmlString xgetPressure();
    
    /**
     * True if has "Pressure" element
     */
    boolean isSetPressure();
    
    /**
     * Sets the "Pressure" element
     */
    void setPressure(java.lang.String pressure);
    
    /**
     * Sets (as xml) the "Pressure" element
     */
    void xsetPressure(org.apache.xmlbeans.XmlString pressure);
    
    /**
     * Unsets the "Pressure" element
     */
    void unsetPressure();
    
    /**
     * Gets the "Visibility" element
     */
    java.lang.String getVisibility();
    
    /**
     * Gets (as xml) the "Visibility" element
     */
    org.apache.xmlbeans.XmlString xgetVisibility();
    
    /**
     * True if has "Visibility" element
     */
    boolean isSetVisibility();
    
    /**
     * Sets the "Visibility" element
     */
    void setVisibility(java.lang.String visibility);
    
    /**
     * Sets (as xml) the "Visibility" element
     */
    void xsetVisibility(org.apache.xmlbeans.XmlString visibility);
    
    /**
     * Unsets the "Visibility" element
     */
    void unsetVisibility();
    
    /**
     * Gets the "WindChill" element
     */
    java.lang.String getWindChill();
    
    /**
     * Gets (as xml) the "WindChill" element
     */
    org.apache.xmlbeans.XmlString xgetWindChill();
    
    /**
     * True if has "WindChill" element
     */
    boolean isSetWindChill();
    
    /**
     * Sets the "WindChill" element
     */
    void setWindChill(java.lang.String windChill);
    
    /**
     * Sets (as xml) the "WindChill" element
     */
    void xsetWindChill(org.apache.xmlbeans.XmlString windChill);
    
    /**
     * Unsets the "WindChill" element
     */
    void unsetWindChill();
    
    /**
     * Gets the "Remarks" element
     */
    java.lang.String getRemarks();
    
    /**
     * Gets (as xml) the "Remarks" element
     */
    org.apache.xmlbeans.XmlString xgetRemarks();
    
    /**
     * True if has "Remarks" element
     */
    boolean isSetRemarks();
    
    /**
     * Sets the "Remarks" element
     */
    void setRemarks(java.lang.String remarks);
    
    /**
     * Sets (as xml) the "Remarks" element
     */
    void xsetRemarks(org.apache.xmlbeans.XmlString remarks);
    
    /**
     * Unsets the "Remarks" element
     */
    void unsetRemarks();
    
    /**
     * A factory class with static methods for creating instances
     * of this type.
     */
    
    public static final class Factory
    {
        public static com.cdyne.ws.weatherws.WeatherReturn newInstance() {
          return (com.cdyne.ws.weatherws.WeatherReturn) org.apache.xmlbeans.XmlBeans.getContextTypeLoader().newInstance( type, null ); }
        
        public static com.cdyne.ws.weatherws.WeatherReturn newInstance(org.apache.xmlbeans.XmlOptions options) {
          return (com.cdyne.ws.weatherws.WeatherReturn) org.apache.xmlbeans.XmlBeans.getContextTypeLoader().newInstance( type, options ); }
        
        /** @param xmlAsString the string value to parse */
        public static com.cdyne.ws.weatherws.WeatherReturn parse(java.lang.String xmlAsString) throws org.apache.xmlbeans.XmlException {
          return (com.cdyne.ws.weatherws.WeatherReturn) org.apache.xmlbeans.XmlBeans.getContextTypeLoader().parse( xmlAsString, type, null ); }
        
        public static com.cdyne.ws.weatherws.WeatherReturn parse(java.lang.String xmlAsString, org.apache.xmlbeans.XmlOptions options) throws org.apache.xmlbeans.XmlException {
          return (com.cdyne.ws.weatherws.WeatherReturn) org.apache.xmlbeans.XmlBeans.getContextTypeLoader().parse( xmlAsString, type, options ); }
        
        /** @param file the file from which to load an xml document */
        public static com.cdyne.ws.weatherws.WeatherReturn parse(java.io.File file) throws org.apache.xmlbeans.XmlException, java.io.IOException {
          return (com.cdyne.ws.weatherws.WeatherReturn) org.apache.xmlbeans.XmlBeans.getContextTypeLoader().parse( file, type, null ); }
        
        public static com.cdyne.ws.weatherws.WeatherReturn parse(java.io.File file, org.apache.xmlbeans.XmlOptions options) throws org.apache.xmlbeans.XmlException, java.io.IOException {
          return (com.cdyne.ws.weatherws.WeatherReturn) org.apache.xmlbeans.XmlBeans.getContextTypeLoader().parse( file, type, options ); }
        
        public static com.cdyne.ws.weatherws.WeatherReturn parse(java.net.URL u) throws org.apache.xmlbeans.XmlException, java.io.IOException {
          return (com.cdyne.ws.weatherws.WeatherReturn) org.apache.xmlbeans.XmlBeans.getContextTypeLoader().parse( u, type, null ); }
        
        public static com.cdyne.ws.weatherws.WeatherReturn parse(java.net.URL u, org.apache.xmlbeans.XmlOptions options) throws org.apache.xmlbeans.XmlException, java.io.IOException {
          return (com.cdyne.ws.weatherws.WeatherReturn) org.apache.xmlbeans.XmlBeans.getContextTypeLoader().parse( u, type, options ); }
        
        public static com.cdyne.ws.weatherws.WeatherReturn parse(java.io.InputStream is) throws org.apache.xmlbeans.XmlException, java.io.IOException {
          return (com.cdyne.ws.weatherws.WeatherReturn) org.apache.xmlbeans.XmlBeans.getContextTypeLoader().parse( is, type, null ); }
        
        public static com.cdyne.ws.weatherws.WeatherReturn parse(java.io.InputStream is, org.apache.xmlbeans.XmlOptions options) throws org.apache.xmlbeans.XmlException, java.io.IOException {
          return (com.cdyne.ws.weatherws.WeatherReturn) org.apache.xmlbeans.XmlBeans.getContextTypeLoader().parse( is, type, options ); }
        
        public static com.cdyne.ws.weatherws.WeatherReturn parse(java.io.Reader r) throws org.apache.xmlbeans.XmlException, java.io.IOException {
          return (com.cdyne.ws.weatherws.WeatherReturn) org.apache.xmlbeans.XmlBeans.getContextTypeLoader().parse( r, type, null ); }
        
        public static com.cdyne.ws.weatherws.WeatherReturn parse(java.io.Reader r, org.apache.xmlbeans.XmlOptions options) throws org.apache.xmlbeans.XmlException, java.io.IOException {
          return (com.cdyne.ws.weatherws.WeatherReturn) org.apache.xmlbeans.XmlBeans.getContextTypeLoader().parse( r, type, options ); }
        
        public static com.cdyne.ws.weatherws.WeatherReturn parse(javax.xml.stream.XMLStreamReader sr) throws org.apache.xmlbeans.XmlException {
          return (com.cdyne.ws.weatherws.WeatherReturn) org.apache.xmlbeans.XmlBeans.getContextTypeLoader().parse( sr, type, null ); }
        
        public static com.cdyne.ws.weatherws.WeatherReturn parse(javax.xml.stream.XMLStreamReader sr, org.apache.xmlbeans.XmlOptions options) throws org.apache.xmlbeans.XmlException {
          return (com.cdyne.ws.weatherws.WeatherReturn) org.apache.xmlbeans.XmlBeans.getContextTypeLoader().parse( sr, type, options ); }
        
        public static com.cdyne.ws.weatherws.WeatherReturn parse(org.w3c.dom.Node node) throws org.apache.xmlbeans.XmlException {
          return (com.cdyne.ws.weatherws.WeatherReturn) org.apache.xmlbeans.XmlBeans.getContextTypeLoader().parse( node, type, null ); }
        
        public static com.cdyne.ws.weatherws.WeatherReturn parse(org.w3c.dom.Node node, org.apache.xmlbeans.XmlOptions options) throws org.apache.xmlbeans.XmlException {
          return (com.cdyne.ws.weatherws.WeatherReturn) org.apache.xmlbeans.XmlBeans.getContextTypeLoader().parse( node, type, options ); }
        
        /** @deprecated {@link org.apache.xmlbeans.xml.stream.XMLInputStream} */
        public static com.cdyne.ws.weatherws.WeatherReturn parse(org.apache.xmlbeans.xml.stream.XMLInputStream xis) throws org.apache.xmlbeans.XmlException, org.apache.xmlbeans.xml.stream.XMLStreamException {
          return (com.cdyne.ws.weatherws.WeatherReturn) org.apache.xmlbeans.XmlBeans.getContextTypeLoader().parse( xis, type, null ); }
        
        /** @deprecated {@link org.apache.xmlbeans.xml.stream.XMLInputStream} */
        public static com.cdyne.ws.weatherws.WeatherReturn parse(org.apache.xmlbeans.xml.stream.XMLInputStream xis, org.apache.xmlbeans.XmlOptions options) throws org.apache.xmlbeans.XmlException, org.apache.xmlbeans.xml.stream.XMLStreamException {
          return (com.cdyne.ws.weatherws.WeatherReturn) org.apache.xmlbeans.XmlBeans.getContextTypeLoader().parse( xis, type, options ); }
        
        /** @deprecated {@link org.apache.xmlbeans.xml.stream.XMLInputStream} */
        public static org.apache.xmlbeans.xml.stream.XMLInputStream newValidatingXMLInputStream(org.apache.xmlbeans.xml.stream.XMLInputStream xis) throws org.apache.xmlbeans.XmlException, org.apache.xmlbeans.xml.stream.XMLStreamException {
          return org.apache.xmlbeans.XmlBeans.getContextTypeLoader().newValidatingXMLInputStream( xis, type, null ); }
        
        /** @deprecated {@link org.apache.xmlbeans.xml.stream.XMLInputStream} */
        public static org.apache.xmlbeans.xml.stream.XMLInputStream newValidatingXMLInputStream(org.apache.xmlbeans.xml.stream.XMLInputStream xis, org.apache.xmlbeans.XmlOptions options) throws org.apache.xmlbeans.XmlException, org.apache.xmlbeans.xml.stream.XMLStreamException {
          return org.apache.xmlbeans.XmlBeans.getContextTypeLoader().newValidatingXMLInputStream( xis, type, options ); }
        
        private Factory() { } // No instance of this class allowed
    }
}
