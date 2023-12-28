package com.solarwinds.joboe.core;

/** Generic Oboe Exception */
public class OboeException extends Exception {
    public OboeException(String message, Throwable cause) {
        super(message, cause);
    }

    public OboeException(Throwable cause) {
        super(cause);
    }

    public OboeException(String msg) {
        super(msg);
    }
    
}
