package com.solarwinds.joboe.span.impl;

/**
 * Reporter to be passed into the ctor of {@link com.solarwinds.joboe.span.impl.Tracer} to react to {@link Span}'s start and finish
 * @author pluk
 *
 */
public interface SpanReporter {
    void reportOnStart(Span span);
    void reportOnLog(Span span, LogEntry logEntry);
    void reportOnFinish(Span span, long finishMicroSec);
}
