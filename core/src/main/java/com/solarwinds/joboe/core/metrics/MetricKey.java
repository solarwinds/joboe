package com.solarwinds.joboe.core.metrics;

import lombok.Getter;

import java.util.Map;

@Getter
public class MetricKey {
    private final String stringKey;
    private final Map<String, ?> tags;

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
            return other.tags == null;
        } else return tags.equals(other.tags);
    }

    @Override
    public String toString() {
        return "MetricKey [stringKey=" + stringKey + ", tags=" + tags + "]";
    }
}
