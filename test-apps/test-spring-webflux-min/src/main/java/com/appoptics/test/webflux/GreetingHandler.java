package com.appoptics.test.webflux;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import reactor.core.publisher.Mono;

import java.time.Duration;
import org.springframework.core.SpringVersion;


@Component
public class GreetingHandler {

  public Mono<ServerResponse> hello(ServerRequest request) {
    return ServerResponse.ok().contentType(MediaType.TEXT_PLAIN)
            .body(BodyInserters.fromPublisher(Mono.just("Hello from Spring version: "+SpringVersion.getVersion()).delayElement(Duration.ofSeconds(1)), String.class));
  }
}
