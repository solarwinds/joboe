package com.solarwinds.joboe.core.settings;

import com.google.protobuf.ByteString;
import com.solarwinds.joboe.core.rpc.RpcSettings;
import com.solarwinds.joboe.sampling.Settings;
import com.solarwinds.trace.ingestion.proto.Collector;
import com.solarwinds.joboe.core.rpc.ResultCode;
import com.solarwinds.joboe.core.rpc.SettingsResult;
import com.solarwinds.joboe.logging.LoggerFactory;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.nio.ByteBuffer;
import java.util.*;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SettingsUtil {

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

        return new RpcSettings(
                grpcOboeSetting.getFlags().toStringUtf8(),
                System.currentTimeMillis(), //use local timestamp for now, as it is easier to compare ttl with it
                grpcOboeSetting.getValue(),
                grpcOboeSetting.getTtl(),
                convertedArguments);
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
