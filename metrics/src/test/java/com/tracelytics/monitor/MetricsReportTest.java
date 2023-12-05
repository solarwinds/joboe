package com.tracelytics.monitor;

import com.tracelytics.joboe.TestRpcClient;
import com.tracelytics.joboe.TestSubmitRejectionRpcClient;
import com.tracelytics.joboe.rpc.Client;
import com.tracelytics.metrics.MetricKey;
import com.tracelytics.metrics.MetricsEntry;
import com.tracelytics.metrics.TopLevelMetricsEntry;
import com.tracelytics.metrics.histogram.HistogramMetricsEntry;
import com.tracelytics.metrics.histogram.MockHistogramAdapter;
import com.tracelytics.metrics.measurement.*;
import com.tracelytics.util.HostInfoUtils;
import com.tracelytics.util.HostInfoUtils.NetworkAddressInfo;
import com.tracelytics.util.ServerHostInfoReader;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.Map.Entry;

import static org.junit.jupiter.api.Assertions.*;

public class MetricsReportTest {
    private static final int INTERVAL = 30000; //30 sec
    static {
        HostInfoUtils.init(ServerHostInfoReader.INSTANCE);
    }

    @Test
    public void testReport() throws Exception {
        TestRpcClient client = new TestRpcClient(0);
        MetricsReporter reporter = new MetricsReporter(client);
        
        Map<MetricsCategory, List<? extends MetricsEntry<?>>> collectedEntries = new HashMap<MetricsCategory, List<? extends MetricsEntry<?>>>();

        List<MeasurementMetricsEntry<?>> measurementEntries = new ArrayList<MeasurementMetricsEntry<?>>();    
        List<HistogramMetricsEntry> histogramEntries = new ArrayList<HistogramMetricsEntry>();
        
        
        final List<MetricsEntry<?>> testSpanMetrics = new ArrayList<MetricsEntry<?>>();
        HistogramMetricsEntry histogramEntry;
        
        histogramEntry = new HistogramMetricsEntry("a", Collections.singletonMap("layer", "a"), new MockHistogramAdapter(10.0, 20, 30, 40, 50, 60.0, 70, 80));
        testSpanMetrics.add(histogramEntry);
        histogramEntries.add(histogramEntry);
        
        histogramEntry = new HistogramMetricsEntry("b", null, new MockHistogramAdapter(100.0, 200, 300, 400, 500, 600.0, 700, 800));
        testSpanMetrics.add(histogramEntry);
        histogramEntries.add(histogramEntry);
        
        testSpanMetrics.add(new TopLevelMetricsEntry<Boolean>(SpanMetricsCollector.TRANSACTION_NAME_OVERFLOW_LABEL, true));
        
        
        final List<MetricsEntry<?>> testLayerCountMetrics = new ArrayList<MetricsEntry<?>>();
        
        SimpleMeasurementMetricsEntry measurementEntry;
        
        measurementEntry = new SimpleMeasurementMetricsEntry("a", null, 1);
        testLayerCountMetrics.add(measurementEntry);
        measurementEntries.add(measurementEntry);
        
        measurementEntry = new SimpleMeasurementMetricsEntry("b", Collections.singletonMap("layer", "b"), 100.123);
        testLayerCountMetrics.add(measurementEntry);
        measurementEntries.add(measurementEntry);
        
        histogramEntry = new HistogramMetricsEntry("c", null, new MockHistogramAdapter(1.0, 2, 3, 4, 5, 6.0, 7, 8));
        testLayerCountMetrics.add(histogramEntry);
        histogramEntries.add(histogramEntry);

        //Custom metric entries
        final List<MetricsEntry<?>> testCustomMetrics = new ArrayList<MetricsEntry<?>>();
        SummaryMeasurement<Double> customMeasurement = new SummaryDoubleMeasurement();
        customMeasurement.recordValue(5.5);
        SummaryMeasurementMetricsEntry summaryMeasurementMetricsEntry = new SummaryMeasurementMetricsEntry(new MetricKey("custom-1", Collections.singletonMap("tag-1", "A")), customMeasurement);
        testCustomMetrics.add(summaryMeasurementMetricsEntry);
        
        //test collector that only returns histograms
        
        collectedEntries.put(MetricsCategory.SPAN_METRICS, testSpanMetrics);
        collectedEntries.put(MetricsCategory.LAYER_COUNT, testLayerCountMetrics);
        collectedEntries.put(MetricsCategory.CUSTOM, testCustomMetrics);
        
        reporter.reportData(collectedEntries, INTERVAL);
        
        Thread.sleep(1000); //give it some time to finish as it's asynchronous
        
        List<Map<String, Object>> postedMessages = client.getPostedMetrics();
        
        assertEquals(2, postedMessages.size()); //one for built-in metrics, and one for custom metrics
        
        Map<String, Object> postedBuiltinMetrics = postedMessages.get(0);
        
        assertBasicKeys(postedBuiltinMetrics);
        
        assertEquals(true, postedBuiltinMetrics.get(SpanMetricsCollector.TRANSACTION_NAME_OVERFLOW_LABEL));
        assertMetricEntries(measurementEntries, (List<Map<String, ?>>) postedBuiltinMetrics.get("measurements"));
        assertMetricEntries(histogramEntries, (List<Map<String, ?>>) postedBuiltinMetrics.get("histograms"));
        
        Map<String, Object> postedCustomMetrics = postedMessages.get(1);
        
        assertBasicKeys(postedCustomMetrics);
        
        assertEquals(null, postedCustomMetrics.get(SpanMetricsCollector.TRANSACTION_NAME_OVERFLOW_LABEL));
        assertMetricEntries(testCustomMetrics, (List<Map<String, ?>>) postedCustomMetrics.get("measurements"));
        assertEquals(null, (List<Map<String, ?>>) postedCustomMetrics.get("histograms"));
    }
    
