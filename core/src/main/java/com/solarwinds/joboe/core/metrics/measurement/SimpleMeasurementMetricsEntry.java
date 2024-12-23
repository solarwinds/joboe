package com.solarwinds.joboe.core.metrics.measurement;

import java.util.Collections;
import java.util.Map;

import com.solarwinds.joboe.logging.Logger;
import com.solarwinds.joboe.logging.LoggerFactory;
import com.solarwinds.joboe.core.metrics.MetricsEntry;
import com.solarwinds.joboe.core.metrics.MetricKey;

/**
 * A {@link MetricsEntry} that holds a single numeric value
 * @author pluk
 *
 */
public class SimpleMeasurementMetricsEntry extends MeasurementMetricsEntry<Number>{
    private static final Logger logger = LoggerFactory.getLogger();
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
            logger.debug("Not serializing this entry as it's NaN: " + this);
            return Collections.emptyMap();
        } else {
            return Collections.singletonMap("value", value);
        }
    }
    

}
