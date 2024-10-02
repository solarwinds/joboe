package com.tracelytics.test.springboot;

import com.appoptics.api.ext.AppOpticsContextUtils;
import com.tracelytics.test.Util;
import io.grpc.Context;
import io.opentelemetry.OpenTelemetry;
import io.opentelemetry.context.ContextUtils;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.HttpTextFormat;
import io.opentelemetry.trace.Span;
import io.opentelemetry.trace.Tracer;
import io.opentelemetry.trace.TracingContextUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Assuming the Spring controller is NOT instrumented by AO agent (Ie with agent but "agent.exclude" : ["SPRING", "SERVLET"]
 */
@Controller
public class GreetingNoAoController {
    private Logger logger = LoggerFactory.getLogger(TestSpringBootApplication.class);
    private static Tracer tracer = OpenTelemetry.getTracerProvider().get("something","1.0.0");
    @GetMapping(value = "/greeting-no-ao", produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    public String greeting(@RequestParam(name="name", required=false, defaultValue="World") String name, @RequestHeader MultiValueMap<String, String> headers, Model model) {
        logger.info("Received request with name " + name);

        //Assuming this Spring application is NOT running with an agent
        // Extract the SpanContext and other elements from the request.
        Context extractedContext = OpenTelemetry.getPropagators().getHttpTextFormat()
                .extract(Context.current(), headers, new HttpTextFormat.Getter<MultiValueMap<String, String>>() {
                    @Override
                    public String get(MultiValueMap<String, String> headers, String key) {
                        if (headers.containsKey(key)) {
                            return headers.get(key).get(0);
                        }
                        return null;
                    }});

        Span serverSpan = null;
        try (Scope scope = AppOpticsContextUtils.withScopedContext(extractedContext)) {
            logger.info("current span after with scoped " + TracingContextUtils.getCurrentSpan().getContext().toString());
            // Automatically use the extracted SpanContext as parent.
            serverSpan = tracer.spanBuilder("pingServer").setSpanKind(Span.Kind.SERVER).setAttribute("ao.profile", true).startSpan();

            serverSpan.setAttribute("http.target", "/greeting");
            //do something that triggers AO instrumentation (non spring controller)
            Util.getURI("http://www.google.com");
        } finally {
            if (serverSpan != null) {
                serverSpan.end();
            }
        }
        return "greeting";
    }
}