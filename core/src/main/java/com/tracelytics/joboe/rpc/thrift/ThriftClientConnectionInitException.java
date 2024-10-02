package com.tracelytics.joboe.rpc.thrift;

import com.tracelytics.joboe.rpc.ClientRecoverableException;

@SuppressWarnings("serial")
/**
 * Exception when client is having problem send out the "connection init" message
 */
public class ThriftClientConnectionInitException extends ClientRecoverableException {

    public ThriftClientConnectionInitException(Throwable cause) {
        super(cause);
    }

    public ThriftClientConnectionInitException(String message) {
        super(message);
    }

}
