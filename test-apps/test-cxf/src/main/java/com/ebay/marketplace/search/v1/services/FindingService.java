package com.ebay.marketplace.search.v1.services;

import java.net.MalformedURLException;
import java.net.URL;
import javax.xml.namespace.QName;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceFeature;
import javax.xml.ws.Service;

import com.cdyne.cxf.ws.weatherws.Weather;

/**
 * 
 * 		
 *
 * This class was generated by Apache CXF 2.7.3
 * 2013-04-30T17:19:56.521-07:00
 * Generated source version: 2.7.3
 * 
 */
@WebServiceClient(name = "FindingService", 
                  wsdlLocation = "classpath:wsdl/FindingService.wsdl",
                  targetNamespace = "http://www.ebay.com/marketplace/search/v1/services") 
public class FindingService extends Service {

    public final static URL WSDL_LOCATION;

    public final static QName SERVICE = new QName("http://www.ebay.com/marketplace/search/v1/services", "FindingService");
    public final static QName FindingServiceSOAPPort = new QName("http://www.ebay.com/marketplace/search/v1/services", "FindingServiceSOAPPort");
    static {
        URL url = FindingService.class.getClassLoader().getResource("wsdl/FindingService.wsdl");
        if (url == null) {
            System.err.println("Can not initialize the default wsdl from classpath:wsdl/FindingService.wsdl");
            // e.printStackTrace();
        }
        WSDL_LOCATION = url;
    }

    public FindingService(URL wsdlLocation) {
        super(wsdlLocation, SERVICE);
    }

    public FindingService(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    public FindingService() {
        super(WSDL_LOCATION, SERVICE);
    }
    
    //This constructor requires JAX-WS API 2.2. You will need to endorse the 2.2
    //API jar or re-run wsdl2java with "-frontend jaxws21" to generate JAX-WS 2.1
    //compliant code instead.
//    public FindingService(WebServiceFeature ... features) {
//        super(WSDL_LOCATION, SERVICE, features);
//    }
//
//    //This constructor requires JAX-WS API 2.2. You will need to endorse the 2.2
//    //API jar or re-run wsdl2java with "-frontend jaxws21" to generate JAX-WS 2.1
//    //compliant code instead.
//    public FindingService(URL wsdlLocation, WebServiceFeature ... features) {
//        super(wsdlLocation, SERVICE, features);
//    }
//
//    //This constructor requires JAX-WS API 2.2. You will need to endorse the 2.2
//    //API jar or re-run wsdl2java with "-frontend jaxws21" to generate JAX-WS 2.1
//    //compliant code instead.
//    public FindingService(URL wsdlLocation, QName serviceName, WebServiceFeature ... features) {
//        super(wsdlLocation, serviceName, features);
//    }

    /**
     *
     * @return
     *     returns FindingServicePortType
     */
    @WebEndpoint(name = "FindingServiceSOAPPort")
    public FindingServicePortType getFindingServiceSOAPPort() {
        return super.getPort(FindingServiceSOAPPort, FindingServicePortType.class);
    }

    /**
     * 
     * @param features
     *     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns FindingServicePortType
     */
    @WebEndpoint(name = "FindingServiceSOAPPort")
    public FindingServicePortType getFindingServiceSOAPPort(WebServiceFeature... features) {
        return super.getPort(FindingServiceSOAPPort, FindingServicePortType.class, features);
    }

}
