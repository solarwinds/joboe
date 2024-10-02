package com.example.low;

import java.util.concurrent.CompletionStage;

import akka.actor.ActorSystem;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.http.javadsl.IncomingConnection;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.model.ContentTypes;
import akka.http.javadsl.model.HttpMethods;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.model.Uri;
import akka.japi.function.Function;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;

public class LowLevelApi {

    public static void bind(String host, int port, ActorSystem system) {
        final Materializer materializer = ActorMaterializer.create(system);
         
        Source<IncomingConnection, CompletionStage<ServerBinding>> serverSource =
                Http.get(system).bind(ConnectHttp.toHost(host, port), materializer);
         
        final Function<HttpRequest, HttpResponse> requestHandler =
            new Function<HttpRequest, HttpResponse>() {
                private final HttpResponse NOT_FOUND =
                    HttpResponse.create()
                        .withStatus(404)
                        .withEntity("Unknown resource!");
         
         
                @Override
                public HttpResponse apply(HttpRequest request) throws Exception {
                    Uri uri = request.getUri();
                    if (request.method() == HttpMethods.GET) {
                        if (uri.path().equals("/"))
                            return
                                HttpResponse.create()
                                    .withEntity(ContentTypes.TEXT_HTML_UTF8,
                                        "<html><body>Hello world!</body></html>");
                        else if (uri.path().equals("/hello")) {
                            String name = uri.query().get("name").orElse("Mister X");
         
                            return
                                HttpResponse.create()
                                    .withEntity("Hello " + name + "!");
                        }
                        else if (uri.path().equals("/ping"))
                            return HttpResponse.create().withEntity("PONG!");
                        else if (uri.path().equals("/crash"))
                            throw new RuntimeException("BOOM!");
                        else
                            return NOT_FOUND;
                    }
                    else return NOT_FOUND;
                }
            };
         
        CompletionStage<ServerBinding> serverBindingFuture =
            serverSource.to(Sink.foreach(connection -> {
                      System.out.println("Accepted new connection from " + connection.remoteAddress());
         
                      connection.handleWithSyncHandler(requestHandler, materializer);
                      // this is equivalent to
                      //connection.handleWith(Flow.of(HttpRequest.class).map(requestHandler), materializer);
                })).run(materializer);

        System.out.println("Low level api Server online at http://" + host + ":" + port);
    }

}