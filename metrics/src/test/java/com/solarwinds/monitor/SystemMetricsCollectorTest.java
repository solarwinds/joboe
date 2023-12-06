package com.solarwinds.monitor;

import com.solarwinds.metrics.measurement.SimpleMeasurementMetricsEntry;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class SystemMetricsCollectorTest {

    @Test
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
