package com.tracelytics.joboe.settings;

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