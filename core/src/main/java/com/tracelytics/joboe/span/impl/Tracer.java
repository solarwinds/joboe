package com.tracelytics.joboe.span.impl;

import com.tracelytics.instrumentation.HeaderConstants;
import com.tracelytics.joboe.*;
import com.tracelytics.joboe.span.impl.Span.SpanProperty;
import com.tracelytics.joboe.span.impl.Span.TraceProperty;
import com.tracelytics.joboe.span.propagation.Extractor;
import com.tracelytics.joboe.span.propagation.Format;
import com.tracelytics.joboe.span.propagation.Injector;
import com.tracelytics.joboe.span.propagation.TextMap;
import com.tracelytics.joboe.span.tag.Tag;
import com.tracelytics.logging.Logger;
import com.tracelytics.logging.LoggerFactory;
import com.tracelytics.util.TimeUtils;

import java.util.*;
import java.util.Map.Entry;

/**
 * Implements OpenTracing's Tracer interface
 * @author pluk
 *
 */
public class Tracer implements com.tracelytics.joboe.span.Tracer {
    private final Map<Format<?>, Injector<?>> injectors = new HashMap<Format<?>, Injector<?>>();
    private final Map<Format<?>, Extractor<?>> extractors = new HashMap<Format<?>, Extractor<?>>();
    
//    public static final Tracer TRACING = new Tracer(TracingSpanReporter.REPORTER);
//    public static final Tracer TRACING_METRIC = new Tracer(TracingSpanReporter.REPORTER, MetricSpanReporter.REPORTER);
//    public static final Tracer METRIC = new Tracer(MetricSpanReporter.REPORTER);
//    public static final Tracer NO_OP = new Tracer();
    public static final Tracer INSTANCE = new Tracer();
    
    private static Random random = new Random(); //better to use ThreadLocalRandom, but it's only supported in 1.7+
    
    private static Logger logger = LoggerFactory.getLogger();
    
    private Tracer() {
        injectors.put(Format.Builtin.HTTP_HEADERS, new Injector<TextMap>() {
            public void inject(com.tracelytics.joboe.span.SpanContext spanContext, TextMap carrier) {
                if (spanContext instanceof SpanContext && ((SpanContext) spanContext).getMetadata().isValid()) {
                    carrier.put(HeaderConstants.W3C_TRACE_CONTEXT_HEADER, ((SpanContext) spanContext).getMetadata().toHexString());
                }
            }
        });
    }
    
    public SpanBuilder buildSpan(String operationName) {
        return new SpanBuilder(operationName);
    }

    public <C> void inject(com.tracelytics.joboe.span.SpanContext spanContext, Format<C> format, C carrier) {
        Injector<C> injector = (Injector<C>) injectors.get(format);
        if (injector == null) {
          throw new UnsupportedOperationException();
        }
        injector.inject((SpanContext) spanContext, carrier);
    }

    public <C> com.tracelytics.joboe.span.SpanContext extract(Format<C> format, C carrier) {
        Extractor<C> extractor = (Extractor<C>) extractors.get(format);
        if (extractor == null) {
          throw new UnsupportedOperationException();
        }
        return extractor.extract(carrier);
    }
    
    public class SpanBuilder implements com.tracelytics.joboe.span.Tracer.SpanBuilder {
        private SpanContext explicitParentContext;
        private Map<String, Object> tags = new HashMap<String, Object>();
        private Map<SpanProperty<?>, Object> properties = new HashMap<SpanProperty<?>, Object>();
        private Long startTimestamp;
        private String operationName;
        private List<SpanReporter> reporters = new ArrayList<SpanReporter>();        
        
        private byte flags;
        private boolean ignoreActiveSpan;
        
        public SpanBuilder(String operationName) {
            this.operationName = operationName;
        }
        
