package com.solarwinds.joboe.core.settings;

import com.solarwinds.joboe.sampling.Settings;

import java.util.Map;

/**
 * Reads {@link Settings}
 * @author pluk
 *
 */
interface SettingsReader {
    Map<String, Settings> getSettings() throws OboeSettingsException;
    void close();
}