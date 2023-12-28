package com.solarwinds.joboe.core.settings;

/**
 * Listens to notification of {@link Settings} from {@link SettingsFetcher}
 * 
 * @author pluk
 *
 */
interface SettingsListener {
    void onSettingsRetrieved(Settings newSettings);
}
