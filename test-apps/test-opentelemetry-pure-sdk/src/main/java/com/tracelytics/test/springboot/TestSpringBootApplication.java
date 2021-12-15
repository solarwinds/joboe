package com.tracelytics.test.springboot;

import com.appoptics.api.ext.AppOpticsSpanProcessor;
import com.appoptics.api.ext.AppOpticsTraceConfig;
import io.opentelemetry.OpenTelemetry;
import io.opentelemetry.sdk.trace.TracerSdkProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication
public class TestSpringBootApplication {
	private static final String SERVICE_KEY = System.getenv("SOLARWINDS_SERVICE_KEY");

	public static void main(String[] args) throws InterruptedException {
		//Begin A La Carte mode (from artifact appoptics-opentelemetry-sdk-standalone)
		TracerSdkProvider tracerProvider = (TracerSdkProvider) OpenTelemetry.getTracerProvider();
		//tracerProvider.addSpanProcessor(SimpleSpansProcessor.newBuilder(new AppOpticsSdkSpanExporter()).build());  //minimum span exporter
		tracerProvider.addSpanProcessor(AppOpticsSpanProcessor.newDefaultBuilder(SERVICE_KEY).build());  //span processor, which include our span exporter and also profiling capability
		tracerProvider.updateActiveTraceConfig(AppOpticsTraceConfig.getTraceConfig()); //optional, AppOptics sampler
		//End A La Carte mode

		//Being full init (from artifact appoptics-opentelemetry-sdk)
//        AppOpticsSdkTracer.init(); //convenient method to append our sampler, span exporters etc to OT SDK
		//End full init (from artifact appoptics-opentelemetry-sdk)

		//Code below is purely OT api usage - no reference to AO code nore OT SDK

		SpringApplication.run(TestSpringBootApplication.class, args);
	}
}
