package com.tracelytics.test.pojo;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.appoptics.api.ext.AgentChecker;
import com.appoptics.api.ext.Metrics;

public class TestMetrics {
    public static void main(String[] args) {
        AgentChecker.waitUntilAgentReady(10, TimeUnit.SECONDS);
        Metrics.incrementMetric("i1", Collections.<String, String>emptyMap());
        Map<String, String> tags = new HashMap<String, String>();
        tags.put("tag1", "1");
        tags.put("tag2", "2");
        Metrics.incrementMetric("i2", tags);
        
        
        Metrics.summaryMetric("s1", 123, tags);
        Metrics.summaryMetric("s1", 222, tags);
    }
}
