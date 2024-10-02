package com.tracelytics.joboe;

/**
 * Known X-Trace headers used by our Agent. Take note that these headers are not tied to any protocol (Http for instance). It is the 
 * caller who should map the input headers to the corresponding X-Trace headers of this enum 
 * @author Patson Luk
 *
 */
public enum XTraceHeader {
    TRACE_ID, SPAN_ID, TRACE_OPTIONS, TRACE_OPTIONS_SIGNATURE
}
