package com.solarwinds.joboe.rpc;

/**
 * Indicates exception that can be recovered (hence could retry on the same operation, and it might become successful)
 * @author pluk
 *
 */
public class ClientRecoverableException extends ClientException {

    public ClientRecoverableException(String message) {
        super(message);
    }

    public ClientRecoverableException(Throwable cause) {
        super(cause);
    }

    public ClientRecoverableException(String message, Throwable cause) {
        super(message, cause);
    }
}
