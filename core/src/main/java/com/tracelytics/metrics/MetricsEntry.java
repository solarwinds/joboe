package com.tracelytics.metrics;

import java.util.Map;


/**
 * Represents a metrics entry which contains a {@code MetricsKey} and a value with type specified by the child class.
 * 
 * Metrics collected by various {@code AbstractMetricsEntryCollector} are all converted into this metric entry format for more generic handling in the {@code MetricsReporter}  
 * 
 * @author pluk
 *
 * @param <T>   Type of the metric value
 */
public abstract class MetricsEntry<T> {
    private MetricKey key;
    protected T value;
    
    public MetricsEntry(MetricKey key, T value) {
        super();
        this.key = key;
        this.value = value;
    }
    
    public String getName() {
        return key.getStringKey();
    }
    
    public Map<String, ?> getTags() {
        return key.getTags();
    }
    
    public MetricKey getKey() {
        return key;
    }
    
    public T getValue() {
        return value;
    }
    
    /**
     * Generate a map of key/values on the stored value in this metrics entry
     * @return
     */
    public abstract Map<String, ?> getSerializedKvs();

    
    public abstract MetricsEntryType getType();
    
    public static enum MetricsEntryType {
        MEASUREMENT, HISTOGRAM, TOP_LEVEL;
    }
    
    @Override
    public String toString() {
    	return "[" + getType() + "] " + key + " : " + value;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((key == null) ? 0 : key.hashCode());
        result = prime * result + ((value == null) ? 0 : value.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        MetricsEntry other = (MetricsEntry) obj;
        if (key == null) {
            if (other.key != null)
                return false;
        } else if (!key.equals(other.key))
            return false;
        if (value == null) {
            if (other.value != null)
                return false;
        } else if (!value.equals(other.value))
            return false;
        return true;
    }
}
