package com.solarwinds.joboe.span.impl;

import com.solarwinds.joboe.Metadata;
import com.solarwinds.joboe.TraceDecision;
import com.solarwinds.joboe.XTraceOptions;
import com.solarwinds.joboe.span.tag.Tag;
import com.solarwinds.logging.Logger;
import com.solarwinds.logging.LoggerFactory;
import com.solarwinds.util.TimeUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class Span implements com.solarwinds.joboe.span.Span {
    private static final Logger logger = LoggerFactory.getLogger();
     
    private final long startTimeMicroseconds;
    private String operationName;
    private SpanContext context;
    private final Map<String, Object> tags = new HashMap<String, Object>();
    private final Map<SpanProperty<?>, Object> spanProperties = new HashMap<SpanProperty<?>, Object>();
    private final Map<TraceProperty<?>, Object> traceProperties;
    private final List<SpanReporter> reporters;
            
    /**
     * 
     * @param tracer    Tracer tied to this span, take note that the tracer instance determined what SpanReporter would be used to report the span
     * @param operationName
     * @param context
     * @param startTimeMicroseconds
     * @param tags
     */
    public Span(Tracer tracer, String operationName, SpanContext context, long startTimeMicroseconds, Map<String, Object> tags, List<SpanReporter> reporters) {
        this.operationName = operationName;
        this.context = context;
        this.startTimeMicroseconds = startTimeMicroseconds;
        if (tags != null) {
            this.tags.putAll(tags);
        }
        this.reporters = new ArrayList<SpanReporter>(reporters);
        
        long traceId = context.getTraceId();
        
        traceProperties = TracePropertyDictionary.getTracePropertiesByTraceId(traceId);
    }

    public long getStart() {
        return startTimeMicroseconds;
    }


    public Map<String, Object> getTags() {
        synchronized (tags) {
            return Collections.unmodifiableMap(tags);
        }
    }
    
    public Object getTag(String tagName) {
        synchronized (tags) {
            return tags.get(tagName);
        }
    }

    public Span setOperationName(String operationName) {
        this.operationName = operationName;
        return this;
    }

    public String getOperationName() {
        return operationName;
    }
    
    public Span setBaggageItem(String key, String value) {
        synchronized(this) {
            this.context = this.context.withBaggageItem(key, value);
            return this;
        }
    }

    public String getBaggageItem(String key) {
        synchronized(this) {
            return this.context.getBaggageItem(key);
        }
    }

    @Override
    public String toString() {
        return context.contextAsString() + " - " + operationName;
    }

    public SpanContext context() {
        // doesn't need to be a copy since all fields are final
        return context;
    }
    
    public void start() {
        for (SpanReporter reporter : reporters) {
            reporter.reportOnStart(this);
        }
    }

    public void finish() {
        finish(TimeUtils.getTimestampMicroSeconds());
    }

    public void finish(long finishMicros) {
        if (isRoot()) {
            //set TRANSACTION_NAME, this is a bit specific but is probably ok for now as Span is our own specific impl (vs ISpan)
            //a more appropriate place would be a centralized trace exit point which does not exist right now
            setTracePropertyValue(TraceProperty.TRANSACTION_NAME, TransactionNameManager.getTransactionName(this));
        }
        
        for (SpanReporter reporter : reporters) {
            reporter.reportOnFinish(this, finishMicros);
        }
        
        Metadata spanMetadata = context().getMetadata();
        
        if (spanMetadata.isValid()) {
            //save the exit x-trace id
            setSpanPropertyValue(SpanProperty.EXIT_XID, spanMetadata.toHexString());
            
            //invalidate this metadata as this span finishes
            spanMetadata.invalidate(); 
        }

        
        if (isRoot()) {
            TracePropertyDictionary.removeTracePropertiesByTraceId(context.getTraceId()); //clean up the TraceProperties as this is the end of the trace
        }
    }

    public Span setTag(String key, String value) {
      return setTagAsObject(key, value);
    }

    public Span setTag(String key, boolean value) {
      return setTagAsObject(key, value);
    }

    public Span setTag(String key, Number value) {
      return setTagAsObject(key, value);
    }
    
    public Span setTagAsObject(String key, Object value) {
      synchronized (tags) {
          tags.put(key, value);
      }

      return this;
    }
    
    @Override
    public <T> Span setTag(Tag<T> tag, T value) {
        return setTagAsObject(tag.getKey(), value);
    }
    
    /**
     * Sets a value with key {@link SpanProperty} to this current span, useful for more specific properties that are not fitting as tags
     * @param property
     * @param value
     */
    public <V> void setSpanPropertyValue(SpanProperty<V> property, V value) {
        synchronized (spanProperties) {
            spanProperties.put(property, value);
        }
    }
    
    /**
     * Gets value associated with the provided {@link SpanProperty} key
     * @param property
     * @return  the default value of the SpanProperty if unset, could be null
     */
    public <V> V getSpanPropertyValue(SpanProperty<V> property) {
        synchronized (spanProperties) {
            if (!spanProperties.containsKey(property)) {
                spanProperties.put(property, property.getDefaultValue());
            }
            
            return (V) spanProperties.get(property);
        }
        
        
    }
    
    public boolean hasSpanProperty(SpanProperty<?> property) {
        synchronized (spanProperties) {
            return spanProperties.containsKey(property);
        }
    }

    /**
     * Sets a value with key {@link TraceProperty} to the trace this span belongs to, this is visible to all spans belong to that trace
     * @param property
     * @param value
     */
    public <V> void setTracePropertyValue(TraceProperty<V> property, V value) {
        synchronized (traceProperties) {
            traceProperties.put(property, value);
        }
    }
    
    /**
     * Gets value associated with the provided {@link TraceProperty}. This value is shared by all spans within the same trace 
     * @param property
     * @return
     */
    public <V> V getTracePropertyValue(TraceProperty<V> property) {
        synchronized (traceProperties) {
            if (!traceProperties.containsKey(property)) {
                traceProperties.put(property, property.getDefaultValue());
            }
            
            return (V) traceProperties.get(property);
        }
    }    
    
    public boolean hasTraceProperty(TraceProperty<?> property) {
        synchronized (traceProperties) {
            return traceProperties.containsKey(property);
        }
    }

    /**
     * Log an error event
     * @param fields
     * @return
     */
    public Span error(Map<String, ?> fields) {
        long time = TimeUtils.getTimestampMicroSeconds();
        return log(time, new LogEntry(time, fields, true));
        
    }
    
    private Span log(long timestampMicroseconds, LogEntry logEntry) {
        for (SpanReporter reporter : reporters) {
            reporter.reportOnLog(this, logEntry);
        }
        
        return this;
    }
    
    public Span log(long timestampMicroseconds, Map<String, ?> fields) {
        log(timestampMicroseconds, new LogEntry(timestampMicroseconds, fields));
        
        return this;
    }
    
    public Span log(Map<String, ?> fields) {
        return log(TimeUtils.getTimestampMicroSeconds(), fields);
    }

    public Span log(String event) {
        return log(Collections.singletonMap("event", event));
    }

    public Span log(long timestampMicroseconds, String event) {
        return log(timestampMicroseconds, Collections.singletonMap("event", event));
    }

    public Span log(String eventName, Object payload) {
        return log(TimeUtils.getTimestampMicroSeconds(), eventName, payload);
    }
    
    /**
     * Extra convenient method
     * @param dataPair
     * @return
     */
    public Span log(Object...dataPair) {
        if (dataPair.length % 2 != 0) {
            logger.warn("log to current span but object array size is not even");
            return this;
        }
        Map<String, Object> dataMap = new HashMap<String, Object>();
        int i = 0;
        while (i < dataPair.length) {
            Object key = dataPair[i ++];
            if (!(key instanceof String)) {
                logger.warn("log to current span but object with index [" + i + "] is not a string");
                return this;
            }
            dataMap.put((String) key, dataPair[i ++]); 
        }
        
        return log(dataMap);
    }

    public Span log(long timestampMicroseconds, String eventName, Object payload) {
        Map<String, Object> fields = new HashMap<String, Object>();
        fields.put("event", eventName);
        fields.put("payload", payload);
        
        return log(timestampMicroseconds, fields);
    }
    
    public boolean isRoot() {
        return context.getParentId() == null && context.getPreviousMetadata() == null; //consider as root only if there's no existing context from legacy instrumentation when this span was created
    }
    
    public void addSpanReporter(SpanReporter reporter) {
        reporters.add(reporter);
    }
    
    public boolean removeSpanRepoter(SpanReporter reporter) {
        return reporters.remove(reporter);
    }
    
    public static class Property<V> {
    }
    
    /**
     * Special typed span property (with default value) that does not fit well with "tag", "log" nor "baggage" 
     * @author pluk
     *
     * @param <V>
     */
    public static class SpanProperty<V> {
        public static final SpanProperty<String> EXIT_XID = new SpanProperty<String>(); 
        public static final SpanProperty<String> ENTRY_XID = new SpanProperty<String>();
//        public static final SpanProperty<Map<String, String>> METRIC_TAGS = new SpanProperty<Map<String, String>>() {
//            @Override
//            public Map<String, String> getDefaultValue() {
//                return new ConcurrentHashMap<String, String>();
//            }
//        };
        public static final SpanProperty<Boolean> IS_PROFILE = new SpanProperty<Boolean>(false);
        public static final SpanProperty<Boolean> IS_ASYNC = new SpanProperty<Boolean>(false);
        public static final SpanProperty<AtomicInteger> PROFILE_SPAN_COUNT = new SpanProperty<AtomicInteger>(new Producer<AtomicInteger>() { 
            public AtomicInteger produce() { 
                return new AtomicInteger(0); 
            }});
        
        public static final SpanProperty<Set<String>> CHILD_EDGES = new SpanProperty<Set<String>>();
        public static final SpanProperty<Set<String>> REPORTED_TAG_KEYS = new SpanProperty<Set<String>>();
        public static final SpanProperty<TraceDecisionParameters> TRACE_DECISION_PARAMETERS = new SpanProperty<TraceDecisionParameters>();
        public static final SpanProperty<Boolean> IS_SDK = new SpanProperty<Boolean>(false); //whether the span was created by SDK call 
        public static final SpanProperty<Long> THREAD_ID = new SpanProperty<Long>();
        public static final SpanProperty<Boolean> IS_ENTRY_SERVICE_ROOT = new SpanProperty<Boolean>(false);
        public static final SpanProperty<TraceDecision> TRACE_DECISION = new SpanProperty<TraceDecision>((TraceDecision) null);
        public static final SpanProperty<XTraceOptions> X_TRACE_OPTIONS = new SpanProperty<XTraceOptions>();


        private V defaultValue;
        private Producer<V> defaultValueProducer;
        
        /**
         * Immutable default value 
         */
        private SpanProperty() {
            this((V) null);
        }
        
        private SpanProperty(V defaultValue) {
            this.defaultValue = defaultValue;
        }
        
        /**
         * Creates default value for mutable object such as AtomicInteger or Collections
         * @param defaultValueProducer
         */
        private SpanProperty(Producer<V> defaultValueProducer) {
            this.defaultValueProducer = defaultValueProducer;
        }
        
        public V getDefaultValue() {
            return defaultValue != null ? defaultValue : (defaultValueProducer != null ? defaultValueProducer.produce() : null);
        }
        
        private interface Producer<V> {
            V produce();
        }
    }
    
    /**
     * Special typed trace property (with default value) that does not fit well with "tag", "log" nor "baggage" 
     * @author pluk
     *
     * @param <V>
     */
    public static class TraceProperty<V> extends Property<V> {
        public static final TraceProperty<String> ACTION = new TraceProperty<String>(null); 
        public static final TraceProperty<String> CONTROLLER = new TraceProperty<String>(null);
        public static final TraceProperty<Boolean> HAS_ERROR = new TraceProperty<Boolean>(false);
        public static final TraceProperty<String> TRANSACTION_NAME = new TraceProperty<String>(null);
        public static final TraceProperty<String> CUSTOM_TRANSACTION_NAME = new TraceProperty<String>(null);
        public static final TraceProperty<Map<Long, String>> PROFILE_IDS = new TraceProperty<Map<Long, String>>(new ConcurrentHashMap<Long, String>());
        
        private V defaultValue;
        
        private TraceProperty() {
            this(null);
        }
        
        private TraceProperty(V defaultValue) {
            this.defaultValue = defaultValue;
        }
        
        public V getDefaultValue() {
            return defaultValue;
        }
    }
}
