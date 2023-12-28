package com.solarwinds.joboe.core.span.impl;

import java.util.List;

import com.solarwinds.joboe.core.config.ConfigManager;
import com.solarwinds.joboe.core.config.ConfigProperty;
import com.solarwinds.joboe.core.logging.Logger;
import com.solarwinds.joboe.core.logging.LoggerFactory;
import com.solarwinds.joboe.core.metrics.MetricsEntry;

/**
 * Records histograms and measurements metrics when span finishes
 * @author pluk
 *
 */
public abstract class MetricSpanReporter implements SpanReporter {
    private final boolean spanMetricsEnabled;

    protected static final Logger logger = LoggerFactory.getLogger();
    protected static final long MAX_DURATION = 60L * 60 * 1000 * 1000; //1hr
    protected static final long MIN_DURATION = 0;

    protected MetricSpanReporter() {
        // disable histogram collection if it's explicitly set to disable
        Boolean configValue = (Boolean) ConfigManager.getConfig(ConfigProperty.MONITOR_SPAN_METRICS_ENABLE);
        this.spanMetricsEnabled = !Boolean.FALSE.equals(configValue);


    }
    
    protected final boolean isSpanMetricsEnabled() {
        return spanMetricsEnabled;
    }
        
    @Override
    public final void reportOnStart(Span span) {
        
    }

    /**
     * Records the span duration as a histogram and a measurement
     */
    @Override
    public final void reportOnFinish(Span span, long finishMicros) {
        if (spanMetricsEnabled && span.context().getMetadata().isReportMetrics()) {
            long duration = finishMicros - span.getStart();
            
            if (duration >= MIN_DURATION && duration <= MAX_DURATION) {
                reportMetrics(span, duration);
            } else { //do not report 
                logger.debug("Skip reporting metric duration [" + duration + "] as it is not within valid range of [" +  MIN_DURATION + " - " + MAX_DURATION + "]");
            }
        }
    }
    
    protected abstract void reportMetrics(Span span, long duration);
    
    @Override
    public final void reportOnLog(Span span, LogEntry logEntry) {
        // do nothing for now. Might want to accumulate log entries in the future?
    }
    
    public abstract List<MetricsEntry<?>> consumeMetricEntries();
}