    /**
     * when there are too many metric entry (exceeding MAX_MEASUREMENT_ENTRY_COUNT), it will then drop some entries. It will drop according to the natural ordering as defined in 
     * <code>MetricsCategory</code> (the last category will get dropped first etc), which in this case the jmx entries will be dropped first 
     * 
     * 
     * @throws Exception
     */
    @Test
    public void testReportTooManyJmx() throws Exception {
        TestRpcClient client = new TestRpcClient(0);
        MetricsReporter reporter = new MetricsReporter(client);
        
        Map<MetricsCategory, List<? extends MetricsEntry<?>>> collectedEntries = new HashMap<MetricsCategory, List<? extends MetricsEntry<?>>>();

            
        
        List<SimpleMeasurementMetricsEntry> testJmxEntries = new ArrayList<SimpleMeasurementMetricsEntry>();
        for (int i = 0 ; i < MetricsReporter.MAX_MEASUREMENT_ENTRY_COUNT; i ++) {
            testJmxEntries.add(new SimpleMeasurementMetricsEntry("jmx." + i, null, i));
        }
        
        List<SimpleMeasurementMetricsEntry> testLayerEntries = new ArrayList<SimpleMeasurementMetricsEntry>();
        final int layerMetricCount = 50;
        for (int i = 0 ; i < 50; i ++) {
            testLayerEntries.add(new SimpleMeasurementMetricsEntry("layer." + i, null, i));
        }
        
        //test collector that only returns histograms
        collectedEntries.put(MetricsCategory.JMX, testJmxEntries);
        collectedEntries.put(MetricsCategory.LAYER_COUNT, testLayerEntries);
        
        reporter.reportData(collectedEntries, INTERVAL);
        
        Thread.sleep(1000); //give it some time to finish as it's asynchronous
        
        List<Map<String, Object>> postedMessages = client.getPostedMetrics();
        
        assertEquals(1, postedMessages.size()); 
        
        Map<String, Object> postedMetrics = postedMessages.get(0);
        
        assertBasicKeys(postedMetrics); //basic keys should always be there
        
        List<Map<String, ?>> postedMeasurements = (List<Map<String, ?>>) postedMetrics.get("measurements");
        
        assertMetricEntries(testLayerEntries, postedMeasurements.subList(0, layerMetricCount)); //the layer entries should all make it as it has higher priority than JMX
        int expectedPostedJmxEntriesCount = MetricsReporter.MAX_MEASUREMENT_ENTRY_COUNT - layerMetricCount; //only a sublist of jmx entries can make it, the remaining counts left after the layer entries
        assertMetricEntries(testJmxEntries.subList(0, expectedPostedJmxEntriesCount), postedMeasurements.subList(layerMetricCount, postedMeasurements.size())); //only a sublist of jmx entries can make it
        
    }

