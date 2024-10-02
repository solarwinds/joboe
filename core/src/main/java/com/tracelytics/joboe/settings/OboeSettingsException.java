package com.tracelytics.joboe.settings;

import com.tracelytics.joboe.OboeException;

/**
 * Reports exception arise during settings (rates/bucket) read operations 
 *
 */
public class OboeSettingsException extends OboeException {
    public OboeSettingsException(String msg) {
        super(msg);
    }

    public OboeSettingsException(String message, Throwable cause) {
        super(message, cause);
    }

    public OboeSettingsException(Throwable cause) {
        super(cause);
    }
}
