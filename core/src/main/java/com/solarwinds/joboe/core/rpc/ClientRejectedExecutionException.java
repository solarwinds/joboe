package com.solarwinds.joboe.core.rpc;

import java.util.concurrent.RejectedExecutionException;

@SuppressWarnings("serial")
public class ClientRejectedExecutionException extends ClientException {

    public ClientRejectedExecutionException(RejectedExecutionException cause) {
        super(cause);
    }
    
    public ClientRejectedExecutionException(String message) {
        super(message);
    }
}
