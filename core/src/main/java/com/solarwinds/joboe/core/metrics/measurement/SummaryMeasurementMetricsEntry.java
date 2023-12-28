package com.solarwinds.joboe.core.metrics.measurement;

import java.util.HashMap;
import java.util.Map;

import com.solarwinds.joboe.core.metrics.MetricsEntry;
import com.solarwinds.joboe.core.metrics.MetricKey;

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
        summaryKvs.put("sum", value.getSum());
        
        return summaryKvs;
    }
    

}
