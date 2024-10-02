package com.tracelytics.joboe.span.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.tracelytics.joboe.*;
import com.tracelytics.joboe.config.ConfigManager;
import com.tracelytics.joboe.config.ConfigProperty;
import com.tracelytics.joboe.span.impl.Span.SpanProperty;
import com.tracelytics.joboe.span.impl.Span.TraceProperty;
import com.tracelytics.logging.Logger;
import com.tracelytics.logging.LoggerFactory;
import com.tracelytics.joboe.config.ProfilerSetting;

/**
 * Reports span as trace events (ie a detailed sampled trace) if it is sampled for tracing
 * @author pluk
 *
 */
public class TraceEventSpanReporter implements SpanReporter {
    private final Logger logger = LoggerFactory.getLogger();
    public static final TraceEventSpanReporter REPORTER = new TraceEventSpanReporter();
    private static final boolean PROFILER_ENABLED;
    
    static  {
        ProfilerSetting profilerSetting = (ProfilerSetting) ConfigManager.getConfig(ConfigProperty.PROFILER);
        PROFILER_ENABLED = profilerSetting != null && profilerSetting.isEnabled();
    }
    
    private TraceEventSpanReporter() {
    }
    
    /**
     * Reports entry event on span start if the span is traced with span tags as KVs in the entry event, then sets
     * the legacy TLS Context.metadata to the tracing metadata of this span
     * 
     * If this is the root span, then the "should trace" logic will be applied to determine whether this
     * span (and corresponding trace) should be traced. If the root span is traced and is a new trace, 
     * then extra KVs are added (SampleRate, BucketCapacity etc)
     */
    public void reportOnStart(Span span) {
//        ClassInstrumentation.startOrContinueTrace()
        
        Metadata spanMetadata = span.context().getMetadata();
        
        Event entryEvent = null;
        if (spanMetadata.isSampled()) {
            String entryXTrace = span.getSpanPropertyValue(SpanProperty.ENTRY_XID);
            
            if (entryXTrace != null) {
                try {
                    entryEvent = Context.createEventWithIDAndContext(entryXTrace, spanMetadata);
                } catch (OboeException e) {
                    logger.warn("Failed to set op ID for entry event : " + e.getMessage(), e);
                    entryEvent = Context.createEventWithContext(spanMetadata);
                }
            } else {
                boolean isEntryServiceRoot = span.isRoot() && span.getSpanPropertyValue(SpanProperty.IS_ENTRY_SERVICE_ROOT); //a span can be a root of a sub-tree of a distributed trace
                entryEvent = Context.createEventWithContext(spanMetadata, !isEntryServiceRoot); //do not add edge if this is the entry service root span
                
                if (isEntryServiceRoot) {
                    TraceDecision traceDecision = span.getSpanPropertyValue(SpanProperty.TRACE_DECISION);
                    TraceConfig config = traceDecision.getTraceConfig();
                    XTraceOptions xTraceOptions = span.getSpanPropertyValue(SpanProperty.X_TRACE_OPTIONS);

                    if (config != null) { //add trace decision KVs
                        TraceDecisionUtil.RequestType requestType = traceDecision.getRequestType();
                        entryEvent.addInfo("SampleRate", requestType.isTriggerTrace() ? -1 : config.getSampleRate(),
                                "SampleSource", requestType.isTriggerTrace() ? -1 : config.getSampleRateSourceValue(),
                                "BucketCapacity", config.getBucketCapacity(requestType.getBucketType()),
                                "BucketRate", config.getBucketRate(requestType.getBucketType()));

                        if (requestType.isTriggerTrace()) {
                            entryEvent.addInfo("TriggeredTrace", true);
                        }
                    }

                    if (xTraceOptions != null) {
                        Map<XTraceOption<String>, String> customKvs = xTraceOptions.getCustomKvs();
                        if (customKvs != null) {
                            for (Map.Entry<XTraceOption<String>, String> entry : customKvs.entrySet()) {
                                entryEvent.addInfo(entry.getKey().getKey(), entry.getValue());
                            }
                        }

                        String pdKeys = xTraceOptions.getOptionValue(XTraceOption.PD_KEYS);
                        if (pdKeys != null) {
                            entryEvent.addInfo("PDKeys", pdKeys);
                        }
                    }
                }
            }
        }
        
        if (entryEvent != null) {
            
            entryEvent.addInfo("Layer", span.getOperationName(),
                               "Label", "entry");

            Map<String, Object> tags = new HashMap<String, Object>(span.getTags());
            entryEvent.addInfo(tags); //add tags as KVs
                    
            entryEvent.setTimestamp(span.getStart());
        
            entryEvent.report(spanMetadata);

            span.setSpanPropertyValue(SpanProperty.REPORTED_TAG_KEYS, tags.keySet()); //to avoid double reporting on exit
        }
    }
        
