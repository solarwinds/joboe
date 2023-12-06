package com.solarwinds.metrics;

import java.util.Map;

public class MetricKey {
    private String stringKey;
    private Map<String, ?> tags;

    public MetricKey(String stringKey, Map<String, ?> tags) {
        super();
        this.stringKey = stringKey;
        this.tags = tags;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((stringKey == null) ? 0 : stringKey.hashCode());
        result = prime * result + ((tags == null) ? 0 : tags.hashCode());
        return result;
    }
    
    public String getStringKey() {
        return stringKey;
    }
    
    public Map<String, ?> getTags() {
        return tags;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        MetricKey other = (MetricKey) obj;
        if (stringKey == null) {
            if (other.stringKey != null)
                return false;
        } else if (!stringKey.equals(other.stringKey))
            return false;
        if (tags == null) {
            if (other.tags != null)
                return false;
        } else if (!tags.equals(other.tags))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "MetricKey [stringKey=" + stringKey + ", tags=" + tags + "]";
    }
}
