package com.tracelytics.test.springboot;

import io.grpc.Context;
import io.opentelemetry.OpenTelemetry;
import io.opentelemetry.context.ContextUtils;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.HttpTextFormat;
import io.opentelemetry.trace.Span;
import io.opentelemetry.trace.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Controller
public class GreetingController {
    private Logger logger = LoggerFactory.getLogger(TestSpringBootApplication.class);
    private static Tracer tracer = OpenTelemetry.getTracerProvider().get("something","1.0.0");
    @GetMapping(value = "/greeting", produces = MediaType.TEXT_PLAIN_VALUE)
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
        try (Scope scope = ContextUtils.withScopedContext(extractedContext)) {
            // Automatically use the extracted SpanContext as parent.
            serverSpan = tracer.spanBuilder("pingServer").setSpanKind(Span.Kind.SERVER).setAttribute("ao.profile", true).startSpan();

            serverSpan.setAttribute("http.target", "/greeting");
            //do something
            TimeUnit.MILLISECONDS.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            if (serverSpan != null) {
                serverSpan.end();
            }
        }
        return "greeting";
    }

    @GetMapping("/listHeaders")
    public ResponseEntity<String> listAllHeaders(
            @RequestHeader Map<String, String> headers) {
        headers.forEach((key, value) -> {
            System.out.println(String.format("Header '%s' = %s", key, value));
        });
        
        return new ResponseEntity<String>(
                String.format("Listed %d headers", headers.size()), HttpStatus.OK);
    }
}