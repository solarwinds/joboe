package com.solarwinds.joboe.settings;

import java.util.concurrent.CountDownLatch;

/**
 * A testing fetcher that returns {@link Settings} by directly reads from the {@link TestSettingsReader} provided in constructor
 * 
 * Notifies {@link SettingsListener} whenever the reader has settings changes 
 * 
 * @author pluk
 *
 */
public class SimpleSettingsFetcher implements SettingsFetcher {
    private final TestSettingsReader reader;
    private SettingsListener listener;
    
    public SimpleSettingsFetcher(TestSettingsReader reader) {
        this.reader = reader;
        
        reader.onSettingsChanged(() -> fetch());
    }
    
    
    @Override
    public Settings getSettings() {
        try {
            return reader.getSettings().get(DEFAULT_LAYER);
        } catch (OboeSettingsException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void registerListener(SettingsListener listener) {
        this.listener = listener;
    }
    
    private void fetch() {
        if (listener != null) {
            Settings newSettings = getSettings();
            listener.onSettingsRetrieved(newSettings);
        }
    }

    @Override
    public CountDownLatch isSettingsAvailableLatch() {
        return new CountDownLatch(0);
    }
    
    @Override
    public void close() {
        if (reader != null) {
            reader.close();
        }
    }
    
    public SettingsReader getReader() {
        return reader;
    }
}
