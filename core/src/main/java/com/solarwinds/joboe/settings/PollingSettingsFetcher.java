package com.solarwinds.joboe.settings;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.solarwinds.logging.Logger;
import com.solarwinds.logging.LoggerFactory;
import com.solarwinds.util.DaemonThreadFactory;

/**
 * A {@link SettingsFetcher} that polls settings from the provided {@link SettingsReader} at a given refresh interval
 * 
 * Every time a {@link Settings} is fetched, the registered listener would be notified
 * 
 * @author pluk
 *
 */
public class PollingSettingsFetcher implements SettingsFetcher {
    private static final Logger logger = LoggerFactory.getLogger();
    private static final int DEAFULT_REFRESH_INTERVAL = 30; //30 secs
    
    
    private final int refreshInterval; //pauses between each reader call in seconds
    private final SettingsReader reader;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor(DaemonThreadFactory.newInstance("poll-settings"));
    private Settings currentSettings;
    private SettingsListener listener;
    
    private final CountDownLatch firstTryLatch = new CountDownLatch(1);
    private boolean active = true;    
    
    public PollingSettingsFetcher(SettingsReader reader) {
        this(reader, DEAFULT_REFRESH_INTERVAL);
    }
    
    PollingSettingsFetcher(SettingsReader reader, int refreshInterval) {
        this.reader = reader;
        this.refreshInterval = refreshInterval;
        startWorker();
    }    

    public Settings getSettings() {
        synchronized(this) {
            if (isExpired(currentSettings)) {
                currentSettings = null;
            }
        }
        return currentSettings;
    }
    
    private boolean isExpired(Settings settings) {
        boolean isExpired = false;
        if (settings != null) {
            long ttlInMillisec = settings.getTtl() * 1000;
            isExpired = settings.getTimestamp() + ttlInMillisec < System.currentTimeMillis();
            if (isExpired) {
                logger.warn("Settings for [" + settings + "] has expired");
            }
        }
        return isExpired;
    }

    public CountDownLatch isSettingsAvailableLatch() {
        return firstTryLatch;
    }
    
    public void registerListener(SettingsListener listener) {
        this.listener = listener;
    }
    
    public void close() {
        active = false;
        executorService.shutdown();
        if (reader != null) {
            reader.close();
        }
    }
    
    /**
     * Starts background worker to update settings record with <code>refreshInterval</code> sec pauses in between
     * @return
     */
    private Future<?> startWorker() {
        return executorService.submit(new Runnable() {
            public void run() {
                logger.debug("Starting background worker to update settings");
                boolean running = true;
                while (PollingSettingsFetcher.this.active && running) { //periodically poll for settings
                    Settings newSettings = null;
                    try {
                        Map<String, Settings> allSettings = reader.getSettings();
                        if (allSettings.size() == 0) {
                            logger.warn("No Settings returned by the Settings Reader!");
                        } else {
                            newSettings = allSettings.get(DEFAULT_LAYER); //for now only get the record with ""
                            if (newSettings == null) {
                                logger.debug("Cannot find Settings with emtpy ID. Trying to locate the default record");
                                for (Settings settings : allSettings.values()) {
                                    if (settings.isDefault()) {
                                        newSettings = settings;
                                        break;
                                    }
                                }
                                if (newSettings == null) {
                                    logger.warn("Cannot find Settings with empty ID nor a default record... using the first record");
                                    newSettings = allSettings.values().iterator().next();
                                }
                            }
                            
                        }
                    } catch (OboeSettingsException e) {
                        logger.debug("Failed to get settings : " + e.getMessage(), e); //Should not be too noisy as this might happen for intermittent connection problem
                    }
                        
                    if (newSettings != null) {
                        synchronized(this) {
                            currentSettings = newSettings;
                        }
                        
                        firstTryLatch.countDown(); //count down on the latch to flag that settings is ready
                        if (listener != null) {
                            listener.onSettingsRetrieved(newSettings);
                        }
                    } else { //purge expired ones
                        logger.debug("Failed to retrieve settings from rpc call");
                    }
                    
                    try {
                        TimeUnit.SECONDS.sleep(refreshInterval); //sleep for the defined interval
                    } catch (InterruptedException e) {
                        logger.debug(PollingSettingsFetcher.class.getName() + " worker is interrupted : " + e.getMessage());
                        running = false;
                    }
                }
            }
        });
    }

}
