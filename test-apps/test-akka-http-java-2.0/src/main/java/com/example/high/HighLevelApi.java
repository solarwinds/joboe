package com.example.high;

import java.io.IOException;

import akka.actor.ActorSystem;
import akka.http.javadsl.model.ContentTypes;
import akka.http.javadsl.model.StatusCodes;
import akka.http.javadsl.model.Uri;
import akka.http.javadsl.server.Handler1;
import akka.http.javadsl.server.HttpApp;
import akka.http.javadsl.server.RequestContext;
import akka.http.javadsl.server.RequestVal;
import akka.http.javadsl.server.Route;
import akka.http.javadsl.server.RouteResult;
import akka.http.javadsl.server.values.Parameters;

public class HighLevelApi extends HttpApp {
    public static void bind(String host, int port, ActorSystem system) throws IOException {
        // HttpApp.bindRoute expects a route being provided by HttpApp.createRoute
        new HighLevelApi().bindRoute(host, port, system);
    }
 
    // A RequestVal is a type-safe representation of some aspect of the request.
    // In this case it represents the `name` URI parameter of type String.
    private RequestVal<String> name = Parameters.stringValue("name").withDefault("Mister X");
    private RequestVal<String> xVal = Parameters.stringValue("x");
    private RequestVal<String> yVal = Parameters.stringValue("y");
    private RequestVal<String> zVal = Parameters.stringValue("z");
 
    @Override
    public Route createRoute() {
        // This handler generates responses to `/hello?name=XXX` requests
        Route helloRoute =
            handleWith1(name, (ctx, name) -> ctx.complete("Hello " + name + "!"));
        Route oldHelloRoute =
            handleWith1(name,  
                new Handler1<String>() {
            @Override
            public RouteResult apply(RequestContext ctx, String name) {
                return ctx.complete("Hello " + name + "!");
            }
        });
        
        return
            // here the complete behavior for this server is defined
            route(
                // only handle GET requests
                get(
                    // matches the empty path
                    pathSingleSlash().route(
                        // return a constant string with a certain content type
                        complete(ContentTypes.TEXT_HTML_UTF8,
                                "<html><body>Hello world!</body></html>")
                    ),
                    path("with-status").route(
                            // return a constant string with a certain content type
                            completeWithStatus(StatusCodes.OK)
                    ),
                    path("ping").route(
                        // return a simple `text/plain` response
                        complete("PONG!")
                    ),
                    path("hello").route(
                        // uses the route defined above
                        helloRoute
                    ),
                    path("old-hello").route( //java 1.7 syntax
                        // uses the route defined above
                        oldHelloRoute
                    ),
                    path("redirect").route(                        
                        redirect(Uri.create("http://www.google.com"), StatusCodes.TEMPORARY_REDIRECT)
                    ),
                    path("exception").route(            
                        handleWith(ctx -> {
                            if (true) {
                                throw new RuntimeException("Testing");
                            }
                            return ctx.complete("not going to here");
                        })
                    ),
                    path("timeout").route(            
                        handleWith(ctx -> {
                            Thread.sleep(20000);
                            return ctx.complete("timeout");  
                        })
                    ),
                    path("handle").route(
                        handleWith1(xVal, (ctx, x) -> ctx.complete("Got " + x)),
                        handleWith2(yVal, zVal, (ctx, y, z) -> ctx.complete("Got " + y + " and " + z)))
                        
                )
            );
    }
    
    
}
