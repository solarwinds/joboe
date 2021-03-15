package com.appoptics.test.testspringboot;

import com.appoptics.api.ext.Trace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;


@RestController
public class GreetingController {
    private Logger logger = LoggerFactory.getLogger(TestSpringBootApplication.class);

    @GetMapping("/hello")
    public String hello() {
        List<Integer> input = Arrays.asList(1, 2, 3);
        Flux.merge(runFlux(input), runFlux(input), runFlux(input)).blockLast();

        return "hello";
    }

    private AtomicInteger count = new AtomicInteger();

    private Mono<Void> runFlux(Iterable<Integer> input) {
        final int fluxId = count.getAndIncrement();
        return Flux.fromIterable(input)
                .publishOn(Schedulers.elastic())
                .flatMap(i -> {
                    Trace.createEntryEvent("map-" + i).report();
                    System.out.println("Thread " + Thread.currentThread().getId() + " Flux " + fluxId +  " Current x-trace ID: " + Trace.getCurrentXTraceID());
                    try {
                        Thread.sleep(100 * i);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    Trace.createExitEvent("map-" + i).report();
                    return Mono.just(i);
                })
                .then();
    }
}