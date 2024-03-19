package com.solarwinds.joboe.sampling;

import java.util.HashMap;
import java.util.Map;

public class SettingsMock extends Settings {
    private final short settingsType;
    private short flags = 0;
    private int sampleRate = 1000000;
    private final Map<SettingsArg<?>, Object> args;


    private SettingsMock(short settingsType, short flags, Integer sampleRate, Map<SettingsArg<?>, Object> args) {
        this.settingsType = settingsType;
        this.flags = flags;
        if (sampleRate != null) {
            this.sampleRate = sampleRate;
        }
        this.args = args;
    }


    @Override
    public long getValue() {
        return sampleRate;
    }

    @Override
    public long getTimestamp() {
        return 0;
    }

    @Override
    public short getFlags() {
        return flags;
    }

    @Override
    public <T> T getArgValue(SettingsArg<T> arg) {
        return (T) args.get(arg);
    }

    @Override
    public short getType() {
        return settingsType;
    }

    @Override
    public String getLayer() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long getTtl() {
        // TODO Auto-generated method stub
        return 0;
    }

    public static SettingsMockBuilder builder() {
        return new SettingsMockBuilder();
    }

    public static class SettingsMockBuilder {
        private short flags = 0;
        private Integer sampleRate = null;
        private final Map<SettingsArg<?>, Object> args = new HashMap<SettingsArg<?>, Object>();

        public static final double DEFAULT_TOKEN_BUCKET_RATE = 8.0;
        public static final double DEFAULT_TOKEN_BUCKET_CAPACITY = 16.0;
        private short settingsType = Settings.OBOE_SETTINGS_TYPE_DEFAULT_SAMPLE_RATE;

        public SettingsMockBuilder() {
            addDefaultArgs();
        }

        private void addDefaultArgs() {
            args.put(SettingsArg.BUCKET_RATE, DEFAULT_TOKEN_BUCKET_RATE);
            args.put(SettingsArg.BUCKET_CAPACITY, DEFAULT_TOKEN_BUCKET_CAPACITY);
            args.put(SettingsArg.RELAXED_BUCKET_RATE, DEFAULT_TOKEN_BUCKET_RATE);
            args.put(SettingsArg.RELAXED_BUCKET_CAPACITY, DEFAULT_TOKEN_BUCKET_CAPACITY);
            args.put(SettingsArg.STRICT_BUCKET_RATE, DEFAULT_TOKEN_BUCKET_RATE);
            args.put(SettingsArg.STRICT_BUCKET_CAPACITY, DEFAULT_TOKEN_BUCKET_CAPACITY);
        }

        public SettingsMockBuilder withFlags(boolean isStart, boolean isThrough, boolean isThroughAlways, boolean isTriggerTraceEnabled, boolean isOverride) {
            flags = getFlags(isStart, isThrough, isThroughAlways, isTriggerTraceEnabled, isOverride);
            return this;
        }

        public SettingsMockBuilder withFlags(TracingMode tracingMode) {
            flags |= tracingMode.toFlags();
            return this;
        }

        public SettingsMockBuilder withSampleRate(int sampleRate) {
            this.sampleRate = sampleRate;
            return this;
        }

        public <T> SettingsMockBuilder withSettingsArg(SettingsArg<T> arg, T value) {
            args.put(arg, value);
            return this;
        }

        public SettingsMockBuilder withSettingsArgs(Map<SettingsArg<?>, ?> args) {
            this.args.clear();
            this.args.putAll(args);
            return this;
        }

        public SettingsMockBuilder withSettingsType(short settingsType) {
            this.settingsType = settingsType;
            return this;
        }

        public SettingsMock build() {
            return new SettingsMock(settingsType, flags, sampleRate, args);
        }


        private static short getFlags(boolean isStart, boolean isThrough, boolean isThroughAlways, boolean isTriggerTraceEnabled, boolean isOverride) {
            byte flags = 0;
            if (isOverride) {
                flags |= Settings.OBOE_SETTINGS_FLAG_OVERRIDE;
            }

            if (isStart) {
                flags |= Settings.OBOE_SETTINGS_FLAG_SAMPLE_START;
            }

            if (isThrough) {
                flags |= Settings.OBOE_SETTINGS_FLAG_SAMPLE_THROUGH;
            }

            if (isThroughAlways) {
                flags |= Settings.OBOE_SETTINGS_FLAG_SAMPLE_THROUGH_ALWAYS;
            }

            if (isTriggerTraceEnabled) {
                flags |= Settings.OBOE_SETTINGS_FLAG_TRIGGER_TRACE_ENABLED;
            }


            return flags;
        }
    }
}