        @Override
        public Span start() {
            Span newSpan;
            
            long startTimestamp;
            if (this.startTimestamp == null) {
                startTimestamp = TimeUtils.getTimestampMicroSeconds();
            } else {
                startTimestamp = this.startTimestamp;
            }
            
            long spanId = random.nextLong();

            SpanContext parentContext = getParentContext();

            Long newTraceId = null;
            
            if (parentContext != null) {
                Metadata parentMetadata;
                if (!parentContext.getMetadata().isValid() && parentContext.getEntryMetadata() != null) { //parent metadata is invalided, span probably comes in not in chronological ordering
                    parentMetadata = parentContext.getEntryMetadata();
                } else {
                    parentMetadata = parentContext.getMetadata();
                }

                Metadata newMetadata = new Metadata(parentMetadata); //create metadata clone from parent span - so it appears as a branch
                Scope existingScope = ScopeManager.INSTANCE.active();

                newSpan = new Span(Tracer.this,
                                   operationName, 
                                   new SpanContext(newMetadata, 
                                                   parentContext.getTraceId(),
                                                   spanId,
                                                   parentContext.getSpanId(),
                                                   (byte)(parentContext.getFlags() | this.flags),
                                                   parentContext.getBaggage(),
                                                   parentContext.getMetadata()),
                                   startTimestamp, 
                                   tags,
                                   reporters);

                //if the existing scope was created/activated by a span originates from a different thread
                //, then this child span should be flagged as async
                if (existingScope != null && existingScope.isAsyncPropagation()) {
                    newSpan.setSpanPropertyValue(SpanProperty.IS_ASYNC, true);
                }
            } else if (Context.isValid() || properties.containsKey(SpanProperty.ENTRY_SPAN_METADATA)) { //existing context NOT as parent context
                Metadata spanMetadata;
                if (properties.containsKey(SpanProperty.ENTRY_SPAN_METADATA)) { //explicitly stated entry x-trace id
                    spanMetadata = (Metadata) properties.get(SpanProperty.ENTRY_SPAN_METADATA);
                } else { //traces started by legacy "event based" way or only legacy context is propagated to this thread
                    spanMetadata = new Metadata(Context.getMetadata()); //create metadata clone so it appears as a branch
                }

                if (spanMetadata.getTraceId() == null) { //a trace has already been started and this propagated context should not trigger a new trace ID
                    newTraceId = random.nextLong();
                    spanMetadata.setTraceId(newTraceId);
                } else {
                    newTraceId = spanMetadata.getTraceId();
                }
                
                newSpan = new Span(Tracer.this, 
                                   operationName, 
                                   new SpanContext(spanMetadata, 
                                                   newTraceId,
                                                   spanId,
                                                   null,
                                                   this.flags,
                                                   new HashMap<String, String>(),
                                                   Context.getMetadata()),
                                   startTimestamp, 
                                   tags,
                                   reporters);
            } else { //then it's a root span and no other existing context
                TraceDecisionParameters samplingParameters = (TraceDecisionParameters) properties.get(SpanProperty.TRACE_DECISION_PARAMETERS);
                Metadata spanMetadata;
                String xtrace = null;
                XTraceOptions xTraceOptions = null;
                TraceDecision traceDecision = null;

                if (samplingParameters != null) { //expected trace entry point, get tracing decision to see whether we want to start tracing/metrics reporting
                    Map<XTraceHeader, String> xTraceHeaders = samplingParameters.getHeaders();
                    xtrace = xTraceHeaders.get(XTraceHeader.TRACE_ID);

                    if (xtrace != null && !Metadata.isCompatible(xtrace)) { //ignore x-trace id if it's not compatible
                        logger.debug("Not accepting X-Trace ID [" + xtrace + "] for trace continuation");
                        xtrace = null;
                    }

                    //extract X-Trace-Options
                    xTraceOptions = XTraceOptions.getXTraceOptions(xTraceHeaders.get(XTraceHeader.TRACE_OPTIONS), xTraceHeaders.get(XTraceHeader.TRACE_OPTIONS_SIGNATURE));


                    if (properties.containsKey(SpanProperty.TRACE_DECISION)) { //Sampling decision is already made
                        //TODO extract http headers still?
                        traceDecision = (TraceDecision) properties.get(SpanProperty.TRACE_DECISION);
                    } else if (samplingParameters != null ){
                        traceDecision = TraceDecisionUtil.shouldTraceRequest(operationName, xtrace, xTraceOptions,
                                Collections.singletonList(samplingParameters.getResource()));
                        withSpanProperty(SpanProperty.TRACE_DECISION, traceDecision);
                    } else { //not an expected trace entry point, and there's no parent span/valid context, hence it's an no-op, we should still create a span though
                        traceDecision = null;
                    }
                    
                    if (traceDecision != null) {
                        spanMetadata = createTraceContext(traceDecision); //set the trace context based on the trace decision
                    } else {
                        spanMetadata = new Metadata(); //just create an empty context
                    }

                    if (xtrace == null) {
                        withSpanProperty(SpanProperty.IS_ENTRY_SERVICE_ROOT, true);
                    }
                    withSpanProperty(SpanProperty.TRACE_DECISION, traceDecision);
                    if (xTraceOptions != null) {
                        withSpanProperty(SpanProperty.X_TRACE_OPTIONS, xTraceOptions);
                    }
                } else { //not an expected trace entry point, and there's no parent span/valid context, hence it's an no-op, we should still create a span though
                    spanMetadata = new Metadata(); //just create an empty context
                }
                
                

                newTraceId = random.nextLong();
                spanMetadata.setTraceId(newTraceId);
                
                SpanContext newContext = new SpanContext(spanMetadata, newTraceId, spanId, null, flags, new HashMap<String, String>(), null);
                newSpan = new Span(Tracer.this, operationName, newContext, startTimestamp, tags, reporters);
            }
            
            for (Entry<SpanProperty<?>, Object> propertyEntry : properties.entrySet()) {
                newSpan.setSpanPropertyValue((SpanProperty) propertyEntry.getKey(), propertyEntry.getValue());
            }

            newSpan.start();

//            newSpan.setSpanPropertyValue(SpanProperty.ENTRY_XID, newSpan.context().getMetadata().toHexString()); //tag it as the metatdata will get updated/invalidated later on
            
            return newSpan;
        }
        
