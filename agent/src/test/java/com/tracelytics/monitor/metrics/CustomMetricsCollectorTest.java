package com.tracelytics.monitor.metrics;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.tracelytics.agent.Agent;
import com.tracelytics.joboe.JoboeTest;
import com.tracelytics.joboe.TracingMode;
import com.tracelytics.joboe.settings.SettingsArg;
import com.tracelytics.joboe.settings.SettingsManager;
import com.tracelytics.joboe.settings.SimpleSettingsFetcher;
import com.tracelytics.joboe.settings.TestSettingsReader.SettingsMockupBuilder;
import com.tracelytics.metrics.MetricKey;
import com.tracelytics.metrics.MetricsEntry;
import com.tracelytics.metrics.measurement.SummaryDoubleMeasurement;

/**
 * Test sub metrics collector from custom metrics
 * @author pluk
 *
 */
public class CustomMetricsCollectorTest extends JoboeTest {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        CustomMetricsCollector.INSTANCE.collectMetricsEntries(); //clear everything
    }
    
    @Override
    protected void tearDown() throws Exception {
        CustomMetricsCollector.INSTANCE.reset();
        super.tearDown();
    }
    
    public void testIncrementMetrics() throws Exception {
        CustomMetricsCollector.INSTANCE.recordIncrementMetrics("test-1", 1, null);
        CustomMetricsCollector.INSTANCE.recordIncrementMetrics("test-1", 2, null);
        
        CustomMetricsCollector.INSTANCE.recordIncrementMetrics("test-1", 1, Collections.singletonMap("key-1", "1"));
        CustomMetricsCollector.INSTANCE.recordIncrementMetrics("test-1", 2, Collections.singletonMap("key-1", "1"));
        CustomMetricsCollector.INSTANCE.recordIncrementMetrics("test-1", 3, Collections.singletonMap("key-1", "1"));
        
        CustomMetricsCollector.INSTANCE.recordIncrementMetrics("test-1", 2, Collections.singletonMap("key-1", "2"));
        
        Map<String, String> tags = new HashMap<String, String>();
        tags.put("key-1", "1");
        tags.put("key-2", "2");
        CustomMetricsCollector.INSTANCE.recordIncrementMetrics("test-1", 3, tags);
        
        CustomMetricsCollector.INSTANCE.recordIncrementMetrics("test-2", 1,null);
        
        List<? extends MetricsEntry<?>> collectMetricsEntries = CustomMetricsCollector.INSTANCE.collectMetricsEntries();
        assertEquals(5, collectMetricsEntries.size());
        Map<MetricKey, Long> entriesByMetricsKey = new HashMap<MetricKey, Long>();
        
        for (MetricsEntry<?> entry : collectMetricsEntries) {
            entriesByMetricsKey.put(entry.getKey(), (Long) entry.getValue());
        }
        
        assertEquals(3, (long) entriesByMetricsKey.get(new MetricKey("test-1", null)));
        assertEquals(6, (long) entriesByMetricsKey.get(new MetricKey("test-1", Collections.singletonMap("key-1", "1"))));
        assertEquals(2, (long) entriesByMetricsKey.get(new MetricKey("test-1", Collections.singletonMap("key-1", "2"))));
        assertEquals(3, (long) entriesByMetricsKey.get(new MetricKey("test-1", tags)));
        assertEquals(1, (long) entriesByMetricsKey.get(new MetricKey("test-2", null)));
    }
    
    public void testSummaryMetrics() throws Exception {
        CustomMetricsCollector.INSTANCE.recordSummaryMetric("test-1", 1, 1, null);
        CustomMetricsCollector.INSTANCE.recordSummaryMetric("test-1", 1, 2, null);
        
        CustomMetricsCollector.INSTANCE.recordSummaryMetric("test-1", 1, 1, Collections.singletonMap("key-1", "1"));
        CustomMetricsCollector.INSTANCE.recordSummaryMetric("test-1", 1, 2, Collections.singletonMap("key-1", "1"));
        CustomMetricsCollector.INSTANCE.recordSummaryMetric("test-1", 1, 3, Collections.singletonMap("key-1", "1"));
        
        CustomMetricsCollector.INSTANCE.recordSummaryMetric("test-1", 1, 2, Collections.singletonMap("key-1", "2"));
        
        Map<String, String> tags = new HashMap<String, String>();
        tags.put("key-1", "1");
        tags.put("key-2", "2");
        CustomMetricsCollector.INSTANCE.recordSummaryMetric("test-1", 3, 1, tags);
        
        CustomMetricsCollector.INSTANCE.recordSummaryMetric("test-2", 4, 1, null);
        
        List<? extends MetricsEntry<?>> collectMetricsEntries = CustomMetricsCollector.INSTANCE.collectMetricsEntries();
        assertEquals(5, collectMetricsEntries.size());
        Map<MetricKey, SummaryDoubleMeasurement> entriesByMetricsKey = new HashMap<MetricKey, SummaryDoubleMeasurement>();
        
        for (MetricsEntry<?> entry : collectMetricsEntries) {
            entriesByMetricsKey.put(entry.getKey(), (SummaryDoubleMeasurement) entry.getValue());
        }
        
        assertEquals(2.0, entriesByMetricsKey.get(new MetricKey("test-1", null)).getSum());
        assertEquals(3, entriesByMetricsKey.get(new MetricKey("test-1", null)).getCount());
        assertEquals(3.0, entriesByMetricsKey.get(new MetricKey("test-1", Collections.singletonMap("key-1", "1"))).getSum());
        assertEquals(6, entriesByMetricsKey.get(new MetricKey("test-1", Collections.singletonMap("key-1", "1"))).getCount());
        assertEquals(1.0, entriesByMetricsKey.get(new MetricKey("test-1", Collections.singletonMap("key-1", "2"))).getSum());
        assertEquals(2, entriesByMetricsKey.get(new MetricKey("test-1", Collections.singletonMap("key-1", "2"))).getCount());
        assertEquals(3.0, entriesByMetricsKey.get(new MetricKey("test-1", tags)).getSum());
        assertEquals(1, entriesByMetricsKey.get(new MetricKey("test-1", tags)).getCount());
        assertEquals(4.0, entriesByMetricsKey.get(new MetricKey("test-2", null)).getSum());
        assertEquals(1, entriesByMetricsKey.get(new MetricKey("test-2", null)).getCount());
    }
    
    public void testMetricLimit() {
        for (int i = 0 ; i < CustomMetricsCollector.limit; i ++) {
            CustomMetricsCollector.INSTANCE.recordIncrementMetrics("test" + i, 1, null);
        }
        
        CustomMetricsCollector.INSTANCE.recordIncrementMetrics("test0", 1, null); //OK as this name already exists
        CustomMetricsCollector.INSTANCE.recordIncrementMetrics("test" + CustomMetricsCollector.limit, 1, null); //NOT OK as this name is new and limit has reached
        
        List<? extends MetricsEntry<?>> collectMetricsEntries = CustomMetricsCollector.INSTANCE.collectMetricsEntries();
        
        assertEquals(CustomMetricsCollector.limit, collectMetricsEntries.size());
        
        Map<MetricKey, Long> entriesByMetricsKey = new HashMap<MetricKey, Long>();
        
        for (MetricsEntry<?> entry : collectMetricsEntries) {
            entriesByMetricsKey.put(entry.getKey(), (Long) entry.getValue());
        }
        
        assertEquals(2, (long) entriesByMetricsKey.get(new MetricKey("test0", null))); //once from loop and another one in the manual increment
        assertTrue(!entriesByMetricsKey.containsKey(new MetricKey("test" + CustomMetricsCollector.limit, null))); //not exist as the entry should be dropped due to limit
        
        //simulate a limit change (lower)
        SimpleSettingsFetcher fetcher = (SimpleSettingsFetcher) SettingsManager.getFetcher();
        testSettingsReader.put(new SettingsMockupBuilder().withFlags(TracingMode.ALWAYS).withSampleRate(Agent.SAMPLE_RESOLUTION).withSettingsArg(SettingsArg.MAX_CUSTOM_METRICS, 1).build());
        CustomMetricsCollector.INSTANCE.recordIncrementMetrics("test-1", 1, null); //ok
        CustomMetricsCollector.INSTANCE.recordIncrementMetrics("test-2", 1, null); //not ok, limit exceeded
        
        //simulate a limit change (no override, reverts back to default limit)
        testSettingsReader.put(new SettingsMockupBuilder().withFlags(TracingMode.ALWAYS).withSampleRate(Agent.SAMPLE_RESOLUTION).build());
        CustomMetricsCollector.INSTANCE.recordIncrementMetrics("test-3", 1, null); //ok now as limit has increased (reverted to default)
        
        collectMetricsEntries = CustomMetricsCollector.INSTANCE.collectMetricsEntries();
        entriesByMetricsKey = new HashMap<MetricKey, Long>();
        
        for (MetricsEntry<?> entry : collectMetricsEntries) {
            entriesByMetricsKey.put(entry.getKey(), (Long) entry.getValue());
        }
        
        assertTrue(entriesByMetricsKey.containsKey(new MetricKey("test-1", null)));
        assertTrue(!entriesByMetricsKey.containsKey(new MetricKey("test-2", null)));
        assertTrue(entriesByMetricsKey.containsKey(new MetricKey("test-3", null)));
    }
    
    public void testTagLimit() {
        Map<String, String> tags = new HashMap<String, String>();
        for (int i = 0 ; i < CustomMetricsCollector.TAGS_LIMIT; i ++) {
            tags.put(String.valueOf(i), String.valueOf(i));
        }
        
        CustomMetricsCollector.INSTANCE.recordIncrementMetrics("test", 1, tags); //OK as the tags limit is not exceeded yet
        List<? extends MetricsEntry<?>> collectMetricsEntries = CustomMetricsCollector.INSTANCE.collectMetricsEntries();
        assertEquals(1, collectMetricsEntries.size());
        assertEquals(tags, collectMetricsEntries.get(0).getTags());
        
        //now add 1 more tag
        tags.put("extra", "tag");
        CustomMetricsCollector.INSTANCE.recordIncrementMetrics("test", 1, tags); //NOT OK as the tags limit exceeded
        collectMetricsEntries = CustomMetricsCollector.INSTANCE.collectMetricsEntries();
        assertTrue(collectMetricsEntries.isEmpty());
    }
}
