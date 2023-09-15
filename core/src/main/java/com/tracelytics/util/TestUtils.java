package com.tracelytics.util;

import com.tracelytics.joboe.ReporterFactory;
import com.tracelytics.joboe.TestReporter;
import com.tracelytics.joboe.TraceDecisionUtil;
import com.tracelytics.joboe.TracingMode;
import com.tracelytics.joboe.config.ProfilerSetting;
import com.tracelytics.joboe.settings.*;
import com.tracelytics.profiler.Profiler;

public abstract class TestUtils {
    private static final TestDefaultSettings DEFAULT_SETTINGS = new TestDefaultSettings();

    private TestUtils() {
    }

    public static TestSettingsReader initSettingsReader() {
        TestSettingsReader reader = new TestSettingsReader();
        SettingsManager.initializeFetcher(new SimpleSettingsFetcher(reader));
        return reader;
    }

    public static TestReporter initTraceReporter() {
        try {
            TestReporter testTracingReporter = ReporterFactory.getInstance().createTestReporter();
            return testTracingReporter;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    public static TestReporter initProfilingReporter(ProfilerSetting profilerSetting) {
        try {
            TestReporter testProfilingReporter = ReporterFactory.getInstance().createTestReporter();
            Profiler.initialize(profilerSetting, testProfilingReporter);
            return testProfilingReporter;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
}

    public static Settings getDefaultSettings() {
        return DEFAULT_SETTINGS;
    }


    private static class TestDefaultSettings extends Settings {
        public static final Double DEFAULT_BUCKET_CAPACITY = 16.0;
        public static final Double DEFAULT_BUCKET_RATE = 8.0;
        public static final long DEFAULT_SAMPLE_RATE = TraceDecisionUtil.SAMPLE_RESOLUTION; //100%
        public static final short DEFAULT_TYPE = Settings.OBOE_SETTINGS_TYPE_DEFAULT_SAMPLE_RATE;
        
        @Override
        public long getValue() {
            return DEFAULT_SAMPLE_RATE;
        }

        @Override
        public long getTimestamp() {
            return TimeUtils.getTimestampMicroSeconds();
        }

        @Override
        public short getType() {
            return DEFAULT_TYPE;
        }

        @Override
        public short getFlags() {
            return TracingMode.ALWAYS.toFlags();
        }

        @Override
        public String getLayer() {
            return "";
        }

        @Override
        public long getTtl() {
            return Integer.MAX_VALUE; //don't use long, otherwise it might overflow...
        }

        @Override
        public <T> T getArgValue(SettingsArg<T> arg) {
            if (arg.equals(SettingsArg.BUCKET_CAPACITY)) {
                return (T) DEFAULT_BUCKET_CAPACITY;
            } else if (arg.equals(SettingsArg.BUCKET_RATE)){
                return (T) DEFAULT_BUCKET_RATE;
            } else if (arg.equals(SettingsArg.RELAXED_BUCKET_CAPACITY)) {
                return (T) DEFAULT_BUCKET_CAPACITY;
            } else if (arg.equals(SettingsArg.RELAXED_BUCKET_RATE)) {
                return (T) DEFAULT_BUCKET_RATE;
            } else if (arg.equals(SettingsArg.STRICT_BUCKET_CAPACITY)) {
                return (T) DEFAULT_BUCKET_CAPACITY;
            } else if (arg.equals(SettingsArg.STRICT_BUCKET_RATE)) {
                return (T) DEFAULT_BUCKET_RATE;
            }

            return null;
        }
    }
}
