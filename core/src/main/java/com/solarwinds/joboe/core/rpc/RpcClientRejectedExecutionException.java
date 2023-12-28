package com.solarwinds.joboe.core.rpc;

import java.util.concurrent.RejectedExecutionException;

/**
 * Indicates collector RPC client rejects the operation before making any actual outbound requests
 * @author pluk
 *
 */
public class RpcClientRejectedExecutionException extends ClientRejectedExecutionException {

    public RpcClientRejectedExecutionException(RejectedExecutionException cause) {
        super(cause);
    }
    
    public RpcClientRejectedExecutionException(String message) {
        super(message);
    }

}
