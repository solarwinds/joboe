package com.solarwinds.joboe.settings;

import java.util.concurrent.CountDownLatch;

import com.solarwinds.joboe.settings.TestSettingsReader.SettingsChangeCallback;

/**
 * A testing fetcher that returns {@link Settings} by directly reads from the {@link TestSettingsReader} provided in constructor
 * 
 * Notifies {@link SettingsListener} whenever the reader has settings changes 
 * 
 * @author pluk
 *
 */
public class SimpleSettingsFetcher implements SettingsFetcher {
    private TestSettingsReader reader;
    private SettingsListener listener;
    
    public SimpleSettingsFetcher(TestSettingsReader reader) {
        this.reader = reader;
        
        reader.onSettingsChanged( new SettingsChangeCallback() {
            public void settingsChanged() {
                fetch();
            }
        });
    }
    
    
    public Settings getSettings() {
        try {
            return reader.getSettings().get(DEFAULT_LAYER);
        } catch (OboeSettingsException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void registerListener(SettingsListener listener) {
        this.listener = listener;
    }
    
    private void fetch() {
        if (listener != null) {
            Settings newSettings = getSettings();
            listener.onSettingsRetrieved(newSettings);
        }
    }

    public CountDownLatch isSettingsAvailableLatch() {
        return new CountDownLatch(0);
    }
    
    public void close() {
        if (reader != null) {
            reader.close();
        }
    }
    
    public SettingsReader getReader() {
        return reader;
    }
}
