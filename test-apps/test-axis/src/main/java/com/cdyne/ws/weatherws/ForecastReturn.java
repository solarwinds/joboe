/*
 * XML Type:  ForecastReturn
 * Namespace: http://ws.cdyne.com/WeatherWS/
 * Java type: com.cdyne.ws.weatherws.ForecastReturn
 *
 * Automatically generated - do not modify.
 */
package com.cdyne.ws.weatherws;


/**
 * An XML ForecastReturn(@http://ws.cdyne.com/WeatherWS/).
 *
 * This is a complex type.
 */
public interface ForecastReturn extends org.apache.xmlbeans.XmlObject
{
    public static final org.apache.xmlbeans.SchemaType type = (org.apache.xmlbeans.SchemaType)
        org.apache.xmlbeans.XmlBeans.typeSystemForClassLoader(ForecastReturn.class.getClassLoader(), "schemaorg_apache_xmlbeans.system.s9BF98892DFFBF58E7682C993B0C16982").resolveHandle("forecastreturnf318type");
    
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
     * Gets the "ForecastResult" element
     */
    com.cdyne.ws.weatherws.ArrayOfForecast getForecastResult();
    
    /**
     * True if has "ForecastResult" element
     */
    boolean isSetForecastResult();
    
    /**
     * Sets the "ForecastResult" element
     */
    void setForecastResult(com.cdyne.ws.weatherws.ArrayOfForecast forecastResult);
    
    /**
     * Appends and returns a new empty "ForecastResult" element
     */
    com.cdyne.ws.weatherws.ArrayOfForecast addNewForecastResult();
    
    /**
     * Unsets the "ForecastResult" element
     */
    void unsetForecastResult();
    
    /**
     * A factory class with static methods for creating instances
     * of this type.
     */
    
    public static final class Factory
    {
        public static com.cdyne.ws.weatherws.ForecastReturn newInstance() {
          return (com.cdyne.ws.weatherws.ForecastReturn) org.apache.xmlbeans.XmlBeans.getContextTypeLoader().newInstance( type, null ); }
        
        public static com.cdyne.ws.weatherws.ForecastReturn newInstance(org.apache.xmlbeans.XmlOptions options) {
          return (com.cdyne.ws.weatherws.ForecastReturn) org.apache.xmlbeans.XmlBeans.getContextTypeLoader().newInstance( type, options ); }
        
        /** @param xmlAsString the string value to parse */
        public static com.cdyne.ws.weatherws.ForecastReturn parse(java.lang.String xmlAsString) throws org.apache.xmlbeans.XmlException {
          return (com.cdyne.ws.weatherws.ForecastReturn) org.apache.xmlbeans.XmlBeans.getContextTypeLoader().parse( xmlAsString, type, null ); }
        
        public static com.cdyne.ws.weatherws.ForecastReturn parse(java.lang.String xmlAsString, org.apache.xmlbeans.XmlOptions options) throws org.apache.xmlbeans.XmlException {
          return (com.cdyne.ws.weatherws.ForecastReturn) org.apache.xmlbeans.XmlBeans.getContextTypeLoader().parse( xmlAsString, type, options ); }
        
        /** @param file the file from which to load an xml document */
        public static com.cdyne.ws.weatherws.ForecastReturn parse(java.io.File file) throws org.apache.xmlbeans.XmlException, java.io.IOException {
          return (com.cdyne.ws.weatherws.ForecastReturn) org.apache.xmlbeans.XmlBeans.getContextTypeLoader().parse( file, type, null ); }
        
        public static com.cdyne.ws.weatherws.ForecastReturn parse(java.io.File file, org.apache.xmlbeans.XmlOptions options) throws org.apache.xmlbeans.XmlException, java.io.IOException {
          return (com.cdyne.ws.weatherws.ForecastReturn) org.apache.xmlbeans.XmlBeans.getContextTypeLoader().parse( file, type, options ); }
        
        public static com.cdyne.ws.weatherws.ForecastReturn parse(java.net.URL u) throws org.apache.xmlbeans.XmlException, java.io.IOException {
          return (com.cdyne.ws.weatherws.ForecastReturn) org.apache.xmlbeans.XmlBeans.getContextTypeLoader().parse( u, type, null ); }
        
        public static com.cdyne.ws.weatherws.ForecastReturn parse(java.net.URL u, org.apache.xmlbeans.XmlOptions options) throws org.apache.xmlbeans.XmlException, java.io.IOException {
          return (com.cdyne.ws.weatherws.ForecastReturn) org.apache.xmlbeans.XmlBeans.getContextTypeLoader().parse( u, type, options ); }
        
        public static com.cdyne.ws.weatherws.ForecastReturn parse(java.io.InputStream is) throws org.apache.xmlbeans.XmlException, java.io.IOException {
          return (com.cdyne.ws.weatherws.ForecastReturn) org.apache.xmlbeans.XmlBeans.getContextTypeLoader().parse( is, type, null ); }
        
