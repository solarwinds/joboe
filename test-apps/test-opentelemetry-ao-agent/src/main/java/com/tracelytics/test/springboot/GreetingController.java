package com.tracelytics.test.springboot;

import io.opentelemetry.OpenTelemetry;
import io.opentelemetry.trace.Span;
import io.opentelemetry.trace.Tracer;
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

import java.util.concurrent.TimeUnit;

@Controller
public class GreetingController {
    private Logger logger = LoggerFactory.getLogger(TestSpringBootApplication.class);
    private static Tracer tracer = OpenTelemetry.getTracerProvider().get("something","1.0.0");
    @GetMapping(value = "/greeting", produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    public String greeting(@RequestParam(name="name", required=false, defaultValue="World") String name, @RequestHeader MultiValueMap<String, String> headers, Model model) {
        logger.info("Received request with name " + name);
            // Automatically use the extracted SpanContext as parent.
        Span customSpan = tracer.spanBuilder("ot-span").setSpanKind(Span.Kind.SERVER).startSpan();
        try {
            //do something
            TimeUnit.MILLISECONDS.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            if (customSpan != null) {
                customSpan.end();
            }
        }
        return "greeting";
    }
}