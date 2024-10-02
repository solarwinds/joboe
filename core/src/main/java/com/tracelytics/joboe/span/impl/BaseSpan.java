package com.tracelytics.joboe.span.impl;

/**
 * @deprecated  Use {@link Span} instead
 * @author pluk
 *
 */
public interface BaseSpan {
    
   
    /**
     * @deprecated use {@link Span.SpanProperty instead}
     * @param <V>
     */
    public static class SpanProperty<V> {
        public static final SpanProperty<String> EXIT_XID = new SpanProperty<String>(com.tracelytics.joboe.span.impl.Span.SpanProperty.EXIT_XID); 
        public static final SpanProperty<Boolean> IS_PROFILE = new SpanProperty<Boolean>(com.tracelytics.joboe.span.impl.Span.SpanProperty.IS_PROFILE);
        public static final SpanProperty<Boolean> IS_ASYNC = new SpanProperty<Boolean>(com.tracelytics.joboe.span.impl.Span.SpanProperty.IS_ASYNC);
        public static final SpanProperty<Boolean> IS_SDK = new SpanProperty<Boolean>(com.tracelytics.joboe.span.impl.Span.SpanProperty.IS_SDK);
        private final com.tracelytics.joboe.span.impl.Span.SpanProperty<V> wrapped;
        
        public SpanProperty(com.tracelytics.joboe.span.impl.Span.SpanProperty<V> wrapped) {
            this.wrapped = wrapped;
        }
        
        public com.tracelytics.joboe.span.impl.Span.SpanProperty<V> getWrapped() {
            return wrapped;
        }
    }
    
    /**
     * @deprecated use {@link Span.TraceProperty instead}
     * @author pluk
     *
     * @param <V>
     */
    public static class TraceProperty<V> {
        public static final TraceProperty<String> CUSTOM_TRANSACTION_NAME = new TraceProperty<String>(com.tracelytics.joboe.span.impl.Span.TraceProperty.CUSTOM_TRANSACTION_NAME);
        private final com.tracelytics.joboe.span.impl.Span.TraceProperty<V> wrapped; 
        
        public TraceProperty(com.tracelytics.joboe.span.impl.Span.TraceProperty<V> wrapped) {
            this.wrapped = wrapped;
        }
        
        public com.tracelytics.joboe.span.impl.Span.TraceProperty<V> getWrapped() {
            return wrapped;
        }
    }
}
