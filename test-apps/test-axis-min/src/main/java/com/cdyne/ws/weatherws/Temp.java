/*
 * XML Type:  temp
 * Namespace: http://ws.cdyne.com/WeatherWS/
 * Java type: com.cdyne.ws.weatherws.Temp
 *
 * Automatically generated - do not modify.
 */
package com.cdyne.ws.weatherws;


/**
 * An XML temp(@http://ws.cdyne.com/WeatherWS/).
 *
 * This is a complex type.
 */
public interface Temp extends org.apache.xmlbeans.XmlObject
{
    public static final org.apache.xmlbeans.SchemaType type = (org.apache.xmlbeans.SchemaType)
        org.apache.xmlbeans.XmlBeans.typeSystemForClassLoader(Temp.class.getClassLoader(), "schemaorg_apache_xmlbeans.system.s32110FFB128B8689E7F695F15648068F").resolveHandle("tempfa0ftype");
    
    /**
     * Gets the "MorningLow" element
     */
    java.lang.String getMorningLow();
    
    /**
     * Gets (as xml) the "MorningLow" element
     */
    org.apache.xmlbeans.XmlString xgetMorningLow();
    
    /**
     * True if has "MorningLow" element
     */
    boolean isSetMorningLow();
    
    /**
     * Sets the "MorningLow" element
     */
    void setMorningLow(java.lang.String morningLow);
    
    /**
     * Sets (as xml) the "MorningLow" element
     */
    void xsetMorningLow(org.apache.xmlbeans.XmlString morningLow);
    
    /**
     * Unsets the "MorningLow" element
     */
    void unsetMorningLow();
    
    /**
     * Gets the "DaytimeHigh" element
     */
    java.lang.String getDaytimeHigh();
    
    /**
     * Gets (as xml) the "DaytimeHigh" element
     */
    org.apache.xmlbeans.XmlString xgetDaytimeHigh();
    
    /**
     * True if has "DaytimeHigh" element
     */
    boolean isSetDaytimeHigh();
    
    /**
     * Sets the "DaytimeHigh" element
     */
    void setDaytimeHigh(java.lang.String daytimeHigh);
    
    /**
     * Sets (as xml) the "DaytimeHigh" element
     */
    void xsetDaytimeHigh(org.apache.xmlbeans.XmlString daytimeHigh);
    
    /**
     * Unsets the "DaytimeHigh" element
     */
    void unsetDaytimeHigh();
    
    /**
     * A factory class with static methods for creating instances
     * of this type.
     */
    
    public static final class Factory
    {
        public static com.cdyne.ws.weatherws.Temp newInstance() {
          return (com.cdyne.ws.weatherws.Temp) org.apache.xmlbeans.XmlBeans.getContextTypeLoader().newInstance( type, null ); }
        
        public static com.cdyne.ws.weatherws.Temp newInstance(org.apache.xmlbeans.XmlOptions options) {
          return (com.cdyne.ws.weatherws.Temp) org.apache.xmlbeans.XmlBeans.getContextTypeLoader().newInstance( type, options ); }
        
        /** @param xmlAsString the string value to parse */
        public static com.cdyne.ws.weatherws.Temp parse(java.lang.String xmlAsString) throws org.apache.xmlbeans.XmlException {
          return (com.cdyne.ws.weatherws.Temp) org.apache.xmlbeans.XmlBeans.getContextTypeLoader().parse( xmlAsString, type, null ); }
        
        public static com.cdyne.ws.weatherws.Temp parse(java.lang.String xmlAsString, org.apache.xmlbeans.XmlOptions options) throws org.apache.xmlbeans.XmlException {
          return (com.cdyne.ws.weatherws.Temp) org.apache.xmlbeans.XmlBeans.getContextTypeLoader().parse( xmlAsString, type, options ); }
        
        /** @param file the file from which to load an xml document */
        public static com.cdyne.ws.weatherws.Temp parse(java.io.File file) throws org.apache.xmlbeans.XmlException, java.io.IOException {
          return (com.cdyne.ws.weatherws.Temp) org.apache.xmlbeans.XmlBeans.getContextTypeLoader().parse( file, type, null ); }
        
        public static com.cdyne.ws.weatherws.Temp parse(java.io.File file, org.apache.xmlbeans.XmlOptions options) throws org.apache.xmlbeans.XmlException, java.io.IOException {
          return (com.cdyne.ws.weatherws.Temp) org.apache.xmlbeans.XmlBeans.getContextTypeLoader().parse( file, type, options ); }
        
        public static com.cdyne.ws.weatherws.Temp parse(java.net.URL u) throws org.apache.xmlbeans.XmlException, java.io.IOException {
          return (com.cdyne.ws.weatherws.Temp) org.apache.xmlbeans.XmlBeans.getContextTypeLoader().parse( u, type, null ); }
        
        public static com.cdyne.ws.weatherws.Temp parse(java.net.URL u, org.apache.xmlbeans.XmlOptions options) throws org.apache.xmlbeans.XmlException, java.io.IOException {
          return (com.cdyne.ws.weatherws.Temp) org.apache.xmlbeans.XmlBeans.getContextTypeLoader().parse( u, type, options ); }
        
