package com.tracelytics.joboe.settings;

import com.solarwinds.trace.ingestion.proto.Collector;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

import static com.tracelytics.joboe.settings.SettingsUtil.transformToKVSetting;
import static com.tracelytics.joboe.settings.SettingsUtil.transformToLocalSettings;

public class FileSettingsReader implements SettingsReader {
    private final String settingsFilePath;

    public FileSettingsReader(String settingsFilePath) {
        this.settingsFilePath = settingsFilePath;
    }

    @Override
    public Map<String, Settings> getSettings() throws OboeSettingsException {
        try {
            byte[] bytes = Files.readAllBytes(Paths.get(settingsFilePath));
            return transformToKVSetting(transformToLocalSettings(Collector.SettingsResult.parseFrom(bytes)));
        } catch (IOException e) {
            throw new OboeSettingsException("Error reading settings from file", e);
        }
    }

    @Override
    public void close() {

    }
}
