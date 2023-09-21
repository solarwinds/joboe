package com.tracelytics.joboe.settings;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.HashMap;

import static com.tracelytics.joboe.settings.SettingsManager.DEFAULT_SETTINGS;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AwsLambdaSettingsFetcherTest {

    private AwsLambdaSettingsFetcher tested;

    @Mock
    private SettingsReader settingsReaderMock;

    @Mock
    private SettingsListener settingsListenerMock;

    @BeforeEach
    void setup(){
        tested = new AwsLambdaSettingsFetcher(settingsReaderMock, DEFAULT_SETTINGS);
    }

    @Test
    void verifyThatDefaultLayerSettingIsUsed() throws OboeSettingsException {
        com.tracelytics.joboe.rpc.Settings settings = new com.tracelytics.joboe.rpc.Settings(
                (short) 1,
                "2",
                System.currentTimeMillis(),
                1_000,
                10,
                "",
                Collections.emptyMap());
        when(settingsReaderMock.getSettings()).thenReturn(new HashMap<String, Settings>() {{
            put("", settings);
        }});

        Settings actual = tested.getSettings();
        assertEquals(settings.getType(), actual.getType());
        assertEquals(settings.getFlags(), actual.getFlags());

        assertEquals(settings.getTimestamp(), actual.getTimestamp());
        assertEquals(settings.getTtl(), actual.getTtl());
        assertEquals(settings.getLayer(), actual.getLayer());

        assertEquals(settings.getValue(), actual.getValue());
    }

    @Test
    void verifyThatDefaultSettingIsUsedWhenThereIsNoDefaultLayerSetting() throws OboeSettingsException {
        com.tracelytics.joboe.rpc.Settings defaultSettings = new com.tracelytics.joboe.rpc.Settings(
                (short) 2,
                "2",
                System.currentTimeMillis(),
                1_000,
                10,
                "90",
                Collections.emptyMap());

        com.tracelytics.joboe.rpc.Settings settings = new com.tracelytics.joboe.rpc.Settings(
                (short) 6,
                "25",
                System.currentTimeMillis(),
                10_000,
                140,
                "--",
                Collections.emptyMap());
        when(settingsReaderMock.getSettings()).thenReturn(new HashMap<String, Settings>() {{
            put("1", defaultSettings);
            put("3", settings);
        }});

        Settings actual = tested.getSettings();
        assertEquals(defaultSettings.getType(), actual.getType());
        assertEquals(defaultSettings.getFlags(), actual.getFlags());

        assertEquals(defaultSettings.getTimestamp(), actual.getTimestamp());
        assertEquals(defaultSettings.getTtl(), actual.getTtl());
        assertEquals(defaultSettings.getLayer(), actual.getLayer());

        assertEquals(defaultSettings.getValue(), actual.getValue());
    }

    @Test
    void verifyThatNewSettingIsNotFetchedWhenCurrentSettingsIsNotExpired() throws OboeSettingsException {
        com.tracelytics.joboe.rpc.Settings defaultSettings = new com.tracelytics.joboe.rpc.Settings(
                (short) 2,
                "2",
                System.currentTimeMillis(),
                1_000,
                120,
                "90",
                Collections.emptyMap());


        tested = new AwsLambdaSettingsFetcher(settingsReaderMock, defaultSettings);
        tested.getSettings();
        verify(settingsReaderMock, never()).getSettings();
    }

    @Test
    void verifyThatHardCodedDefaultSettingIsUsedWhenThereIsNoSetting() throws OboeSettingsException {
        when(settingsReaderMock.getSettings()).thenReturn(Collections.emptyMap());

        Settings actual = tested.getSettings();
        assertEquals(0, actual.getType());
        assertEquals(20, actual.getFlags());

        assertEquals(1, actual.getTtl());
        assertEquals("", actual.getLayer());
        assertEquals(1_000_000, actual.getValue());
    }

    @Test
    void verifyThatHardCodedDefaultSettingIsUsedWhenThereIsAOboeSettingsException() throws OboeSettingsException {
        when(settingsReaderMock.getSettings()).thenThrow(OboeSettingsException.class);

        Settings actual = tested.getSettings();
        assertEquals(0, actual.getType());
        assertEquals(20, actual.getFlags());

        assertEquals(1, actual.getTtl());
        assertEquals("", actual.getLayer());
        assertEquals(1_000_000, actual.getValue());
    }

    @Test
    void verifyThatListenerIsCalled() {
        tested.registerListener(settingsListenerMock);
        tested.getSettings();
        verify(settingsListenerMock).onSettingsRetrieved(any());
    }

    @Test
    void veryThatCountDownLatchCountIsZero() {
        assertEquals(0, tested.isSettingsAvailableLatch().getCount());
    }
}