        public static com.cdyne.ws.weatherws.Temp parse(java.io.InputStream is) throws org.apache.xmlbeans.XmlException, java.io.IOException {
          return (com.cdyne.ws.weatherws.Temp) org.apache.xmlbeans.XmlBeans.getContextTypeLoader().parse( is, type, null ); }
        
        public static com.cdyne.ws.weatherws.Temp parse(java.io.InputStream is, org.apache.xmlbeans.XmlOptions options) throws org.apache.xmlbeans.XmlException, java.io.IOException {
          return (com.cdyne.ws.weatherws.Temp) org.apache.xmlbeans.XmlBeans.getContextTypeLoader().parse( is, type, options ); }
        
        public static com.cdyne.ws.weatherws.Temp parse(java.io.Reader r) throws org.apache.xmlbeans.XmlException, java.io.IOException {
          return (com.cdyne.ws.weatherws.Temp) org.apache.xmlbeans.XmlBeans.getContextTypeLoader().parse( r, type, null ); }
        
        public static com.cdyne.ws.weatherws.Temp parse(java.io.Reader r, org.apache.xmlbeans.XmlOptions options) throws org.apache.xmlbeans.XmlException, java.io.IOException {
          return (com.cdyne.ws.weatherws.Temp) org.apache.xmlbeans.XmlBeans.getContextTypeLoader().parse( r, type, options ); }
        
        public static com.cdyne.ws.weatherws.Temp parse(javax.xml.stream.XMLStreamReader sr) throws org.apache.xmlbeans.XmlException {
          return (com.cdyne.ws.weatherws.Temp) org.apache.xmlbeans.XmlBeans.getContextTypeLoader().parse( sr, type, null ); }
        
        public static com.cdyne.ws.weatherws.Temp parse(javax.xml.stream.XMLStreamReader sr, org.apache.xmlbeans.XmlOptions options) throws org.apache.xmlbeans.XmlException {
          return (com.cdyne.ws.weatherws.Temp) org.apache.xmlbeans.XmlBeans.getContextTypeLoader().parse( sr, type, options ); }
        
        public static com.cdyne.ws.weatherws.Temp parse(org.w3c.dom.Node node) throws org.apache.xmlbeans.XmlException {
          return (com.cdyne.ws.weatherws.Temp) org.apache.xmlbeans.XmlBeans.getContextTypeLoader().parse( node, type, null ); }
        
        public static com.cdyne.ws.weatherws.Temp parse(org.w3c.dom.Node node, org.apache.xmlbeans.XmlOptions options) throws org.apache.xmlbeans.XmlException {
          return (com.cdyne.ws.weatherws.Temp) org.apache.xmlbeans.XmlBeans.getContextTypeLoader().parse( node, type, options ); }
        
        /** @deprecated {@link org.apache.xmlbeans.xml.stream.XMLInputStream} */
        public static com.cdyne.ws.weatherws.Temp parse(org.apache.xmlbeans.xml.stream.XMLInputStream xis) throws org.apache.xmlbeans.XmlException, org.apache.xmlbeans.xml.stream.XMLStreamException {
          return (com.cdyne.ws.weatherws.Temp) org.apache.xmlbeans.XmlBeans.getContextTypeLoader().parse( xis, type, null ); }
        
        /** @deprecated {@link org.apache.xmlbeans.xml.stream.XMLInputStream} */
        public static com.cdyne.ws.weatherws.Temp parse(org.apache.xmlbeans.xml.stream.XMLInputStream xis, org.apache.xmlbeans.XmlOptions options) throws org.apache.xmlbeans.XmlException, org.apache.xmlbeans.xml.stream.XMLStreamException {
          return (com.cdyne.ws.weatherws.Temp) org.apache.xmlbeans.XmlBeans.getContextTypeLoader().parse( xis, type, options ); }
        
        /** @deprecated {@link org.apache.xmlbeans.xml.stream.XMLInputStream} */
        public static org.apache.xmlbeans.xml.stream.XMLInputStream newValidatingXMLInputStream(org.apache.xmlbeans.xml.stream.XMLInputStream xis) throws org.apache.xmlbeans.XmlException, org.apache.xmlbeans.xml.stream.XMLStreamException {
          return org.apache.xmlbeans.XmlBeans.getContextTypeLoader().newValidatingXMLInputStream( xis, type, null ); }
        
        /** @deprecated {@link org.apache.xmlbeans.xml.stream.XMLInputStream} */
        public static org.apache.xmlbeans.xml.stream.XMLInputStream newValidatingXMLInputStream(org.apache.xmlbeans.xml.stream.XMLInputStream xis, org.apache.xmlbeans.XmlOptions options) throws org.apache.xmlbeans.XmlException, org.apache.xmlbeans.xml.stream.XMLStreamException {
          return org.apache.xmlbeans.XmlBeans.getContextTypeLoader().newValidatingXMLInputStream( xis, type, options ); }
        
        private Factory() { } // No instance of this class allowed
    }
}
