package com.tracelytics.metrics.measurement;

import java.util.Collections;
import java.util.Map;

import com.tracelytics.logging.Logger;
import com.tracelytics.logging.LoggerFactory;
import com.tracelytics.metrics.MetricKey;
import com.tracelytics.metrics.MetricsEntry;

/**
 * A {@link MetricsEntry} that holds a single numeric value
 * @author pluk
 *
 */
public class SimpleMeasurementMetricsEntry extends MeasurementMetricsEntry<Number>{
    private static Logger logger = LoggerFactory.getLogger();
    public SimpleMeasurementMetricsEntry(String name, Number value) {
        this(name, null, value);
    }
    public SimpleMeasurementMetricsEntry(String name, Map<String, ?> tags, Number value) {
        this(new MetricKey(name, tags), value);
    }
    public SimpleMeasurementMetricsEntry(MetricKey key, Number value) {
        super(key, value);
    }
    
    @Override
    public Map<String, Number> getSerializedKvs() {
        if (isNaN(value)) {
            logger.debug("Not serializing this entry as it's NaN: " + toString());
            return Collections.emptyMap();
        } else {
            return Collections.singletonMap("value", value);
        }
    }
    

}
