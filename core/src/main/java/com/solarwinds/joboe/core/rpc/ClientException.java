package com.solarwinds.joboe.core.rpc;

public class ClientException extends Exception {
    public ClientException(Throwable cause) {
        super(cause);
    }

    public ClientException(String message) {
        super(message);
    }

    public ClientException(String message, Throwable cause) {
        super(message, cause);
    }

}
