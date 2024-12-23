package com.solarwinds.joboe.core.settings;

import com.solarwinds.joboe.core.OboeException;

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
