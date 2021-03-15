package com.tracelytics.monitor.metrics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import com.tracelytics.agent.Agent;
import com.tracelytics.joboe.rpc.Client;
import com.tracelytics.joboe.rpc.ClientException;
import com.tracelytics.joboe.rpc.ClientLoggingCallback;
import com.tracelytics.joboe.rpc.Result;
import com.tracelytics.metrics.MetricsEntry;
import com.tracelytics.metrics.MetricsEntry.MetricsEntryType;
import com.tracelytics.monitor.SystemReporter;
import com.tracelytics.monitor.SystemReporterException;
import com.tracelytics.util.HostInfoUtils;


/**
 * {@code SystemReporter} for all metrics by converting metrics collected from {@link MetricsCollector} into metrics messages, and send them using the provided rpc client    
 * 
 * Take note that though <code>MetricsCollector</code> extracts metrics from several different sources, 
 * this reporter handles those metrics generically without making any special cases based on the source type.
 *  
 * 
 * @author Patson Luk
 *
 */
class MetricsReporter extends SystemReporter<MetricsCategory, List<? extends MetricsEntry<?>>> {
    static final int MAX_MEASUREMENT_ENTRY_COUNT = 2000;
    //static final int MAX_HISTOGRAM_ENTRY_COUNT = 100; //Do not limit histogram counts for now
    static final int MAX_METRIC_NAME_LENGTH = 255;
    static final int MAX_TAG_NAME_LENGTH = 64;
    static final int MAX_TAG_VALUE_LENGTH = 255;
    private Client rpcClient;
    private ClientLoggingCallback<Result> loggingCallback = new ClientLoggingCallback<Result>("post metrics");
    
    MetricsReporter(Client rpcClient) {
        this.rpcClient = rpcClient;
    }
    
    @Override
    public void reportData(Map<MetricsCategory, List<? extends MetricsEntry<?>>> collectedData, long interval) throws SystemReporterException {
        List<Map<String, Object>> measurementKeyValues = new ArrayList<Map<String, Object>>();
        List<Map<String, Object>> histogramKeyValues = new ArrayList<Map<String, Object>>();
        List<Map<String, Object>> customMetricsKeyValues =  new ArrayList<Map<String, Object>>();
        Map<String, Object> topLevelKeyValues = new HashMap<String, Object>();
        
        SortedMap<MetricsCategory, List<? extends MetricsEntry<?>>> sortedData = new TreeMap<MetricsCategory, List<? extends MetricsEntry<?>>>(collectedData);
        
        int measurementCount = 0;
        int histogramCount = 0;
        for (Entry<MetricsCategory, List<? extends MetricsEntry<?>>> collectedByMetricsType : sortedData.entrySet()) {
            for (MetricsEntry<?> metricsEntry : collectedByMetricsType.getValue()) {
                if (metricsEntry.getType() == MetricsEntryType.MEASUREMENT || metricsEntry.getType() == MetricsEntryType.HISTOGRAM) {
                    if (metricsEntry.getType() == MetricsEntryType.MEASUREMENT) {
                        if (++ measurementCount > MAX_MEASUREMENT_ENTRY_COUNT) {
                            continue; //do not proceed further if limit exceeded
                        }
                    } else {
                        histogramCount ++; //no limit for now
                    }
                    
                    if (metricsEntry.getType() == MetricsEntryType.MEASUREMENT) {
                        if (collectedByMetricsType.getKey() == MetricsCategory.CUSTOM) {
                            customMetricsKeyValues.add(extractKeyValues(metricsEntry));
                        } else {
                            measurementKeyValues.add(extractKeyValues(metricsEntry));
                        }
                    } else {
                        histogramKeyValues.add(extractKeyValues(metricsEntry));
                    }
                } else if (metricsEntry.getType() == MetricsEntryType.TOP_LEVEL) {
                    topLevelKeyValues.putAll(metricsEntry.getSerializedKvs());
                } else {
                    logger.warn("Unexpected metrics type : " + metricsEntry.getType());
                }
            }
        }
        
        if (measurementCount > MAX_MEASUREMENT_ENTRY_COUNT) {
            logger.warn("Dropped " + (measurementCount - MAX_MEASUREMENT_ENTRY_COUNT) + " measurement entries as the limit " + MAX_MEASUREMENT_ENTRY_COUNT + " was exceeded");
        } else {
            logger.debug("Going to report " + measurementCount + " Measurement metrics entries");
        }
        logger.debug("Going to report " + histogramCount + " Histogram metrics entries");

        List<Map<String, Object>> metricsMessages = new ArrayList<Map<String,Object>>();
        //build the non-custom metric message
        Map<String, Object> metricsMessage = new HashMap<String, Object>();
        
        if (!measurementKeyValues.isEmpty()) {
            metricsMessage.put("measurements", measurementKeyValues);
        }
        
        if (!histogramKeyValues.isEmpty()) {
        	metricsMessage.put("histograms", histogramKeyValues);   
        }
        
        metricsMessage.putAll(topLevelKeyValues);
        
        int flushInterval = (int)interval / 1000; //from millisec to second
        putInfoKeyValues(metricsMessage, flushInterval);
        
        metricsMessages.add(metricsMessage);
        
        //build the custom metric message
        if (!customMetricsKeyValues.isEmpty()) {
            Map<String, Object> customMetricsMessage = new HashMap<String, Object>();
            putInfoKeyValues(customMetricsMessage, flushInterval);
            
            customMetricsMessage.put("IsCustom", true);
            customMetricsMessage.put("measurements", customMetricsKeyValues);
            
            metricsMessages.add(customMetricsMessage);
        }

        try {
            rpcClient.postMetrics(metricsMessages, loggingCallback);
        } catch (ClientException e) {
            logger.debug("Failed to post metrics message : " + e.getMessage());
        }
    }
    
