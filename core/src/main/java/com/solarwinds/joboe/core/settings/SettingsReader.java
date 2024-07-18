package com.solarwinds.joboe.core.settings;

import com.solarwinds.joboe.sampling.Settings;

/**
 * Reads {@link Settings}
 * @author pluk
 *
 */
public interface SettingsReader {
    Settings getSettings() throws OboeSettingsException;
    void close();
}