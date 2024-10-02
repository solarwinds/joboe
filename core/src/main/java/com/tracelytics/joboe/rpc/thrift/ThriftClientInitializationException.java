package com.tracelytics.joboe.rpc.thrift;

import com.tracelytics.joboe.rpc.ClientFatalException;

@SuppressWarnings("serial")
public class ThriftClientInitializationException extends ClientFatalException {

    public ThriftClientInitializationException(Throwable cause) {
        super(cause);
    }

    public ThriftClientInitializationException(String message) {
        super(message);
    }

}
