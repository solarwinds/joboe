
package com.ebay.marketplace.search.v1.services;

import java.net.MalformedURLException;
import java.net.URL;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.WebServiceFeature;


/**
 * This class was generated by the JAX-WS RI.
 * JAX-WS RI 2.2.4-b01
 * Generated source version: 2.2
 * 
 */
@WebServiceClient(name = "FindingService", targetNamespace = "http://www.ebay.com/marketplace/search/v1/services", wsdlLocation = "http://developer.ebay.com/webservices/finding/latest/FindingService.wsdl")
public class FindingService
    extends Service
{

    private final static URL FINDINGSERVICE_WSDL_LOCATION;
    private final static WebServiceException FINDINGSERVICE_EXCEPTION;
    private final static QName FINDINGSERVICE_QNAME = new QName("http://www.ebay.com/marketplace/search/v1/services", "FindingService");

    static {
        URL url = null;
        WebServiceException e = null;
        try {
            url = new URL("http://developer.ebay.com/webservices/finding/latest/FindingService.wsdl");
        } catch (MalformedURLException ex) {
            e = new WebServiceException(ex);
        }
        FINDINGSERVICE_WSDL_LOCATION = url;
        FINDINGSERVICE_EXCEPTION = e;
    }

    public FindingService() {
        super(__getWsdlLocation(), FINDINGSERVICE_QNAME);
    }

    public FindingService(WebServiceFeature... features) {
        super(__getWsdlLocation(), FINDINGSERVICE_QNAME, features);
    }

    public FindingService(URL wsdlLocation) {
        super(wsdlLocation, FINDINGSERVICE_QNAME);
    }

    public FindingService(URL wsdlLocation, WebServiceFeature... features) {
        super(wsdlLocation, FINDINGSERVICE_QNAME, features);
    }

    public FindingService(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    public FindingService(URL wsdlLocation, QName serviceName, WebServiceFeature... features) {
        super(wsdlLocation, serviceName, features);
    }

    /**
     * 
     * @return
     *     returns FindingServicePortType
     */
    @WebEndpoint(name = "FindingServiceSOAPPort")
    public FindingServicePortType getFindingServiceSOAPPort() {
        return super.getPort(new QName("http://www.ebay.com/marketplace/search/v1/services", "FindingServiceSOAPPort"), FindingServicePortType.class);
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
        return super.getPort(new QName("http://www.ebay.com/marketplace/search/v1/services", "FindingServiceSOAPPort"), FindingServicePortType.class, features);
    }

    private static URL __getWsdlLocation() {
        if (FINDINGSERVICE_EXCEPTION!= null) {
            throw FINDINGSERVICE_EXCEPTION;
        }
        return FINDINGSERVICE_WSDL_LOCATION;
    }

}
