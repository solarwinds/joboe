package com.solarwinds.joboe.metrics;

import com.solarwinds.joboe.config.ConfigContainer;
import com.solarwinds.joboe.config.ConfigProperty;
import com.solarwinds.joboe.config.InvalidConfigException;
import com.solarwinds.joboe.core.metrics.MetricsEntry;
import com.solarwinds.joboe.core.util.DaemonThreadFactory;
import com.solarwinds.joboe.logging.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Collects metrics by maintain a list of sub metric entry collector of type {@link AbstractMetricsEntryCollector}. 
 * 
 * {@link MetricsCategory} outlines a list of possible metrics available, though the actual metric types collected depends on configurations
 * 
 * 
 * @author pluk
 *
 */
public class MetricsCollector extends SystemCollector<MetricsCategory, List<? extends MetricsEntry<?>>> {
    private final Map<MetricsCategory, AbstractMetricsEntryCollector> collectors = new HashMap<MetricsCategory, AbstractMetricsEntryCollector>();
    private final ExecutorService executorService;
    private static final int MAX_WAIT_TIME = 10; //max wait time for a collection task, in terms of second


    public MetricsCollector(ConfigContainer configs) {
        this(configs, null);
    }

    public MetricsCollector(ConfigContainer configs, SpanMetricsCollector spanMetricsCollector) {
        collectors.put(MetricsCategory.SYSTEM, new SystemMetricsCollector());
        if (configs.get(ConfigProperty.MONITOR_SPAN_METRICS_ENABLE) == null || (Boolean)configs.get(ConfigProperty.MONITOR_SPAN_METRICS_ENABLE)) { //default as true
            if (spanMetricsCollector == null) {
                collectors.put(MetricsCategory.SPAN_METRICS, new SpanMetricsCollector(null));
            } else {
                collectors.put(MetricsCategory.SPAN_METRICS, spanMetricsCollector);
            }
        }

        collectors.put(MetricsCategory.LAYER_COUNT, new TraceDecisionMetricsCollector());
        if (configs.get(ConfigProperty.MONITOR_JMX_ENABLE) == null || (Boolean)configs.get(ConfigProperty.MONITOR_JMX_ENABLE)) {
            LoggerFactory.getLogger().warn("JMX Metrics Collector has been removed. This will fail in future release.");
        }
        
        collectors.put(MetricsCategory.CUSTOM, CustomMetricsCollector.INSTANCE);
        executorService = Executors.newCachedThreadPool(DaemonThreadFactory.newInstance("metrics-collector"));
    }

    public void addCollector(MetricsCategory category, AbstractMetricsEntryCollector collector) {
        collectors.put(category, collector);
    }

    public void removeCollector(MetricsCategory category) {
        collectors.remove(category);
    }

    @Override
    protected Map<MetricsCategory, List<? extends MetricsEntry<?>>> collect() throws Exception {
        Map<MetricsCategory, Future<List<? extends MetricsEntry<?>>>> collectedFutures = new HashMap<MetricsCategory, Future<List<? extends MetricsEntry<?>>>>();
        Map<MetricsCategory, List<? extends MetricsEntry<?>>> entriesFromAllCollectors = new HashMap<MetricsCategory, List<? extends MetricsEntry<?>>>();
        
        for (final Entry<MetricsCategory, AbstractMetricsEntryCollector> collectorEntry : collectors.entrySet()) {
            final AbstractMetricsEntryCollector collector = collectorEntry.getValue();
            
            //asynchronously call all sub metric entry collectors to collect metrics
            Future<List<? extends MetricsEntry<?>>> collectedFuture = executorService.submit(() -> {
                List<? extends MetricsEntry<?>> collectedEntries = collector.collectMetricsEntries();
                return collectedEntries;
            });
            
            collectedFutures.put(collectorEntry.getKey(), collectedFuture);
        }
        

        //rather simple but naive implementation to iterate each task and wait. Can be improved if jdk 8 is used...
        for (Entry<MetricsCategory, Future<List<? extends MetricsEntry<?>>>> futureEntry : collectedFutures.entrySet()) {
            try {
                entriesFromAllCollectors.put(futureEntry.getKey(), futureEntry.getValue().get(MAX_WAIT_TIME, TimeUnit.SECONDS));
            } catch (Exception e) {
                logger.warn("Failed to collect info for " + futureEntry.getKey() + ", skipping... Error message: " + e.getMessage(), e);
            }
        }
        
        return entriesFromAllCollectors;
    }
}
