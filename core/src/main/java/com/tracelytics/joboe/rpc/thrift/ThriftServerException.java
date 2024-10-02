package com.tracelytics.joboe.rpc.thrift;

import com.appoptics.ext.org.apache.thrift.TException;
import com.tracelytics.joboe.rpc.ClientRecoverableException;

/**
 * Exception from the thrift server. It could be connection exception or some exception is thrown on the server side
 * while processing the RPC operation
 */
public class ThriftServerException extends ClientRecoverableException {

    public ThriftServerException(TException cause) {
        super(cause);
    }
}