    private Map<String, Object> extractKeyValues(MetricsEntry<?> metricsEntry) {
        Map<String, Object> extractedKeyValues = new HashMap<String, Object>();
        String trimmedMetricName = metricsEntry.getName().length() <= MAX_METRIC_NAME_LENGTH ? metricsEntry.getName() : metricsEntry.getName().substring(0, MAX_METRIC_NAME_LENGTH);
        
        extractedKeyValues.put("name", trimmedMetricName);
        if (metricsEntry.getTags() != null && !metricsEntry.getTags().isEmpty()) { //only add tags KV if tags is non-empty
            Map<String, String> trimmedTags = new HashMap<String, String>(); //AO metrics only support String value for now
            for (Entry<String, ?> tagEntry : metricsEntry.getTags().entrySet()) {
                String trimmedKey = tagEntry.getKey().length() <= MAX_TAG_NAME_LENGTH ? tagEntry.getKey() : tagEntry.getKey().substring(0, MAX_TAG_NAME_LENGTH);
                Object trimmedValue;
                Object value = tagEntry.getValue();
                if (value != null) {
                    if (value instanceof String) {
                        String valueString = (String) value;
                        trimmedValue = valueString.length() <= MAX_TAG_VALUE_LENGTH ? valueString : valueString.substring(0, MAX_TAG_VALUE_LENGTH);
                    } else {
                        trimmedValue = value;
                    }
                } else {
                    logger.warn("Unexpected null tag value for metrics [" + metricsEntry.getName() + "] with tag [" + tagEntry.getKey() + "]" );
                    trimmedValue = null;
                }
                trimmedTags.put(trimmedKey, trimmedValue.toString());
            }
            extractedKeyValues.put("tags", trimmedTags);
        }
        extractedKeyValues.putAll(metricsEntry.getSerializedKvs());
        
        return extractedKeyValues;
    }
    
    /**
     * 
     * @param metricsMessage
     * @param flushInterval     flush interval in second
     */
    private void putInfoKeyValues(Map<String, Object> metricsMessage, int flushInterval) {
        metricsMessage.putAll(HostInfoUtils.getHostMetadata());
        
        metricsMessage.put("Timestamp_u", Agent.currentTimeStamp());
        
        metricsMessage.put("MetricsFlushInterval", flushInterval);
    }
}