        private Metadata createTraceContext(TraceDecision decision) {
            Metadata newMetadata;
            Metadata incomingMetadata = decision.getIncomingMetadata();
            if (decision.isSampled()) {
                if (incomingMetadata != null) {
                    logger.debug("Continuing trace: " + incomingMetadata.toHexString());
                    newMetadata = new Metadata(incomingMetadata);
                } else {
                    logger.debug("Starting new trace");
                    newMetadata = new Metadata();
                    //randomize the context metadata provided
                    newMetadata.randomize(true);
                }
            } else { //do not trace
                if (incomingMetadata != null) {
                    logger.debug("Propagation but not continuing trace: " + incomingMetadata.toHexString());
                    newMetadata = new Metadata(incomingMetadata);
                    newMetadata.setSampled(false); //make it not sampled
                } else {
                    newMetadata = new Metadata();
                    newMetadata.randomize(false); //create a new x-trace ID but not sampled
                }
            }
            
            newMetadata.setReportMetrics(decision.isReportMetrics());
            
            return newMetadata;
        }
        
        public SpanBuilder asChildOf(com.tracelytics.joboe.span.SpanContext parentContext) {
            this.explicitParentContext = (SpanContext) parentContext; 
            return this;
        }

        public SpanBuilder asChildOf(com.tracelytics.joboe.span.Span parentSpan) {
            return asChildOf(parentSpan.context());
        }

        public SpanBuilder addReference(String referenceType, com.tracelytics.joboe.span.SpanContext referencedContext) {
            throw new UnsupportedOperationException();
        }

