package com.tracelytics.test.springboot;

import com.appoptics.api.ext.*;
import io.opentelemetry.OpenTelemetry;
import io.opentelemetry.sdk.trace.export.SimpleSpansProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication
public class TestSpringBootApplication {

	public static void main(String[] args) {
		AppOpticsAgentSdk.init();
		AppOpticsTracerProvider tracerProvider = (AppOpticsTracerProvider) OpenTelemetry.getTracerProvider();  //ideally we don't want to overwrite the TracerProvider, but we need this now to sync scope
		tracerProvider.updateActiveTraceConfig(AppOpticsTraceConfig.getTraceConfig()); //regular OT SDK call
		tracerProvider.addSpanProcessor(AppOpticsSpanProcessor.newDefaultBuilder(null).build());


		SpringApplication.run(TestSpringBootApplication.class, args);
	}
}
