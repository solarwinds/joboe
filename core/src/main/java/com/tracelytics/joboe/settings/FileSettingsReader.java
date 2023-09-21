package com.tracelytics.joboe.settings;

import com.solarwinds.trace.ingestion.proto.Collector;
import com.tracelytics.logging.Logger;
import com.tracelytics.logging.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

import static com.tracelytics.joboe.settings.SettingsUtil.transformToKVSetting;
import static com.tracelytics.joboe.settings.SettingsUtil.transformToLocalSettings;

public class FileSettingsReader implements SettingsReader {
    private final String settingsFilePath;

    private static final Logger logger = LoggerFactory.getLogger();

    public FileSettingsReader(String settingsFilePath) {
        this.settingsFilePath = settingsFilePath;
    }

    @Override
    public Map<String, Settings> getSettings() throws OboeSettingsException {
        try {
            byte[] bytes = Files.readAllBytes(Paths.get(settingsFilePath));
            Map<String, Settings> kvSetting = transformToKVSetting(transformToLocalSettings(Collector.SettingsResult.parseFrom(bytes)));
            logger.debug(String.format("Got settings from file: %s", kvSetting));

            return kvSetting;

        } catch (IOException e) {
            logger.debug(String.format("Failed to read settings from file, error: %s", e));
            throw new OboeSettingsException("Error reading settings from file", e);
        }
    }

    @Override
    public void close() {

    }
}
