package com.appoptics.api.ext;

import com.google.auto.service.AutoService;
import com.tracelytics.agent.Agent;
import com.tracelytics.joboe.StartupManager;
import com.tracelytics.joboe.config.InvalidConfigException;
import io.opentelemetry.sdk.autoconfigure.spi.SdkTracerProviderConfigurer;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;

import java.util.concurrent.TimeUnit;

@AutoService(SdkTracerProviderConfigurer.class)
public class AppOpticsTracerProviderConfigurer implements SdkTracerProviderConfigurer {
    private static final String APPOPTICS_SERVICE_KEY = "otel.exporter.appoptics.service.key";
    public AppOpticsTracerProviderConfigurer() {
        String serviceKey = System.getProperty(APPOPTICS_SERVICE_KEY);
        try {
            Agent.initConfig(null, serviceKey);
            AgentChecker.waitUntilAgentReady(10, TimeUnit.SECONDS);
            StartupManager.flagSystemStartupCompleted();
        } catch (InvalidConfigException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void configure(SdkTracerProviderBuilder tracerProvider) {
        tracerProvider.addSpanProcessor(new AppOpticsProfilingSpanProcessor());
    }
}