        public static com.cdyne.ws.weatherws.ForecastReturn parse(java.io.InputStream is, org.apache.xmlbeans.XmlOptions options) throws org.apache.xmlbeans.XmlException, java.io.IOException {
          return (com.cdyne.ws.weatherws.ForecastReturn) org.apache.xmlbeans.XmlBeans.getContextTypeLoader().parse( is, type, options ); }
        
        public static com.cdyne.ws.weatherws.ForecastReturn parse(java.io.Reader r) throws org.apache.xmlbeans.XmlException, java.io.IOException {
          return (com.cdyne.ws.weatherws.ForecastReturn) org.apache.xmlbeans.XmlBeans.getContextTypeLoader().parse( r, type, null ); }
        
        public static com.cdyne.ws.weatherws.ForecastReturn parse(java.io.Reader r, org.apache.xmlbeans.XmlOptions options) throws org.apache.xmlbeans.XmlException, java.io.IOException {
          return (com.cdyne.ws.weatherws.ForecastReturn) org.apache.xmlbeans.XmlBeans.getContextTypeLoader().parse( r, type, options ); }
        
        public static com.cdyne.ws.weatherws.ForecastReturn parse(javax.xml.stream.XMLStreamReader sr) throws org.apache.xmlbeans.XmlException {
          return (com.cdyne.ws.weatherws.ForecastReturn) org.apache.xmlbeans.XmlBeans.getContextTypeLoader().parse( sr, type, null ); }
        
        public static com.cdyne.ws.weatherws.ForecastReturn parse(javax.xml.stream.XMLStreamReader sr, org.apache.xmlbeans.XmlOptions options) throws org.apache.xmlbeans.XmlException {
          return (com.cdyne.ws.weatherws.ForecastReturn) org.apache.xmlbeans.XmlBeans.getContextTypeLoader().parse( sr, type, options ); }
        
        public static com.cdyne.ws.weatherws.ForecastReturn parse(org.w3c.dom.Node node) throws org.apache.xmlbeans.XmlException {
          return (com.cdyne.ws.weatherws.ForecastReturn) org.apache.xmlbeans.XmlBeans.getContextTypeLoader().parse( node, type, null ); }
        
        public static com.cdyne.ws.weatherws.ForecastReturn parse(org.w3c.dom.Node node, org.apache.xmlbeans.XmlOptions options) throws org.apache.xmlbeans.XmlException {
          return (com.cdyne.ws.weatherws.ForecastReturn) org.apache.xmlbeans.XmlBeans.getContextTypeLoader().parse( node, type, options ); }
        
        /** @deprecated {@link org.apache.xmlbeans.xml.stream.XMLInputStream} */
        public static com.cdyne.ws.weatherws.ForecastReturn parse(org.apache.xmlbeans.xml.stream.XMLInputStream xis) throws org.apache.xmlbeans.XmlException, org.apache.xmlbeans.xml.stream.XMLStreamException {
          return (com.cdyne.ws.weatherws.ForecastReturn) org.apache.xmlbeans.XmlBeans.getContextTypeLoader().parse( xis, type, null ); }
        
        /** @deprecated {@link org.apache.xmlbeans.xml.stream.XMLInputStream} */
        public static com.cdyne.ws.weatherws.ForecastReturn parse(org.apache.xmlbeans.xml.stream.XMLInputStream xis, org.apache.xmlbeans.XmlOptions options) throws org.apache.xmlbeans.XmlException, org.apache.xmlbeans.xml.stream.XMLStreamException {
          return (com.cdyne.ws.weatherws.ForecastReturn) org.apache.xmlbeans.XmlBeans.getContextTypeLoader().parse( xis, type, options ); }
        
        /** @deprecated {@link org.apache.xmlbeans.xml.stream.XMLInputStream} */
        public static org.apache.xmlbeans.xml.stream.XMLInputStream newValidatingXMLInputStream(org.apache.xmlbeans.xml.stream.XMLInputStream xis) throws org.apache.xmlbeans.XmlException, org.apache.xmlbeans.xml.stream.XMLStreamException {
          return org.apache.xmlbeans.XmlBeans.getContextTypeLoader().newValidatingXMLInputStream( xis, type, null ); }
        
        /** @deprecated {@link org.apache.xmlbeans.xml.stream.XMLInputStream} */
        public static org.apache.xmlbeans.xml.stream.XMLInputStream newValidatingXMLInputStream(org.apache.xmlbeans.xml.stream.XMLInputStream xis, org.apache.xmlbeans.XmlOptions options) throws org.apache.xmlbeans.XmlException, org.apache.xmlbeans.xml.stream.XMLStreamException {
          return org.apache.xmlbeans.XmlBeans.getContextTypeLoader().newValidatingXMLInputStream( xis, type, options ); }
        
        private Factory() { } // No instance of this class allowed
    }
}