    /**
     * Reports exit event on span finish if the span is traced with tags as KVs in the exit event,
     * then sets the legacy TLS Context.metadata to previous tracing metadata (usually the parent span), clear the TLS metadata if there's no previous tracing metadata 
     * 
     */
    public void reportOnFinish(Span span, long finishMicros) {
        Metadata spanMetadata = span.context().getMetadata();
        
        if (span.context().isSampled()) {
            String exitXTraceId = span.getSpanPropertyValue(SpanProperty.EXIT_XID);
            
            Event exitEvent;
            if (exitXTraceId != null) {
                try {
                    exitEvent = Context.createEventWithIDAndContext(exitXTraceId, spanMetadata);
                } catch (OboeException e) {
                    logger.warn("Failed to report span on finish with exitXTraceId [" + exitXTraceId + "], not using the exitXTraceId :" + e.getMessage());
                    exitEvent = Context.createEventWithContext(spanMetadata);
                }
            } else {
                exitEvent = Context.createEventWithContext(spanMetadata);
            }
            
            exitEvent.addInfo("Layer", span.getOperationName(),
                              "Label", "exit");
            
            exitEvent.setTimestamp(finishMicros);
            
            if (span.isRoot()) { //top span
                if (span.hasTraceProperty(TraceProperty.TRANSACTION_NAME)) {
                    exitEvent.addInfo("TransactionName", span.getTracePropertyValue(TraceProperty.TRANSACTION_NAME));
                }
                if (PROFILER_ENABLED) {
                    int profileSpanCount = span.getSpanPropertyValue(SpanProperty.PROFILE_SPAN_COUNT).get();
                    exitEvent.addInfo("ProfileSpans", profileSpanCount);
                } else {
                    exitEvent.addInfo("ProfileSpans", -1);
                }
            }

            if (span.getSpanPropertyValue(SpanProperty.IS_ASYNC)) {
                exitEvent.setAsync();
            }

            Map<String, Object> unreportedTags = new HashMap<String, Object>(span.getTags());
            Set<String> reportedTagKeys = span.getSpanPropertyValue(SpanProperty.REPORTED_TAG_KEYS);
            if (reportedTagKeys != null) {
                unreportedTags.keySet().removeAll(reportedTagKeys);
            }

            exitEvent.addInfo(unreportedTags); //add tags as KVs
            
            Set<String> childEdges = span.getSpanPropertyValue(SpanProperty.CHILD_EDGES);
            if (childEdges != null) {
                for (String childEdge : childEdges) { //report edges of child exits
                    exitEvent.addEdge(childEdge);
                }
            }
            
            exitEvent.report(spanMetadata);
            
            Span parentSpan = ScopeManager.INSTANCE.activeSpan(); //add the current exit edge to parent span
            if (parentSpan != null) {
                Set<String> edges = new HashSet<String>();
                Set<String> existingEdges = parentSpan.getSpanPropertyValue(SpanProperty.CHILD_EDGES);
                if (existingEdges != null) {
                    edges.addAll(existingEdges);
                }
                edges.add(spanMetadata.toHexString());
                parentSpan.setSpanPropertyValue(SpanProperty.CHILD_EDGES, edges);
            }
        }
    }

    /**
     * Reports info event base on the fields in the LogEntry provided
     */
    public void reportOnLog(Span span, LogEntry logEntry) {
        Metadata spanMetadata = span.context().getMetadata();
        
        if (spanMetadata.isSampled()) {
            Event event = Context.createEventWithContext(spanMetadata);
            event.addInfo("Layer", span.getOperationName());
            
            if (logEntry.isError()) {
                event.addInfo("Label", "error");
            } else {
                event.addInfo("Label", "info");
            }
            event.addInfo(logEntry.getFields());
            event.setTimestamp(logEntry.getTimestamp());
            event.report(spanMetadata);
        }
    }
}