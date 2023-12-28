package com.solarwinds.joboe.core.span.impl;

import lombok.Getter;

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
    @Getter
    class SpanProperty<V> {
        public static final SpanProperty<String> EXIT_XID = new SpanProperty<String>(com.solarwinds.joboe.core.span.impl.Span.SpanProperty.EXIT_XID);
        public static final SpanProperty<Boolean> IS_PROFILE = new SpanProperty<Boolean>(com.solarwinds.joboe.core.span.impl.Span.SpanProperty.IS_PROFILE);
        public static final SpanProperty<Boolean> IS_ASYNC = new SpanProperty<Boolean>(com.solarwinds.joboe.core.span.impl.Span.SpanProperty.IS_ASYNC);
        public static final SpanProperty<Boolean> IS_SDK = new SpanProperty<Boolean>(com.solarwinds.joboe.core.span.impl.Span.SpanProperty.IS_SDK);
        private final com.solarwinds.joboe.core.span.impl.Span.SpanProperty<V> wrapped;
        
        public SpanProperty(com.solarwinds.joboe.core.span.impl.Span.SpanProperty<V> wrapped) {
            this.wrapped = wrapped;
        }

    }
    
    /**
     * @deprecated use {@link Span.TraceProperty instead}
     * @author pluk
     *
     * @param <V>
     */
    @Getter
    class TraceProperty<V> {
        public static final TraceProperty<String> CUSTOM_TRANSACTION_NAME = new TraceProperty<String>(com.solarwinds.joboe.core.span.impl.Span.TraceProperty.CUSTOM_TRANSACTION_NAME);
        private final com.solarwinds.joboe.core.span.impl.Span.TraceProperty<V> wrapped;
        
        public TraceProperty(com.solarwinds.joboe.core.span.impl.Span.TraceProperty<V> wrapped) {
            this.wrapped = wrapped;
        }

    }
}
