package com.tracelytics.joboe.settings;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.tracelytics.util.TestUtils;
import com.tracelytics.joboe.TraceDecisionUtil;
import com.tracelytics.joboe.TracingMode;
import com.tracelytics.joboe.settings.TestSettingsReader.SettingsMockupBuilder;
import junit.framework.TestCase;

public class SettingsManagerTest extends TestCase {
    private final TestSettingsReader testSettingsReader = TestUtils.initSettingsReader();

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        testSettingsReader.put(TestUtils.getDefaultSettings());
    }

    @Override
    protected void tearDown() throws Exception {
        testSettingsReader.reset();
        super.tearDown();
    }

    public void testArgChangeListener() throws OboeSettingsException {
        TestArgChangeListener<Double> bucketCapacityListener = new TestArgChangeListener<Double>(SettingsArg.BUCKET_CAPACITY);
        TestArgChangeListener<Integer> metricsFlushIntervalListener = new TestArgChangeListener<Integer>(SettingsArg.METRIC_FLUSH_INTERVAL);
        
        SettingsManager.registerListener(bucketCapacityListener);
        SettingsManager.registerListener(metricsFlushIntervalListener);

        SimpleSettingsFetcher fetcher = new SimpleSettingsFetcher(testSettingsReader);
        SettingsManager.initializeFetcher(fetcher);
        
        //test settings args
        Map<SettingsArg<?>, Object> args = new HashMap<SettingsArg<?>, Object>();
        args.put(SettingsArg.BUCKET_CAPACITY, (double)1.0);
        args.put(SettingsArg.METRIC_FLUSH_INTERVAL, 5);
        testSettingsReader.put(new SettingsMockupBuilder().withFlags(TracingMode.ALWAYS).withSampleRate(TraceDecisionUtil.SAMPLE_RESOLUTION).withSettingsArgs(args).build());
        assertEquals(1.0, bucketCapacityListener.newValue);
        assertEquals((Integer) 5, metricsFlushIntervalListener.newValue);
        
        //test settings args changes
        args.clear();
        args.put(SettingsArg.BUCKET_CAPACITY, (double)2.0);
        args.put(SettingsArg.METRIC_FLUSH_INTERVAL, 6);
        testSettingsReader.put(new SettingsMockupBuilder().withFlags(TracingMode.ALWAYS).withSampleRate(TraceDecisionUtil.SAMPLE_RESOLUTION).withSettingsArgs(args).build());
        assertEquals(2.0, bucketCapacityListener.newValue);
        assertEquals((Integer) 6, metricsFlushIntervalListener.newValue);
        
        //test settings args set to null from non null
        testSettingsReader.put(new SettingsMockupBuilder().withFlags(TracingMode.ALWAYS).withSampleRate(TraceDecisionUtil.SAMPLE_RESOLUTION).withSettingsArgs(Collections.<SettingsArg<?>, Object>emptyMap()).build());
        assertEquals(null, bucketCapacityListener.newValue);
        assertEquals(null, metricsFlushIntervalListener.newValue);
        
        
        //one of the test args changed from null to some value
        args.clear();
        args.put(SettingsArg.BUCKET_CAPACITY, (double)3.0);
        testSettingsReader.put(new SettingsMockupBuilder().withFlags(TracingMode.ALWAYS).withSampleRate(TraceDecisionUtil.SAMPLE_RESOLUTION).withSettingsArgs(args).build());
        assertEquals(3.0, bucketCapacityListener.newValue); //the value has not been changed
        assertEquals(null, metricsFlushIntervalListener.newValue); //value has been changed
        bucketCapacityListener.newValue = null; //reset listener
        
        //test values unchanged
        assertEquals(null, bucketCapacityListener.newValue); //the value has not been changed
        assertEquals(null, metricsFlushIntervalListener.newValue); //the value has not been changed
        

        SettingsManager.removeListener(bucketCapacityListener);
        SettingsManager.removeListener(metricsFlushIntervalListener);
    }
    
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
