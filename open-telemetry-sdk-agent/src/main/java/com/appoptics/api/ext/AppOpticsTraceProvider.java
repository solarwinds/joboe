package com.appoptics.api.ext;

import com.google.auto.service.AutoService;
import io.opentelemetry.trace.TracerProvider;
import io.opentelemetry.trace.spi.TraceProvider;

/**
 * A workaround class to synchronize OT scope with AO scope, we do NOT need this if we can detect scope via some interpreter mechanism as proposed in
 * https://github.com/open-telemetry/opentelemetry-java/issues/922
 */
@AutoService(TraceProvider.class)
public class AppOpticsTraceProvider implements TraceProvider {
    @Override
    public TracerProvider create() {
        return new AppOpticsTracerProvider();
    }
}
