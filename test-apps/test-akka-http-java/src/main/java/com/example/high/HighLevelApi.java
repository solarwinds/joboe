package com.example.high;

import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.model.*;
import akka.http.javadsl.server.AllDirectives;
import akka.http.javadsl.server.ExceptionHandler;
import akka.http.javadsl.server.Route;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Flow;
import scala.concurrent.duration.Duration;

import java.io.IOException;
import java.util.concurrent.*;

public class HighLevelApi extends AllDirectives {
    public static void bind(String host, int port, ActorSystem system) throws IOException {
        // boot up server using the route as defined below
        //ActorSystem system = ActorSystem.create("routes");

        final Http http = Http.get(system);
        final ActorMaterializer materializer = ActorMaterializer.create(system);

        //In order to access all directives we need an instance where the routes are define.
        HighLevelApi app = new HighLevelApi();

        final Flow<HttpRequest, HttpResponse, NotUsed> routeFlow = app.createRoute().flow(system, materializer);
        final CompletionStage<ServerBinding> binding = http.bindAndHandle(routeFlow,
                ConnectHttp.toHost(host, port), materializer);

        System.out.println("High level API Server online at http://" + host + ":" + port);
        System.in.read(); // let it run until user presses return

        binding.thenCompose(ServerBinding::unbind) // trigger unbinding from the port
                .thenAccept(unbound -> system.terminate()); // and shutdown when done
    }

    private Route createRoute() {
        final ActorSystem system = ActorSystem.create();
        final ExceptionHandler myExceptionHandler = ExceptionHandler.newBuilder().match(MyException.class, x ->
                complete(StatusCodes.OK, "Handled exception")).build();
        return handleExceptions(myExceptionHandler, () -> concat(
                path("hello", () ->
                        get(() ->
                                complete("<h1>Say hello to akka-http</h1>"))),
                path("crash", () ->
                        get(() -> {
                            throw new RuntimeException("KABOOM");
                        })),
                path("handled-exception", () ->
                        get(() -> {
                            throw new MyException("this should be handled");
                        })),
                path("wait", () ->
                        parameter("duration", durationString -> {
                            try {
                                long duration = durationString != null ? Long.parseLong(durationString) : 1;
                                TimeUnit.SECONDS.sleep(duration);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            return complete("Waited for " + durationString + " sec(s)");
                        })),
                path("cross-server", () ->
                        parameterOptional("host", host ->
                            parameterOptional("port", port ->
                                parameterOptional("method", methodKey -> {
                                    HttpMethod method = methodKey != null ? HttpMethods.lookup(methodKey.orElse("GET").toUpperCase()).orElse(HttpMethods.GET) : HttpMethods.GET;
                                    String url = "http://" + host.orElse("localhost") + ":" + port.orElse("8080");
                                    CompletionStage<HttpResponse> future1 = Http.get(system).singleRequest(HttpRequest.create(url).withMethod(method));
                                    CompletionStage<HttpResponse> future2 = Http.get(system).singleRequest(HttpRequest.create(url).withMethod(method));

                                    HttpResponse response1 = future1.toCompletableFuture().join();
                                    HttpResponse response2 = future2.toCompletableFuture().join();
                                    return complete("From " + url + " response codes are " + response1.status() + " and " + response2.status());
                                })
                            )
                        )),
                path( "timeout", () ->
                        withRequestTimeout(Duration.create(1, TimeUnit.SECONDS), () -> {
                            CompletableFuture<String> slowFuture = CompletableFuture.supplyAsync(() -> {
                                try {
                                    TimeUnit.SECONDS.sleep(2);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                return "Too Late!";
                            });
                            return completeOKWithFutureString(slowFuture);
                         }))
        ));
    }

    private static class MyException extends RuntimeException {
        MyException(String message) {
            super(message);
        }
    }
    
    
}
