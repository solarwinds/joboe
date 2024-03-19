package com.solarwinds.joboe.sampling;

/**
 * Listens to notification of {@link Settings} from {@link SettingsFetcher}
 * 
 * @author pluk
 *
 */
public interface SettingsListener {
    void onSettingsRetrieved(Settings newSettings);
}