    @Test
    public void testLongTags() throws Exception {
        final String longName = "0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789" +
                                   "0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789" +
                                   "0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789"; //300 characters
        final String longValue = "9876543210987654321098765432109876543210987654321098765432109876543210987654321098765432109876543210" +
                "98765432109876543210987654321098765432 10987654321098765432109876543210987654321098765432109876543210" +
                "9876543210987654321098765432109876543210987654321098765432109876543210987654321098765432109876543210"; //300 characters
        TestRpcClient client = new TestRpcClient(0);
        MetricsReporter reporter = new MetricsReporter(client);
        
        Map<MetricsCategory, List<? extends MetricsEntry<?>>> collectedEntries = new HashMap<MetricsCategory, List<? extends MetricsEntry<?>>>();

        List<SimpleMeasurementMetricsEntry> measurementEntries = new ArrayList<SimpleMeasurementMetricsEntry>();    
        List<HistogramMetricsEntry> histogramEntries = new ArrayList<HistogramMetricsEntry>();
        
        
        final List<HistogramMetricsEntry> testLayerHistogramMetrics = new ArrayList<HistogramMetricsEntry>();
        HistogramMetricsEntry histogramEntry;
        
        histogramEntry = new HistogramMetricsEntry("a", Collections.singletonMap(longName, longValue), new MockHistogramAdapter(10.0, 20, 30, 40, 50, 60.0, 70, 80));
        testLayerHistogramMetrics.add(histogramEntry);
        histogramEntries.add(histogramEntry);
        
        final List<MetricsEntry<?>> testLayerCountMetrics = new ArrayList<MetricsEntry<?>>();
        SimpleMeasurementMetricsEntry measurementEntry;
        
        measurementEntry = new SimpleMeasurementMetricsEntry(longName, Collections.singletonMap(longName, longValue), 100.123);
        testLayerCountMetrics.add(measurementEntry);
        measurementEntries.add(measurementEntry);
        
        collectedEntries.put(MetricsCategory.SPAN_METRICS, testLayerHistogramMetrics);
        collectedEntries.put(MetricsCategory.LAYER_COUNT, testLayerCountMetrics);
        
        reporter.reportData(collectedEntries, INTERVAL);
        
        Thread.sleep(1000); //give it some time to finish as it's asynchronous
        
        List<Map<String, Object>> postedMessages = client.getPostedMetrics();
        
        assertEquals(1, postedMessages.size());
        
        Map<String, Object> postedMetrics = postedMessages.get(0);
        
        assertBasicKeys(postedMetrics);
        
        assertMetricEntries(measurementEntries, (List<Map<String, ?>>) postedMetrics.get("measurements"));
        assertMetricEntries(histogramEntries, (List<Map<String, ?>>) postedMetrics.get("histograms"));
    }
    

    @Test
    public void testReportException() throws Exception {
        Client client = new TestSubmitRejectionRpcClient();
        MetricsReporter reporter = new MetricsReporter(client);
        
        Map<MetricsCategory, List<? extends MetricsEntry<?>>> collectedEntries = new HashMap<MetricsCategory, List<? extends MetricsEntry<?>>>();

        List<SimpleMeasurementMetricsEntry> measurementEntries = new ArrayList<SimpleMeasurementMetricsEntry>();    
        List<HistogramMetricsEntry> histogramEntries = new ArrayList<HistogramMetricsEntry>();
        
        final List<HistogramMetricsEntry> testLayerHistogramMetrics = new ArrayList<HistogramMetricsEntry>();
        HistogramMetricsEntry histogramEntry;
        
        histogramEntry = new HistogramMetricsEntry("a", Collections.singletonMap("layer", "a"), new MockHistogramAdapter(10.0, 20, 30, 40, 50, 60.0, 70, 80));
        testLayerHistogramMetrics.add(histogramEntry);
        histogramEntries.add(histogramEntry);
        
        histogramEntry = new HistogramMetricsEntry("b", null, new MockHistogramAdapter(100.0, 200, 300, 400, 500, 600.0, 700, 800));
        testLayerHistogramMetrics.add(histogramEntry);
        histogramEntries.add(histogramEntry);
        
        final List<MetricsEntry<?>> testLayerCountMetrics = new ArrayList<MetricsEntry<?>>();
        SimpleMeasurementMetricsEntry measurementEntry;
        
        measurementEntry = new SimpleMeasurementMetricsEntry("a", null, 1);
        testLayerCountMetrics.add(measurementEntry);
        measurementEntries.add(measurementEntry);
        
        measurementEntry = new SimpleMeasurementMetricsEntry("b", Collections.singletonMap("layer", "b"), 100.123);
        testLayerCountMetrics.add(measurementEntry);
        measurementEntries.add(measurementEntry);
        
        measurementEntry = new SimpleMeasurementMetricsEntry("b", Collections.singletonMap("layer", "b"), 100.123);
        testLayerCountMetrics.add(measurementEntry);
        measurementEntries.add(measurementEntry);
        
        histogramEntry = new HistogramMetricsEntry("c", null, new MockHistogramAdapter(1.0, 2, 3, 4, 5, 6.0, 7, 8));
        testLayerCountMetrics.add(histogramEntry);
        histogramEntries.add(histogramEntry);
        

        //client rejection is considered a minor problem and should not throw SystemReporterException 
        reporter.reportData(collectedEntries, INTERVAL);
    }
    
