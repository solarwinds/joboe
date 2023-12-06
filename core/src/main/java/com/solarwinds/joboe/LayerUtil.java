package com.solarwinds.joboe;

import com.solarwinds.logging.Logger;
import com.solarwinds.logging.LoggerFactory;

import java.util.Map;

/**
 * @author pluk
 * @deprecated Use {@link TraceDecisionUtil instead}. Keeping this class for backward compatibility with TraceView API usage
 */
public class LayerUtil {
    private static final Logger logger = LoggerFactory.getLogger();

    /**
     * Determines if we should trace this request
     * Returns TraceConfig if so, otherwise null.
     *
     * @param layer
     * @param headers extra headers to determine whether the request should be traced
     * @return sample decision, should always be non null
     * @see XTraceHeader
     */
    public static SampleRateConfig shouldTraceRequest(String layer, Map<XTraceHeader, String> headers) {
        String xtrace = headers.get(XTraceHeader.TRACE_ID);
        logger.debug("Not accepting X-Trace ID [" + xtrace + "] for trace continuation");
        if (xtrace != null && !Metadata.isCompatible(xtrace)) { //ignore x-trace id if it's not compatible
            xtrace = null;
        }
        XTraceOptions xTraceOptions = XTraceOptions.getXTraceOptions(headers.get(XTraceHeader.TRACE_OPTIONS), headers.get(XTraceHeader.TRACE_OPTIONS_SIGNATURE));
        TraceDecision traceDecision = TraceDecisionUtil.shouldTraceRequest(layer, xtrace, xTraceOptions, null);

        return traceDecision.isSampled() ? new SampleRateConfig(traceDecision.getTraceConfig()) : null;
    }
}
