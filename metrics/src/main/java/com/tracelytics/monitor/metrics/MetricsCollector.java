package com.tracelytics.monitor.metrics;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.*;

import com.tracelytics.joboe.config.ConfigContainer;
import com.tracelytics.joboe.config.ConfigProperty;
import com.tracelytics.joboe.config.InvalidConfigException;
import com.tracelytics.metrics.MetricsEntry;
import com.tracelytics.monitor.SystemCollector;
import com.tracelytics.util.DaemonThreadFactory;

/**
 * Collects metrics by maintain a list of sub metric entry collector of type {@link AbstractMetricsEntryCollector}. 
 * 
 * {@link MetricsCategory} outlines a list of possible metrics available, though the actual metric types collected depends on configurations
 * 
 * 
 * @author pluk
 *
 */
class MetricsCollector extends SystemCollector<MetricsCategory, List<? extends MetricsEntry<?>>> {
    private Map<MetricsCategory, AbstractMetricsEntryCollector> collectors = new HashMap<MetricsCategory, AbstractMetricsEntryCollector>();
    private ExecutorService executorService; 
    private static final int MAX_WAIT_TIME = 10; //max wait time for a collection task, in terms of second
    
    
    public MetricsCollector(ConfigContainer configs) throws InvalidConfigException {
        collectors.put(MetricsCategory.SYSTEM, new SystemMetricsCollector());
        collectors.put(MetricsCategory.TRACING_REPORTER, new TracingReporterMetricsCollector());
        
        if (configs.get(ConfigProperty.MONITOR_SPAN_METRICS_ENABLE) == null || (Boolean)configs.get(ConfigProperty.MONITOR_SPAN_METRICS_ENABLE)) { //default as true
        	collectors.put(MetricsCategory.SPAN_METRICS, new SpanMetricsCollector());
        }
        collectors.put(MetricsCategory.LAYER_COUNT, new TraceDecisionMetricsCollector());
        
        if (configs.get(ConfigProperty.MONITOR_JMX_ENABLE) == null || (Boolean)configs.get(ConfigProperty.MONITOR_JMX_ENABLE)) {
            collectors.put(MetricsCategory.JMX, new JMXCollector(configs));
        }
        
        collectors.put(MetricsCategory.CUSTOM, CustomMetricsCollector.INSTANCE);
                
        executorService = Executors.newCachedThreadPool(DaemonThreadFactory.newInstance("metrics-collector"));
    }
    
    @Override
    protected Map<MetricsCategory, List<? extends MetricsEntry<?>>> collect() throws Exception {
        Map<MetricsCategory, Future<List<? extends MetricsEntry<?>>>> collectedFutures = new HashMap<MetricsCategory, Future<List<? extends MetricsEntry<?>>>>();
        Map<MetricsCategory, List<? extends MetricsEntry<?>>> entriesFromAllCollectors = new HashMap<MetricsCategory, List<? extends MetricsEntry<?>>>();
        
        for (final Entry<MetricsCategory, AbstractMetricsEntryCollector> collectorEntry : collectors.entrySet()) {
            final AbstractMetricsEntryCollector collector = collectorEntry.getValue();
            
            //asynchronously call all sub metric entry collectors to collect metrics
            Future<List<? extends MetricsEntry<?>>> collectedFuture = executorService.submit(new Callable<List<? extends MetricsEntry<?>>>() {
                public List<? extends MetricsEntry<?>> call() throws Exception {
                    List<? extends MetricsEntry<?>> collectedEntries = collector.collectMetricsEntries();
                    return collectedEntries;
                }
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
