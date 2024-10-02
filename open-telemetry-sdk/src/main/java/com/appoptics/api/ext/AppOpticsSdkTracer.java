package com.appoptics.api.ext;

import com.tracelytics.agent.Agent;
import com.tracelytics.joboe.StartupManager;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.TracerSdkProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpansProcessor;
import io.opentelemetry.trace.TracerProvider;
import io.opentelemetry.trace.spi.TraceProvider;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class AppOpticsSdkTracer {
    private static Logger logger = Logger.getLogger("ao-ot-sdk");
    public static TracerProvider init() {
        return init(10, TimeUnit.SECONDS);
    }

    public static TracerProvider init(int timeout, TimeUnit unit) {
        Agent.premain("profiler=true", null);   //TODO flag for profiler
        try {
            StartupManager.isAgentReady().get(timeout, unit);
        } catch (Exception e) {
            logger.warning("Agent is still not ready after waiting for " + timeout + " " + unit);
        }

        // Use the OpenTelemetry SDK
        TracerSdkProvider tracerProvider = OpenTelemetrySdk.getTracerProvider();

        tracerProvider.updateActiveTraceConfig(AppOpticsTraceConfig.getTraceConfig()); //use our own sampler

        tracerProvider.addSpanProcessor(new AppOpticsProfilingSpanProcessor(SimpleSpansProcessor.newBuilder(new AppOpticsSdkSpanExporter()).build()));
        return tracerProvider;
    }
}