        public SpanBuilder withTag(String key, Object value) {
            tags.put(key, value);
            return this;
        }
        
        public SpanBuilder withTag(String key, String value) {
            return withTag(key, (Object)value);
        }
        
        public SpanBuilder withTag(String key, boolean value) {
            return withTag(key, (Object)value);
        }
        
        public SpanBuilder withTag(String key, Number value) {
            return withTag(key, (Object)value);
        }
        
        public SpanBuilder withTags(Map<String, ?> tags) {
            this.tags.putAll(tags);
            return this;
        }
        
        @Override
        public <T> SpanBuilder withTag(Tag<T> tag, T value) {
            return withTag(tag.getKey(), value);
        }
        
        public SpanBuilder withStartTimestamp(long startTimestamp) {
            this.startTimestamp = startTimestamp;
            return this;
        }
        
        public SpanBuilder withFlags(byte flags) {
            this.flags = flags;
            return this;
        }
        
        public <V> SpanBuilder withSpanProperty(SpanProperty<V> property, V value) {
            this.properties.put(property, value);
            return this;
        }
        
        public SpanBuilder withReporters(SpanReporter...reporters) {
            this.reporters.addAll(Arrays.asList(reporters));
            return this;
        }
        
        public SpanBuilder removeReporter(SpanReporter reporter) {
            this.reporters.remove(reporter);
            return this;
        }

        /**
         * Not standard OT usage, convenient method to start a Scope which finishes the wrapped span on Scope close
         * @return
         */
        public Scope startActive() {
            return startActive(true);
        }
        
        @Override
        public Scope startActive(boolean finishSpanOnClose) {
            Span span = start();
        
            return scopeManager().activate(span, finishSpanOnClose);
        }
        
        @Deprecated
        @Override
        public Span startManual() {
            return start();
        }

        public SpanBuilder ignoreActiveSpan() {
            ignoreActiveSpan = true;
            return this;
        }
        
        /**
         * Gets the parent context, whether it's explicitly assigned by asChildOf calls or inferred reference as described in 
         * {@link com.tracelytics.joboe.span.Tracer.SpanBuilder#addReference(String, SpanContext)}
         * @return
         */
        public SpanContext getParentContext() {
            if (explicitParentContext != null) {
                return explicitParentContext;
            } else if (!ignoreActiveSpan) {
                Span activeSpan = ScopeManager.INSTANCE.activeSpan();
                if (activeSpan != null) {
                    return activeSpan.context();
                }
            }
            
            return null;
        }
    }

    @Override
    public ScopeManager scopeManager() {
        return ScopeManager.INSTANCE;
    }

    @Override
    public Span activeSpan() {
        return ScopeManager.INSTANCE.activeSpan();
    }

    @Override
    public Scope activateSpan(com.tracelytics.joboe.span.Span span) {
        return scopeManager().activate(span);
    }
    
    /**
     * Sets the `TraceProperty` when there's no available `Scope` object
     * 
     * The code would look up the current active `Metadata` (which is more likely be available), look up 
     * and set the corresponding `TraceProperty` with the provided value
     *  
     * @param traceProperty
     * @param value
     * @return  true if a there's a valid `Metadata` and that the property was set successfully
     */
    public static <T> boolean setTraceProperty(TraceProperty<T> traceProperty, T value) {
        //ideally getCurrentSpan().setTracePropertyValue(traceProperty, value) 
        //but span might not be available in async context
        Metadata metadata = Context.getMetadata();
        if (metadata.isValid()) {
            Long traceId = metadata.getTraceId();
            if (traceId != null) {
                Map<TraceProperty<?>, Object> traceProperties = TracePropertyDictionary.getTracePropertiesByTraceId(traceId);
                if (traceProperties != null) {
                    traceProperties.put(traceProperty, value);
                    return true;
                }
            }
        }
        
        return false;
    }
}



 
