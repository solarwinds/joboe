package com.solarwinds.joboe.settings;

/**
 * Listens to notification of {@link Settings} from {@link SettingsFetcher}
 * 
 * @author pluk
 *
 */
interface SettingsListener {
    void onSettingsRetrieved(Settings newSettings);
}