    private void assertMetricEntries(List<? extends MetricsEntry<?>> expectedEntries, List<Map<String, ?>> list) {
        assertEquals(expectedEntries.size(), list.size());
        for (int i = 0 ; i < expectedEntries.size(); i ++) {
            MetricsEntry<?> expectedMeasurement = expectedEntries.get(i);
            Map<String, ?> postedMeasurement = list.get(i);
            
            String expectedName = expectedMeasurement.getName();
            if (expectedName.length() > MetricsReporter.MAX_METRIC_NAME_LENGTH) {
                expectedName = expectedName.substring(0, MetricsReporter.MAX_METRIC_NAME_LENGTH);
            }
            assertEquals(expectedName, postedMeasurement.get("name"));
            for (Entry<String, ?> expectedKv : expectedMeasurement.getSerializedKvs().entrySet()) {
                assertEquals(expectedKv.getValue(), postedMeasurement.get(expectedKv.getKey()));
            }
            
            //check tags
            Map<String, String> postedTags = (Map<String, String>) postedMeasurement.get("tags");
            if (expectedMeasurement.getTags() == null) {
                assertNull(postedTags);
            } else {
                assertEquals(expectedMeasurement.getTags().size(), postedTags.size());
                for (String expectedTagKey : expectedMeasurement.getTags().keySet()) {
                    Object expectedTagValue = expectedMeasurement.getTags().get(expectedTagKey);
                    if (expectedTagKey.length() > MetricsReporter.MAX_TAG_NAME_LENGTH) {
                        expectedTagKey = expectedTagKey.substring(0, MetricsReporter.MAX_TAG_NAME_LENGTH);
                    }

                    if (expectedTagValue instanceof String) {
                        if (((String) expectedTagValue).length() > MetricsReporter.MAX_TAG_VALUE_LENGTH) {
                            expectedTagValue = ((String) expectedTagValue).substring(0, MetricsReporter.MAX_TAG_VALUE_LENGTH);
                        }
                    }
                    assertEquals(expectedTagValue, postedTags.get(expectedTagKey));
                }
            }
        }
    }

    private void assertBasicKeys(Map<String, Object> postedMetrics) {
        assertTrue(postedMetrics.containsKey("Timestamp_u"));
        assertTrue(postedMetrics.containsKey("UnameSysName"));
        assertTrue(postedMetrics.containsKey("UnameVersion"));
        HostInfoUtils.OsType osType = HostInfoUtils.getOsType();
        if (osType == HostInfoUtils.OsType.WINDOWS || osType == HostInfoUtils.OsType.LINUX) {
            assertTrue(postedMetrics.containsKey("Distro"));
        }
        NetworkAddressInfo networkInfo = HostInfoUtils.getNetworkAddressInfo();
        if (networkInfo != null) {
            if (!networkInfo.getIpAddresses().isEmpty()) {
                assertEquals(networkInfo.getIpAddresses(), postedMetrics.get("IPAddresses"));
            }
        }
        
        assertEquals(INTERVAL / 1000, postedMetrics.get("MetricsFlushInterval")); //from millisec to sec
    }
}
