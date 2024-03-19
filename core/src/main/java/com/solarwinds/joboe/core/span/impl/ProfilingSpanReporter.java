package com.solarwinds.joboe.core.span.impl;

import com.solarwinds.joboe.core.config.ConfigManager;
import com.solarwinds.joboe.core.config.ConfigProperty;
import com.solarwinds.joboe.logging.Logger;
import com.solarwinds.joboe.logging.LoggerFactory;
import com.solarwinds.joboe.core.config.ProfilerSetting;
import com.solarwinds.joboe.core.profiler.Profiler;
import com.solarwinds.joboe.sampling.Metadata;

/**
 * Span reporter that acts on span start and end to trigger profiling for this current thread.
 * 
 * @author pluk
 *
 */
public class ProfilingSpanReporter implements SpanReporter {
    private final Logger logger = LoggerFactory.getLogger();
    public static final ProfilingSpanReporter REPORTER = new ProfilingSpanReporter();
    
    private static final ProfilerSetting PROFILER_SETTING  = (ProfilerSetting) ConfigManager.getConfig(ConfigProperty.PROFILER);
    private static final boolean IS_ENABLED = PROFILER_SETTING != null && PROFILER_SETTING.isEnabled();

    private ProfilingSpanReporter() {
    }


    @Override
    public void reportOnStart(Span span) {
        if (IS_ENABLED && span.context().getMetadata().isSampled()) {
            boolean spanProfiled = Profiler.addProfiledThread(Thread.currentThread(), span.context().getMetadata(), Metadata.bytesToHex(span.context().getMetadata().getTaskID()));
            span.setTag("ProfilerStatus", Profiler.getStatus().toString());
            if (spanProfiled) {
                //increment the profile span count on the tracing span so it can report # of outstanding profiling span on exit
                span.getSpanPropertyValue(Span.SpanProperty.PROFILE_SPAN_COUNT).incrementAndGet();
            }

        }
    }
        
    @Override
    public void reportOnFinish(final Span span, long finishMicros) {
      //Cannot check for span.context().getMetadata().isSampled() unfortunately as TracingSpanReporter might have cleared the sampled flag already upon exit
      //which means such a check might actual prevent profiler from stopping properly on sampled requests  
        if (IS_ENABLED) {
            Profiler.stopProfile(Metadata.bytesToHex(span.context().getMetadata().getTaskID()));
        }
    }
    
    @Override
    public void reportOnLog(Span span, LogEntry logEntry) {
    }
}
