package com.tracelytics.joboe;

import java.util.concurrent.Future;
import java.util.logging.Logger;

import com.tracelytics.agent.Agent;
import com.tracelytics.joboe.config.ConfigManager;
import com.tracelytics.joboe.settings.Settings;
import com.tracelytics.joboe.settings.SettingsArg;
import com.tracelytics.joboe.settings.SettingsManager;
import com.tracelytics.joboe.settings.SimpleSettingsFetcher;
import com.tracelytics.joboe.settings.TestSettingsReader;
import com.tracelytics.joboe.span.impl.ScopeManager;

import junit.framework.TestCase;

/**
 * Common parent class for joboe unit tests.
 */
public abstract class JoboeTest extends TestCase {
    protected Logger log = Logger.getLogger(getClass().getName());
    protected static final TestReporter tracingReporter;
    protected static final TestReporter profilingReporter;
    protected static final TestSettingsReader testSettingsReader;
    
    static {
        StartupManager.flagTestingMode();
        Future<TestingEnv> testingEnvFuture = (Future<TestingEnv>) StartupManager.isAgentReady();
        TestingEnv testingEnv;
        TestReporter testTracingReporter = null;
        TestReporter testProfilingReporter = null;
        TestSettingsReader reader = null;
        try {
            testingEnv = testingEnvFuture.get();
            testTracingReporter = testingEnv.getTracingReporter();
            testProfilingReporter = testingEnv.getProfilingReporter(); 
            reader = testingEnv.getSettingsReader();
        } catch (Exception e) {
            e.printStackTrace();
        } finally  {
            tracingReporter = testTracingReporter;
            profilingReporter = testProfilingReporter;
            testSettingsReader = reader;
        }
    }
    
    public static class TestDefaultSettings extends Settings {
        public static final Double DEFAULT_BUCKET_CAPACITY = 16.0;
        public static final Double DEFAULT_BUCKET_RATE = 8.0;
        public static final long DEFAULT_SAMPLE_RATE = Agent.SAMPLE_RESOLUTION; //100%
        public static final short DEFAULT_TYPE = Settings.OBOE_SETTINGS_TYPE_DEFAULT_SAMPLE_RATE;

        @Override
        public long getValue() {
            return DEFAULT_SAMPLE_RATE; 
        }

        @Override
        public long getTimestamp() {
            return Agent.currentTimeStamp();
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
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        tracingReporter.reset();
        testSettingsReader.reset();
        testSettingsReader.put(new TestDefaultSettings());
        SimpleSettingsFetcher fetcher = (SimpleSettingsFetcher) SettingsManager.getFetcher();
        Context.clearMetadata();
        ScopeManager.INSTANCE.removeAllScopes();
    }
    
    @Override
    protected void tearDown() throws Exception {
        tracingReporter.reset();
        testSettingsReader.reset();
        Context.clearMetadata();
        ScopeManager.INSTANCE.removeAllScopes();
        TraceDecisionUtil.reset();
        ConfigManager.reset();
        super.tearDown();
    }
}
