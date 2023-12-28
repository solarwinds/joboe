package com.solarwinds.joboe.metrics;

import com.solarwinds.joboe.core.config.ConfigContainer;
import com.solarwinds.joboe.core.config.ConfigProperty;
import com.solarwinds.joboe.core.metrics.MetricKey;
import com.solarwinds.joboe.core.metrics.MetricsEntry;
import com.solarwinds.joboe.core.metrics.histogram.HistogramMetricsEntry;
import com.solarwinds.joboe.metrics.histogram.MockHistogramAdapter;
import com.solarwinds.joboe.core.metrics.measurement.SimpleMeasurementMetricsEntry;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Test collecting from multiple source, we do not have to test specifically each of the actual collector as tests will created for each of them correspondingly
 * @author pluk
 *
 */
public class MetricsCollectorTest {

    @Test
    public void testCollectorsInit() throws Exception {
        ConfigContainer configs = new ConfigContainer();
        configs.putByStringValue(ConfigProperty.MONITOR_JMX_SCOPES, "{}");
        MetricsCollector metricsCollector = new MetricsCollector(configs);
       
        //modify the collector to use our testing source collectors
        Field collectorsField = MetricsCollector.class.getDeclaredField("collectors");
        collectorsField.setAccessible(true);
        Map<MetricsCategory, AbstractMetricsEntryCollector> collectorsMap = (Map<MetricsCategory, AbstractMetricsEntryCollector>) collectorsField.get(metricsCollector);
        
        for (MetricsCategory category : MetricsCategory.values()) {
            collectorsMap.containsKey(category);
        }
        
        configs.putByStringValue(ConfigProperty.MONITOR_JMX_ENABLE, "false");
        metricsCollector = new MetricsCollector(configs);
        collectorsMap = (Map<MetricsCategory, AbstractMetricsEntryCollector>) collectorsField.get(metricsCollector);
        assertFalse(collectorsMap.containsKey(MetricsCategory.JMX)); //should no longer has the JMX monitor
        
        configs.putByStringValue(ConfigProperty.MONITOR_SPAN_METRICS_ENABLE, "false");
        metricsCollector = new MetricsCollector(configs);
        collectorsMap = (Map<MetricsCategory, AbstractMetricsEntryCollector>) collectorsField.get(metricsCollector);
        assertFalse(collectorsMap.containsKey(MetricsCategory.SPAN_METRICS)); //should no longer has the historgram collector
    }
    
    
    @Test
    public void testCollect() throws Exception {
        ConfigContainer configs = new ConfigContainer();
        configs.putByStringValue(ConfigProperty.MONITOR_JMX_SCOPES, "{}");
        MetricsCollector metricsCollector = new MetricsCollector(configs);
       
        //modify the collector to use our testing source collectors
        Field collectorsField = MetricsCollector.class.getDeclaredField("collectors");
        collectorsField.setAccessible(true);
        Map<MetricsCategory, AbstractMetricsEntryCollector> collectorsMap = (Map<MetricsCategory, AbstractMetricsEntryCollector>) collectorsField.get(metricsCollector);
        
        collectorsMap.clear();
        
        //test collector that returns a mix of measurement and histogram
        final List<MetricsEntry<?>> testLayerCountMetrics = new ArrayList<MetricsEntry<?>>();
        testLayerCountMetrics.add(new SimpleMeasurementMetricsEntry("a", null, 1));
        testLayerCountMetrics.add(new SimpleMeasurementMetricsEntry("b", Collections.singletonMap("Layer", "b"), 100.123));
        testLayerCountMetrics.add(new HistogramMetricsEntry(new MetricKey("c", null), new MockHistogramAdapter(1.0, 2, 3, 4, 5, 6.0, 7, 8)));

        //test collector that only returns histograms
        final List<HistogramMetricsEntry> testLayerHistogramMetrics = new ArrayList<HistogramMetricsEntry>();
        testLayerHistogramMetrics.add(new HistogramMetricsEntry(new MetricKey("a", Collections.singletonMap("Layer", "a")), new MockHistogramAdapter(10.0, 20, 30, 40, 50, 60.0, 70, 80)));
        testLayerHistogramMetrics.add(new HistogramMetricsEntry(new MetricKey("b", null), new MockHistogramAdapter(100.0, 200, 300, 400, 500, 600.0, 700, 800)));
        
        collectorsMap.put(MetricsCategory.LAYER_COUNT, new AbstractMetricsEntryCollector() {
            @Override
            List<? extends MetricsEntry<?>> collectMetricsEntries() throws Exception {
                return testLayerCountMetrics;
            }
        });
        
        collectorsMap.put(MetricsCategory.SPAN_METRICS, new AbstractMetricsEntryCollector() {
            @Override
            List<HistogramMetricsEntry> collectMetricsEntries() throws Exception {
                return testLayerHistogramMetrics;
            }
        });
        
        Map<MetricsCategory, List<? extends MetricsEntry<?>>> collectedEntries = metricsCollector.collect();
        
        assertEquals(testLayerCountMetrics, collectedEntries.get(MetricsCategory.LAYER_COUNT));
        assertEquals(testLayerHistogramMetrics, collectedEntries.get(MetricsCategory.SPAN_METRICS));
    }
    
