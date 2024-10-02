/**
 * HelloWorldImplService.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.appoptics.test.ws;

public interface HelloWorldImplService extends javax.xml.rpc.Service {
    public java.lang.String getHelloWorldImplPortAddress();

    public com.appoptics.test.ws.HelloWorld getHelloWorldImplPort() throws javax.xml.rpc.ServiceException;

    public com.appoptics.test.ws.HelloWorld getHelloWorldImplPort(java.net.URL portAddress) throws javax.xml.rpc.ServiceException;
}
