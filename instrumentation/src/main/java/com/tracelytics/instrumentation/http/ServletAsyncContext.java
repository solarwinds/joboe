package com.tracelytics.instrumentation.http;

import com.tracelytics.joboe.span.impl.Span;

import java.util.List;

/**
 * Tags the {@link javax.servlet.AsyncContext}, adds extra method to flag whether the <code>AsyncContext</code> is active.
 * 
 * An AsyncContext is considered active after <code>javax.servlet.ServletRequest#startAsync()</code> and before <code>javax.servlet.AsyncContext#complete()</code> 
 * 
 * @author pluk
 *
 */
public interface ServletAsyncContext {
    void tvSetSpanStack(List<Span> spanStack);
    List<Span> tvGetSpanStack();

}
