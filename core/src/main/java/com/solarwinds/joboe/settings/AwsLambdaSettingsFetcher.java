package com.solarwinds.joboe.settings;

import com.solarwinds.logging.Logger;
import com.solarwinds.logging.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;


public class AwsLambdaSettingsFetcher implements SettingsFetcher {
    private static final Logger logger = LoggerFactory.getLogger();

    private final SettingsReader settingsReader;

    private SettingsListener settingsListener;

    private Settings currentSettings;

    public AwsLambdaSettingsFetcher(SettingsReader settingsReader, Settings settings){
        this.settingsReader = settingsReader;
        this.currentSettings = settings;
    }

    @Override
    public Settings getSettings() {
        Settings settings = currentSettings;
        long millisecondsElapsed = System.currentTimeMillis() - settings.getTimestamp();
        if (millisecondsElapsed > settings.getTtl() * 1000) {
            try {
                Map<String, Settings> allSettings = settingsReader.getSettings();
                if (allSettings.isEmpty()) {
                    logger.warn("No Settings returned by the Settings Reader!");

                } else {
                    settings = allSettings.get(DEFAULT_LAYER);
                    if (settings == null) {
                        logger.debug("Cannot find Settings with empty ID. Trying to locate the default record");
                        Optional<Settings> defaultSetting = allSettings.values()
                                .stream()
                                .filter(Settings::isDefault)
                                .findFirst();

                        if (defaultSetting.isPresent()) {
                            settings = defaultSetting.get();
                        } else {
                            logger.warn("Cannot find Settings with empty ID nor a default record... using the first record");
                            settings = allSettings.values().iterator().next();
                        }
                    }
                }

            } catch (OboeSettingsException e) {
                logger.debug("Failed to get settings : " + e.getMessage(), e); //Should not be too noisy as this might happen for intermittent connection problem
            }

            if (settingsListener != null) {
                settingsListener.onSettingsRetrieved(settings);
            }
        }

        currentSettings = settings;
        return settings;
    }

    @Override
    public void registerListener(SettingsListener listener) {
        settingsListener = listener;
    }

    @Override
    public CountDownLatch isSettingsAvailableLatch() {
        return new CountDownLatch(0);
    }

    @Override
    public void close() {

    }
}