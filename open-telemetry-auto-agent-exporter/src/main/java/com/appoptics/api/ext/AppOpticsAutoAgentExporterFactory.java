package com.appoptics.api.ext;


import io.opentelemetry.javaagent.spi.exporter.SpanExporterFactory;
import io.opentelemetry.sdk.trace.export.SpanExporter;

import java.util.Collections;
import java.util.Properties;
import java.util.Set;

public class AppOpticsAutoAgentExporterFactory implements SpanExporterFactory {
    private static final String APPOPTICS_SERVICE_KEY = "appoptics.service.key";

//    public SpanExporter fromConfig(Config config) {
//        final String serviceKey = config.getString(APPOPTICS_SERVICE_KEY, null);
//        return AppOpticsAutoAgentSpanExporter.newBuilder(serviceKey).build();
//    }

    @Override
    public SpanExporter fromConfig(Properties properties) {
        final String serviceKey = properties.getProperty(APPOPTICS_SERVICE_KEY);
        return AppOpticsAutoAgentSpanExporter.newBuilder(serviceKey).build();
    }

    @Override
    public Set<String> getNames() {
        return Collections.singleton("AppOptics Span Exporter");
    }
}
