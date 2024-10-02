package com.solarwinds.joboe.settings;

import java.util.concurrent.CountDownLatch;

/**
 * Fetches {@link Settings}, fetching strategy based on concrete implementation
 * 
 * Provides <code>Settings</code> via 2 mechanisms:
 * <ol>
 *  <li>Direct retrieval from <code>getSettings</code></li>
 *  <li>Notify subscribing {@link SettingsListener} from <code>registerListener</code>, 
 *  the fetcher implementation determines whenever notification should be sent to subscribing <code>SettingsListener</code>
 *  </li>
 * </ol>
 *
 * Take note that other logics should inquire about <code>Settings<code> via {@link SettingsManager} instead of this fetcher directly
 * 
 * @author pluk
 *
 */
public interface SettingsFetcher {
    String DEFAULT_LAYER = "";
    
    //Settings getSettings(String serviceName);
    Settings getSettings();
    void registerListener(SettingsListener listener);
    
    CountDownLatch isSettingsAvailableLatch();
    
    void close();
}
