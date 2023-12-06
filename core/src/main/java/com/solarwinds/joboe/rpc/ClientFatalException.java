package com.solarwinds.joboe.rpc;

/**
 * Indicates exception that cannot be recovered (hence should not retry on the same operation)
 * @author pluk
 *
 */
public class ClientFatalException extends ClientException {

    public ClientFatalException(String message) {
        super(message);
    }

    public ClientFatalException(Throwable cause) {
        super(cause);
    }

    public ClientFatalException(String message, Throwable cause) {
        super(message, cause);
    }
}
