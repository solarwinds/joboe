package com.tracelytics.joboe.rpc.thrift;

import com.tracelytics.joboe.BsonBufferException;
import com.tracelytics.joboe.rpc.ClientFatalException;

@SuppressWarnings("serial")
public class ThriftClientBufferException extends ClientFatalException {

    public ThriftClientBufferException(BsonBufferException cause) {
        super(cause);
    }

    public ThriftClientBufferException(String message) {
        super(message);
    }

}
