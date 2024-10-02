package com.tracelytics.instrumentation.ejb;

/**
 * Allows setting/getting of host and port. Also exposes the getRemoteEndpointName method
 * @author Patson Luk
 *
 */
public interface JbossConnection {
    void tvSetHost(String host);
    String tvGetHost();
    void tvSetPort(int port);    
    int tvGetPort();
    String getRemoteEndpointName(); //existing method provided by JBoss
}
