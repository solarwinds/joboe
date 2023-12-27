package com.solarwinds.joboe.settings;

import com.solarwinds.joboe.TraceDecisionUtil;
import com.solarwinds.joboe.TracingMode;
import com.solarwinds.joboe.rpc.ClientException;
import com.solarwinds.joboe.settings.TestSettingsReader.SettingsMockupBuilder;
import com.solarwinds.util.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.ClearEnvironmentVariable;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class SettingsManagerTest {
    private static final TestSettingsReader testSettingsReader = TestUtils.initSettingsReader();

    @BeforeEach
    protected void setUp() throws Exception {
        testSettingsReader.put(TestUtils.getDefaultSettings());
    }

    @AfterEach
    protected void tearDown() throws Exception {
        testSettingsReader.reset();
    }

    @Test
    public void testArgChangeListener() throws OboeSettingsException {
        TestArgChangeListener<Double> bucketCapacityListener = new TestArgChangeListener<Double>(SettingsArg.BUCKET_CAPACITY);
        TestArgChangeListener<Integer> metricsFlushIntervalListener = new TestArgChangeListener<Integer>(SettingsArg.METRIC_FLUSH_INTERVAL);
        
        SettingsManager.registerListener(bucketCapacityListener);
        SettingsManager.registerListener(metricsFlushIntervalListener);

        SimpleSettingsFetcher fetcher = new SimpleSettingsFetcher(testSettingsReader);
        SettingsManager.initializeFetcher(fetcher);
        
        //test settings args
        Map<SettingsArg<?>, Object> args = new HashMap<SettingsArg<?>, Object>();
        args.put(SettingsArg.BUCKET_CAPACITY, 1.0);
        args.put(SettingsArg.METRIC_FLUSH_INTERVAL, 5);
        testSettingsReader.put(new SettingsMockupBuilder().withFlags(TracingMode.ALWAYS).withSampleRate(TraceDecisionUtil.SAMPLE_RESOLUTION).withSettingsArgs(args).build());
        assertEquals(1.0, bucketCapacityListener.newValue);
        assertEquals((Integer) 5, metricsFlushIntervalListener.newValue);
        
        //test settings args changes
        args.clear();
        args.put(SettingsArg.BUCKET_CAPACITY, 2.0);
        args.put(SettingsArg.METRIC_FLUSH_INTERVAL, 6);
        testSettingsReader.put(new SettingsMockupBuilder().withFlags(TracingMode.ALWAYS).withSampleRate(TraceDecisionUtil.SAMPLE_RESOLUTION).withSettingsArgs(args).build());
        assertEquals(2.0, bucketCapacityListener.newValue);
        assertEquals((Integer) 6, metricsFlushIntervalListener.newValue);
        
        //test settings args set to null from non null
        testSettingsReader.put(new SettingsMockupBuilder().withFlags(TracingMode.ALWAYS).withSampleRate(TraceDecisionUtil.SAMPLE_RESOLUTION).withSettingsArgs(Collections.emptyMap()).build());
        assertNull(bucketCapacityListener.newValue);
        assertNull(metricsFlushIntervalListener.newValue);
        
        
        //one of the test args changed from null to some value
        args.clear();
        args.put(SettingsArg.BUCKET_CAPACITY, 3.0);
        testSettingsReader.put(new SettingsMockupBuilder().withFlags(TracingMode.ALWAYS).withSampleRate(TraceDecisionUtil.SAMPLE_RESOLUTION).withSettingsArgs(args).build());
        assertEquals(3.0, bucketCapacityListener.newValue); //the value has not been changed
        assertNull(metricsFlushIntervalListener.newValue); //value has been changed
        bucketCapacityListener.newValue = null; //reset listener
        
        //test values unchanged
        assertNull(bucketCapacityListener.newValue); //the value has not been changed
        assertNull(metricsFlushIntervalListener.newValue); //the value has not been changed
        

        SettingsManager.removeListener(bucketCapacityListener);
        SettingsManager.removeListener(metricsFlushIntervalListener);
    }

    @Test
    public void testGetSettings() {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        SimpleSettingsFetcher fetcher = new SimpleSettingsFetcher(testSettingsReader) {
            @Override
            public Settings getSettings() {
                // TODO Auto-generated method stub
                return super.getSettings();
            }
            
            @Override
            public CountDownLatch isSettingsAvailableLatch() {
                return countDownLatch;
            }
        };
        
        //simulate a 5 secs delay for fetching
        new Thread() {
            @Override
            public void run() {
                try {
                    TimeUnit.SECONDS.sleep(5);
                    countDownLatch.countDown();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }.start();
        
        SettingsManager.initializeFetcher(fetcher);
        
        assert(SettingsManager.getSettings(2, TimeUnit.SECONDS) == null); //should give null, as 2 sec < 5 sec
        assert(SettingsManager.getSettings(4, TimeUnit.SECONDS) != null); //should not be null as 6 sec > 5 sec
    }

    @Test
    @SetEnvironmentVariable(key = "LAMBDA_TASK_ROOT", value = "lambda eh!")
    @SetEnvironmentVariable(key = "AWS_LAMBDA_FUNCTION_NAME", value = "lambda Fn eh!")
    void returnZeroCountDownWhenInLambda() throws ClientException {
        CountDownLatch initialize = SettingsManager.initialize(new File("src/test/resources/solarwinds-apm-settings-raw").getPath());
        assertEquals(0, initialize.getCount());
    }


    @Test
    @ClearEnvironmentVariable(key = "LAMBDA_TASK_ROOT")
    @ClearEnvironmentVariable(key = "AWS_LAMBDA_FUNCTION_NAME")
    void returnOneCountDownWhenNotInLambda() throws ClientException {
        CountDownLatch initialize = SettingsManager.initialize(new File("src/test/resources/solarwinds-apm-settings-raw").getPath());
        assertEquals(1, initialize.getCount());
    }

    @Test
    void verifyDefaultSettings() {
        Double bucketCap = SettingsManager.DEFAULT_SETTINGS.getArgValue(SettingsArg.BUCKET_CAPACITY);
        assertEquals(8, bucketCap);

        Double bucketRate = SettingsManager.DEFAULT_SETTINGS.getArgValue(SettingsArg.BUCKET_RATE);
        assertEquals(0.17, bucketRate);

        Integer metricFlushInterval = SettingsManager.DEFAULT_SETTINGS.getArgValue(SettingsArg.METRIC_FLUSH_INTERVAL);
        assertEquals(60, metricFlushInterval);

        Double triggerStrictRate = SettingsManager.DEFAULT_SETTINGS.getArgValue(SettingsArg.STRICT_BUCKET_RATE);
        assertEquals(0.1, triggerStrictRate);

        Double triggerStrictCap = SettingsManager.DEFAULT_SETTINGS.getArgValue(SettingsArg.STRICT_BUCKET_CAPACITY);
        assertEquals(6, triggerStrictCap);

        Double triggerRelaxedCap = SettingsManager.DEFAULT_SETTINGS.getArgValue(SettingsArg.RELAXED_BUCKET_CAPACITY);
        assertEquals(20, triggerRelaxedCap);

        Double triggerRelaxedRate = SettingsManager.DEFAULT_SETTINGS.getArgValue(SettingsArg.RELAXED_BUCKET_RATE);
        assertEquals(1, triggerRelaxedRate);
    }

    private static class TestArgChangeListener<T> extends SettingsArgChangeListener<T> {
        private T newValue;
        public TestArgChangeListener(SettingsArg<T> type) {
            super(type);
        }

        @Override
        public void onChange(T newValue) {
            this.newValue = newValue;
        }
    }
}
