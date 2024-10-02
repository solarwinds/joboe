package com.tracelytics.instrumentation;

/**
 * Tags Thrift Transport object to provide the host and port information
 * @author Patson Luk
 *
 */
public interface ThriftTransportWithHost {
    String tvGetHost();
    int tvGetPort();
}