    /**
     * Test if one source of collection takes too long to complete
     * @throws Exception
     */
    @Test
    public void testCollectTimeout() throws Exception {
        ConfigContainer configs = new ConfigContainer();
        configs.putByStringValue(ConfigProperty.MONITOR_JMX_SCOPES, "{}");
        MetricsCollector metricsCollector = new MetricsCollector(configs);
       
        //modify the collector to use our testing source collectors
        Field collectorsField = MetricsCollector.class.getDeclaredField("collectors");
        collectorsField.setAccessible(true);
        Map<MetricsCategory, AbstractMetricsEntryCollector> collectorsMap = (Map<MetricsCategory, AbstractMetricsEntryCollector>) collectorsField.get(metricsCollector);
        
        collectorsMap.clear();
        
        //test collector that returns a mix of measurement and histogram
        final List<MetricsEntry<?>> testLayerCountMetrics = new ArrayList<MetricsEntry<?>>();
        testLayerCountMetrics.add(new SimpleMeasurementMetricsEntry("a", null, 1));
        testLayerCountMetrics.add(new SimpleMeasurementMetricsEntry("b", Collections.singletonMap("Layer", "b"), 100.123));
        testLayerCountMetrics.add(new HistogramMetricsEntry(new MetricKey("c", null), new MockHistogramAdapter(1.0, 2, 3, 4, 5, 6.0, 7, 8)));

        //test collector that only returns histograms
        final List<HistogramMetricsEntry> testLayerHistogramMetrics = new ArrayList<HistogramMetricsEntry>();
        testLayerHistogramMetrics.add(new HistogramMetricsEntry(new MetricKey("a", Collections.singletonMap("Layer", "a")), new MockHistogramAdapter(10.0, 20, 30, 40, 50, 60.0, 70, 80)));
        testLayerHistogramMetrics.add(new HistogramMetricsEntry(new MetricKey("b", null), new MockHistogramAdapter(100.0, 200, 300, 400, 500, 600.0, 700, 800)));
        
        collectorsMap.put(MetricsCategory.LAYER_COUNT, new AbstractMetricsEntryCollector() {
            @Override
            List<? extends MetricsEntry<?>> collectMetricsEntries() throws Exception {
                //make this really slow
                Thread.sleep(30000); //30 secs
                return testLayerCountMetrics;
            }
        });
        
        collectorsMap.put(MetricsCategory.SPAN_METRICS, new AbstractMetricsEntryCollector() {
            @Override
            List<HistogramMetricsEntry> collectMetricsEntries() throws Exception {
                return testLayerHistogramMetrics;
            }
        });
        
        Map<MetricsCategory, List<? extends MetricsEntry<?>>> collectedEntries = metricsCollector.collect();
        
        assertFalse(collectedEntries.containsKey(MetricsCategory.LAYER_COUNT));
        assertEquals(testLayerHistogramMetrics, collectedEntries.get(MetricsCategory.SPAN_METRICS)); //should still have histograms
    }
    
    
}
