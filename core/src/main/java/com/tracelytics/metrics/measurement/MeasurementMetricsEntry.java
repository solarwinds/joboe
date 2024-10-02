package com.tracelytics.metrics.measurement;

import com.tracelytics.metrics.MetricKey;
import com.tracelytics.metrics.MetricsEntry;

/**
 * A {@link MetricsEntry} for {@link Measurement}
 * @author pluk
 *
 * @param <T>   type of the value
 */
public abstract class MeasurementMetricsEntry<T> extends MetricsEntry<T>{
    protected MeasurementMetricsEntry(MetricKey key, T value) {
        super(key, value);
    }

    @Override
    public MetricsEntryType getType() {
        return MetricsEntryType.MEASUREMENT;
    }

    protected static boolean isNaN(Object value) {
        //do NOT use value != value, as it does NOT work in Java
        if (value instanceof Double) {
            return ((Double) value).isNaN();
        } else if (value instanceof Float) {
            return ((Float) value).isNaN();
        } else {
            return false;
        }
    }
}
