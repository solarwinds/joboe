package com.solarwinds.joboe.core.settings;

import com.solarwinds.joboe.core.rpc.RpcSettings;
import com.solarwinds.joboe.sampling.Settings;
import com.solarwinds.joboe.sampling.SettingsListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;
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
        tested = new AwsLambdaSettingsFetcher(settingsReaderMock);
    }

    @Test
    void verifyThatDefaultLayerSettingIsUsed() throws OboeSettingsException {
        RpcSettings settings = new RpcSettings(
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
        RpcSettings defaultSettings = new RpcSettings(
                (short) 2,
                "2",
                System.currentTimeMillis(),
                1_000,
                10,
                "90",
                Collections.emptyMap());

        RpcSettings settings = new RpcSettings(
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
    void verifyThatNullIsReturnedWhenThereIsNoSetting() throws OboeSettingsException {
        when(settingsReaderMock.getSettings()).thenReturn(Collections.emptyMap());

        Settings actual = tested.getSettings();
        assertNull(actual);
    }

    @Test
    void verifyThatNullIsReturnedWhenThereIsAnOboeSettingsException() throws OboeSettingsException {
        when(settingsReaderMock.getSettings()).thenThrow(OboeSettingsException.class);

        Settings actual = tested.getSettings();
        assertNull(actual);
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