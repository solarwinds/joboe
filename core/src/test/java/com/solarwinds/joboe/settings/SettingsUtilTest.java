package com.solarwinds.joboe.settings;

import com.google.protobuf.InvalidProtocolBufferException;
import com.solarwinds.trace.ingestion.proto.Collector;
import com.solarwinds.joboe.rpc.ResultCode;
import com.solarwinds.joboe.rpc.SettingsResult;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SettingsUtilTest {

    private static byte[] settingsBlob;

    @Mock
    private com.solarwinds.joboe.rpc.Settings settingsMock;

    @BeforeAll
    static void setup() throws IOException {
        settingsBlob = Files.readAllBytes(Paths.get(new File("src/test/resources/solarwinds-apm-settings-raw").getPath()));
    }

    @Test
    void testTransformToKVSetting() {
        when(settingsMock.getTtl()).thenReturn(60L);
        SettingsResult settingsResult = new SettingsResult(ResultCode.OK, "arg", "we up", Collections.singletonList(settingsMock));
        Map<String, Settings> settingsMap = SettingsUtil.transformToKVSetting(settingsResult);

        boolean anyMatch = settingsMap.values().stream().anyMatch(settings -> settings.getTtl() == 60);
        assertTrue(anyMatch);
    }

    @Test
    void testTransformToLocalSettings() throws InvalidProtocolBufferException {
        SettingsResult settingsResult = SettingsUtil.transformToLocalSettings(Collector.SettingsResult.parseFrom(settingsBlob));
        assertEquals(ResultCode.OK, settingsResult.getResultCode());
    }

    @Test
    void testConvertSetting() throws InvalidProtocolBufferException {
        Collector.SettingsResult settingsResult = Collector.SettingsResult.parseFrom(settingsBlob);
        Settings settings = SettingsUtil.convertSetting(settingsResult.getSettings(0));
        assertEquals(120, settings.getTtl());
    }

    @Test
    void testConvertType() {
        assertEquals(-1, SettingsUtil.convertType(Collector.OboeSettingType.UNRECOGNIZED));
        assertEquals(Settings.OBOE_SETTINGS_TYPE_DEFAULT_SAMPLE_RATE, SettingsUtil.convertType(Collector.OboeSettingType.DEFAULT_SAMPLE_RATE));
        assertEquals(Settings.OBOE_SETTINGS_TYPE_LAYER_SAMPLE_RATE, SettingsUtil.convertType(Collector.OboeSettingType.LAYER_SAMPLE_RATE));
        assertEquals(Settings.OBOE_SETTINGS_TYPE_LAYER_APP_SAMPLE_RATE, SettingsUtil.convertType(Collector.OboeSettingType.LAYER_APP_SAMPLE_RATE));
        assertEquals(Settings.OBOE_SETTINGS_TYPE_LAYER_HTTPHOST_SAMPLE_RATE, SettingsUtil.convertType(Collector.OboeSettingType.LAYER_HTTPHOST_SAMPLE_RATE));
    }
}