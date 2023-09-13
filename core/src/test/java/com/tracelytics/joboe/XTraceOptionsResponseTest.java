package com.tracelytics.joboe;

import com.tracelytics.joboe.rpc.Settings;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class XTraceOptionsResponseTest {
    @Test
    public void testResponse() {
        TraceConfig traceConfig = new TraceConfig(TraceDecisionUtil.SAMPLE_RESOLUTION, SampleRateSource.OBOE_DEFAULT, TracingMode.ALWAYS.toFlags());
        XTraceOptions options;

        XTraceOptionsResponse response;
        //no x-trace options
        response = XTraceOptionsResponse.computeResponse(XTraceOptions.getXTraceOptions(null, null), new TraceDecision(true, true, traceConfig, TraceDecisionUtil.RequestType.REGULAR), true);
        assertNull(response);

        //empty x-trace options
        response = XTraceOptionsResponse.computeResponse(XTraceOptions.getXTraceOptions("", null), new TraceDecision(true, true, traceConfig, TraceDecisionUtil.RequestType.REGULAR), true);
        assertEquals("trigger-trace=not-requested", response.toString());

        //trigger trace (unauthenticated)
        options = new XTraceOptions(Collections.singletonMap(XTraceOption.TRIGGER_TRACE, true), Collections.EMPTY_LIST, XTraceOptions.AuthenticationStatus.NOT_AUTHENTICATED);
        response = XTraceOptionsResponse.computeResponse(options, new TraceDecision(true, true, traceConfig, TraceDecisionUtil.RequestType.UNAUTHENTICATED_TRIGGER_TRACE), true);
        assertEquals("trigger-trace=ok", response.toString());

        //trigger trace (authenticated)
        options = new XTraceOptions(Collections.singletonMap(XTraceOption.TRIGGER_TRACE, true), Collections.EMPTY_LIST, XTraceOptions.AuthenticationStatus.OK);
        response = XTraceOptionsResponse.computeResponse(options, new TraceDecision(true, true, traceConfig, TraceDecisionUtil.RequestType.AUTHENTICATED_TRIGGER_TRACE), true);
        assertEquals("auth=ok;trigger-trace=ok", response.toString());

        //trigger trace no remote settings
        options = new XTraceOptions(Collections.singletonMap(XTraceOption.TRIGGER_TRACE, true), Collections.EMPTY_LIST, XTraceOptions.AuthenticationStatus.NOT_AUTHENTICATED);
        response = XTraceOptionsResponse.computeResponse(options, new TraceDecision(false, false, null, TraceDecisionUtil.RequestType.UNAUTHENTICATED_TRIGGER_TRACE), true);
        assertEquals("trigger-trace=settings-not-available", response.toString());

        //trigger trace bucket exhausted
        options = new XTraceOptions(Collections.singletonMap(XTraceOption.TRIGGER_TRACE, true), Collections.EMPTY_LIST, XTraceOptions.AuthenticationStatus.NOT_AUTHENTICATED);
        response = XTraceOptionsResponse.computeResponse(options, new TraceDecision(false, false, true, traceConfig, TraceDecisionUtil.RequestType.UNAUTHENTICATED_TRIGGER_TRACE), true);
        assertEquals("trigger-trace=rate-exceeded", response.toString());

        //trigger trace trace mode = disabled
        options = new XTraceOptions(Collections.singletonMap(XTraceOption.TRIGGER_TRACE, true), Collections.EMPTY_LIST, XTraceOptions.AuthenticationStatus.NOT_AUTHENTICATED);
        TraceConfig tracingDisabledConfig = new TraceConfig(0, SampleRateSource.FILE, TracingMode.NEVER.toFlags());
        response = XTraceOptionsResponse.computeResponse(options, new TraceDecision(false, false, tracingDisabledConfig, TraceDecisionUtil.RequestType.UNAUTHENTICATED_TRIGGER_TRACE), true);
        assertEquals("trigger-trace=tracing-disabled", response.toString());

        //trigger trace feature is disabled
        options = new XTraceOptions(Collections.singletonMap(XTraceOption.TRIGGER_TRACE, true), Collections.EMPTY_LIST, XTraceOptions.AuthenticationStatus.NOT_AUTHENTICATED);
        TraceConfig featureDisabledConfig = new TraceConfig(TraceDecisionUtil.SAMPLE_RESOLUTION, SampleRateSource.FILE, (short) (TracingMode.ENABLED.toFlags() & ~Settings.OBOE_SETTINGS_FLAG_TRIGGER_TRACE_ENABLED));
        response = XTraceOptionsResponse.computeResponse(options, new TraceDecision(false, true, featureDisabledConfig, TraceDecisionUtil.RequestType.UNAUTHENTICATED_TRIGGER_TRACE), true);
        assertEquals("trigger-trace=trigger-tracing-disabled", response.toString());
    }

    @Test
    public void testExceptionResponse() {
        TraceConfig traceConfig = new TraceConfig(TraceDecisionUtil.SAMPLE_RESOLUTION, SampleRateSource.OBOE_DEFAULT, TracingMode.ALWAYS.toFlags());

        XTraceOptionsResponse response;
        //unknown X-Trace-Options
        response = XTraceOptionsResponse.computeResponse(XTraceOptions.getXTraceOptions("unknown1=1;unknown2;" + XTraceOption.SW_KEYS.getKey() + "=3", null), new TraceDecision(true, true, traceConfig, TraceDecisionUtil.RequestType.REGULAR), true);
        assertEquals("trigger-trace=not-requested;ignored=unknown1,unknown2", response.toString());

        //invalid trigger-trace (has value)
        response = XTraceOptionsResponse.computeResponse(XTraceOptions.getXTraceOptions(XTraceOption.TRIGGER_TRACE.getKey() + "=0;" + XTraceOption.SW_KEYS.getKey() + "=3", null), new TraceDecision(true, true, traceConfig, TraceDecisionUtil.RequestType.REGULAR), true);
        assertEquals("trigger-trace=not-requested;ignored=trigger-trace", response.toString());
    }

    @Test
    public void testBadSignatureResponse() {
        TraceConfig traceConfig = new TraceConfig(TraceDecisionUtil.SAMPLE_RESOLUTION, SampleRateSource.OBOE_DEFAULT, TracingMode.ALWAYS.toFlags());

        XTraceOptionsResponse response;
        //bad timestamp
        XTraceOptions badTimestampOptions = new XTraceOptions(Collections.EMPTY_MAP, Collections.EMPTY_LIST, XTraceOptions.AuthenticationStatus.failure("bad-timestamp"));
        response = XTraceOptionsResponse.computeResponse(badTimestampOptions, new TraceDecision(false, true, traceConfig, TraceDecisionUtil.RequestType.BAD_SIGNATURE), true);
        assertEquals("auth=bad-timestamp", response.toString());

        //bad signature
        XTraceOptions badSignatureOptions = new XTraceOptions(Collections.EMPTY_MAP, Collections.EMPTY_LIST, XTraceOptions.AuthenticationStatus.failure("bad-signature"));
        response = XTraceOptionsResponse.computeResponse(badSignatureOptions, new TraceDecision(false, true, traceConfig, TraceDecisionUtil.RequestType.BAD_SIGNATURE), true);
        assertEquals("auth=bad-signature", response.toString());

    }

}
