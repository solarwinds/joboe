package com.tracelytics.metrics.measurement;

import java.util.HashMap;
import java.util.Map;

import com.tracelytics.metrics.MetricKey;
import com.tracelytics.metrics.MetricsEntry;

/**
 * A {@link MetricsEntry} that holds a {@link SummaryMeasurement}
 * @author pluk
 *
 */
public class SummaryMeasurementMetricsEntry extends MeasurementMetricsEntry<SummaryMeasurement<?>>{
    public SummaryMeasurementMetricsEntry(MetricKey key, SummaryMeasurement<?> value) {
        super(key, value);
    }

    @Override
    public Map<String, Number> getSerializedKvs() {
        Map<String, Number> summaryKvs = new HashMap<String, Number>();
        
        summaryKvs.put("count", value.getCount());
        summaryKvs.put("sum", (Number) value.getSum());
        
        return summaryKvs;
    }
    

}
