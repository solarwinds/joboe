package com.tracelytics.monitor.metrics;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.tracelytics.joboe.JoboeTest;
import com.tracelytics.metrics.measurement.SimpleMeasurementMetricsEntry;

public class SystemMetricsCollectorTest extends JoboeTest {
    public void testCollect() throws Exception {
        SystemMetricsCollector collector = new SystemMetricsCollector();
        
        List<SimpleMeasurementMetricsEntry> entries = collector.collectMetricsEntries();
        Map<String, SimpleMeasurementMetricsEntry> entriesByName = new HashMap<String, SimpleMeasurementMetricsEntry>();
        
        for (SimpleMeasurementMetricsEntry entry : entries) {
            entriesByName.put(entry.getName(), entry);
        }
        
        assertTrue(entriesByName.containsKey("TotalRAM"));
        assertTrue(entriesByName.containsKey("FreeRAM"));
        assertTrue(entriesByName.containsKey("Load1"));
        assertTrue(entriesByName.containsKey("ProcessLoad"));
        assertTrue(entriesByName.containsKey("ProcessRAM"));
    }
}
