/*
 * Copyright 2015, gRPC Authors All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.grpc.examples.routeguide;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.appoptics.api.ext.AgentChecker;
import com.appoptics.api.ext.Trace;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.examples.routeguide.RouteGuideGrpc.RouteGuideBlockingStub;
import io.grpc.examples.routeguide.RouteGuideGrpc.RouteGuideFutureStub;
import io.grpc.examples.routeguide.RouteGuideGrpc.RouteGuideStub;
import io.grpc.stub.StreamObserver;

/**
 * Sample client code that makes gRPC calls to the server.
 */
public class TestClientError {
    private static final Logger logger = Logger.getLogger(TestClientError.class.getName());

    private final ManagedChannel channel;
    private final RouteGuideBlockingStub blockingStub;
    private final RouteGuideFutureStub futureStub;
    private final RouteGuideStub asyncStub;

    
    /** Construct client for accessing RouteGuide server at {@code host:port}. */
    public TestClientError(String host, int port) {
        this(ManagedChannelBuilder.forAddress(host, port).usePlaintext(true));
    }

    /** Construct client for accessing RouteGuide server using the existing channel. */
    public TestClientError(ManagedChannelBuilder<?> channelBuilder) {
        channel = channelBuilder.build();
        blockingStub = RouteGuideGrpc.newBlockingStub(channel);
        asyncStub = RouteGuideGrpc.newStub(channel);
        futureStub = RouteGuideGrpc.newFutureStub(channel);
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

  
    /** Issues several different requests and then exits. */
    public static void main(String[] args) throws InterruptedException {
        AgentChecker.waitUntilAgentReady(5, TimeUnit.SECONDS);

        TestClientError client;
        
        Trace.startTrace("test-grpc-client-exception").report();
        client = new TestClientError("localhost", 8980);
        try {
            client.testClientException();
        } finally {
            client.shutdown();
            Trace.endTrace("test-grpc-client-exception");
        }

        Trace.startTrace("test-grpc-client-not-completed").report();
        client = new TestClientError("localhost", 8980);
        try {
            client.testClientNotCompleted();
        } finally {
            client.shutdown();
            Trace.endTrace("test-grpc-client-not-completed");
        }

    }

    /**
     * Test client streaming exception
     * @throws InterruptedException
     */
    private void testClientException() throws InterruptedException {
        //quick exceptions, the client wouldn't even send anything to server
        StreamObserver<Point> recordRoute = asyncStub.recordRoute(new DummyStreamObserver<RouteSummary>());
        recordRoute.onError(new RuntimeException("test exception"));
        
        StreamObserver<RouteNote> routeChat = asyncStub.routeChat(new DummyStreamObserver<RouteNote>());
        routeChat.onError(new RuntimeException("test exception"));
        
        
        recordRoute = asyncStub.recordRoute(new DummyStreamObserver<RouteSummary>());
        routeChat = asyncStub.routeChat(new DummyStreamObserver<RouteNote>());
      //slower exceptions, the client would trigger exception on server side as well
        
        TimeUnit.SECONDS.sleep(1);
        recordRoute.onError(new RuntimeException("test exception"));
        routeChat.onError(new RuntimeException("test exception"));
    }

    /**
     * Test client with dangling client streams. Take note that this WILL create span with missing exit on client side and is expected, 
     * see https://github.com/librato/joboe/pull/725   
     * @throws InterruptedException
     */
    private void testClientNotCompleted() throws InterruptedException {
        StreamObserver<Point> recordRoute = asyncStub.recordRoute(new DummyStreamObserver<RouteSummary>());
        StreamObserver<RouteNote> routeChat = asyncStub.routeChat(new DummyStreamObserver<RouteNote>());
        //do NOT call onComplete/onError at all
        TimeUnit.SECONDS.sleep(1); //wait for a bit so client will send something to server
    }
    
    private class DummyStreamObserver<T> implements StreamObserver<T> {
        @Override
        public void onNext(T value) {
        }

        @Override
        public void onError(Throwable t) {
        }

        @Override
        public void onCompleted() {
        }
        
    }

    private void info(String msg, Object... params) {
        logger.log(Level.INFO, msg, params);
    }

    private void warning(String msg, Object... params) {
        logger.log(Level.WARNING, msg, params);
    }

   
}