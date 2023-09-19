package com.tracelytics.joboe.settings;

import com.solarwinds.trace.ingestion.proto.Collector;
import com.tracelytics.ext.google.protobuf.ByteString;
import com.tracelytics.joboe.rpc.ResultCode;
import com.tracelytics.joboe.rpc.SettingsResult;
import com.tracelytics.logging.LoggerFactory;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.nio.ByteBuffer;
import java.util.*;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SettingsUtil {

    public static Map<String, Settings> transformToKVSetting(SettingsResult settingsResult){
        Map<String, Settings> updatedSettings = new LinkedHashMap<String, Settings>();
        for (Settings settingsForLayer : settingsResult.getSettings()) {
            LoggerFactory.getLogger().debug("Got settings from collector: " + settingsForLayer);
            updatedSettings.put(settingsForLayer.getLayer(), settingsForLayer);
        }
        return updatedSettings;
    }

    public static SettingsResult transformToLocalSettings(Collector.SettingsResult result){
        List<Settings> settings = new ArrayList<>();
        if (result.getResult() == Collector.ResultCode.OK) {
            for (Collector.OboeSetting oboeSetting : result.getSettingsList()) {
                settings.add(convertSetting(oboeSetting));
            }
        }

        return new SettingsResult(ResultCode.valueOf(result.getResult().name()), result.getArg(), result.getWarning(), settings);
    }

    public static Settings convertSetting(Collector.OboeSetting grpcOboeSetting) {
        Map<String, ByteBuffer> convertedArguments = new HashMap<String, ByteBuffer>();

        for (Map.Entry<String, ByteString> argumentEntry : grpcOboeSetting.getArgumentsMap().entrySet()) {
            convertedArguments.put(argumentEntry.getKey(), argumentEntry.getValue().asReadOnlyByteBuffer());
        }

        com.tracelytics.joboe.rpc.Settings settings = new com.tracelytics.joboe.rpc.Settings(
                convertType(grpcOboeSetting.getType()),
                grpcOboeSetting.getFlags().toStringUtf8(),
                //oboeSetting.getTimestamp(),
                System.currentTimeMillis(), //use local timestamp for now, as it is easier to compare ttl with it
                grpcOboeSetting.getValue(),
                grpcOboeSetting.getTtl(),
                grpcOboeSetting.getLayer().toStringUtf8(),
                convertedArguments);

        return settings;
    }

    public static short convertType(Collector.OboeSettingType grpcType) {
        switch (grpcType) {
            case DEFAULT_SAMPLE_RATE:
                return Settings.OBOE_SETTINGS_TYPE_DEFAULT_SAMPLE_RATE;
            case LAYER_SAMPLE_RATE:
                return Settings.OBOE_SETTINGS_TYPE_LAYER_SAMPLE_RATE;
            case LAYER_APP_SAMPLE_RATE:
                return Settings.OBOE_SETTINGS_TYPE_LAYER_APP_SAMPLE_RATE;
            case LAYER_HTTPHOST_SAMPLE_RATE:
                return Settings.OBOE_SETTINGS_TYPE_LAYER_HTTPHOST_SAMPLE_RATE;
            default:
                return -1;
        }
    }
}
