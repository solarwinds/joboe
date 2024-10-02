package com.tracelytics.joboe.rpc.thrift;

import com.tracelytics.joboe.rpc.ClientRecoverableException;

@SuppressWarnings("serial")
/**
 * Indicates exception while connecting the Thrift client
 * @author pluk
 *
 */
public class ThriftClientConnectException extends ClientRecoverableException {

    public ThriftClientConnectException(Throwable cause) {
        super(cause);
    }

    public ThriftClientConnectException(String message) {
        super(message);
    }

}
