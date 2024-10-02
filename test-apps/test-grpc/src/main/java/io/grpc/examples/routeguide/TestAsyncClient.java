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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.appoptics.api.ext.AgentChecker;
import com.appoptics.api.ext.Trace;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.Message;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.examples.routeguide.RouteGuideGrpc.RouteGuideFutureStub;
import io.grpc.examples.routeguide.RouteGuideGrpc.RouteGuideStub;
import io.grpc.stub.StreamObserver;

/**
 * Sample client code that makes gRPC calls to the server.
 */
public class TestAsyncClient {
    private static final Logger logger = Logger.getLogger(TestAsyncClient.class.getName());

    private final ManagedChannel channel;
    private final RouteGuideFutureStub futureStub;
    private final RouteGuideStub asyncStub;

    private Random random = new Random();
    private TestHelper testHelper;

    /** Construct client for accessing RouteGuide server at {@code host:port}. */
    public TestAsyncClient(String host, int port) {
        this(ManagedChannelBuilder.forAddress(host, port).usePlaintext());
    }

    /** Construct client for accessing RouteGuide server using the existing channel. */
    public TestAsyncClient(ManagedChannelBuilder<?> channelBuilder) {
        channel = channelBuilder.build();
        asyncStub = RouteGuideGrpc.newStub(channel);
        futureStub = RouteGuideGrpc.newFutureStub(channel);
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    /** Issues several different requests and then exits. */
    public static void main(String[] args) throws InterruptedException {
        List<Feature> features;
        try {
            features = RouteGuideUtil.parseFeatures(RouteGuideUtil.getDefaultFeaturesFile());
        } catch (IOException ex) {
            ex.printStackTrace();
            return;
        }

        AgentChecker.waitUntilAgentReady(5, TimeUnit.SECONDS);

        Trace.startTrace("test-grpc-async-client").report();
        TestAsyncClient client = new TestAsyncClient("localhost", 8980);
        try {
            // Get features using Async stub
            client.getFeaturesAsync(409146138, -746188906, 409146138, -746188906);

            // Get features using Future stub
            client.getFeaturesFuture(409146138, -746188906, 409146138, -746188906);

            // List features using Async stub
            client.listFeaturesAsync(400000000, -750000000, 420000000, -730000000, 2);

            // Record a few randomly selected points from the features file.
            client.recordRoute(features, 10, 2);

            // Send and receive some notes.
            CountDownLatch finishLatch = client.routeChat(2);

            if (!finishLatch.await(100, TimeUnit.MINUTES)) {
                client.warning("routeChat can not finish within 1 minutes");
            }
        } finally {
            client.shutdown();
            Trace.endTrace("test-grpc-async-client");
        }
    }

    /**
     * Async client-streaming example. Sends {@code numPoints} randomly chosen points from {@code
     * features} with a variable delay in between. Prints the statistics when they are sent from the server.
     */
    public void recordRoute(List<Feature> features, int numPoints, int concurrency) throws InterruptedException {
        info("*** RecordRoute");
        final CountDownLatch finishLatch = new CountDownLatch(concurrency);

        StreamObserver<RouteSummary> responseObserver = new StreamObserver<RouteSummary>() {
            @Override
            public void onNext(RouteSummary summary) {
                info("Finished trip with {0} points. Passed {1} features. " + "Travelled {2} meters. It took {3} seconds.", summary.getPointCount(), summary.getFeatureCount(),
                        summary.getDistance(), summary.getElapsedTime());
                if (testHelper != null) {
                    testHelper.onMessage(summary);
                }
            }

            @Override
            public void onError(Throwable t) {
                warning("RecordRoute Failed: {0}", Status.fromThrowable(t));
                if (testHelper != null) {
                    testHelper.onRpcError(t);
                }
                finishLatch.countDown();
            }

            @Override
            public void onCompleted() {
                info("Finished RecordRoute");
                finishLatch.countDown();
            }
        };

        List<StreamObserver<Point>> requestObservers = new ArrayList<StreamObserver<Point>>();

        for (int i = 0; i < concurrency; i++) {
            requestObservers.add(asyncStub.recordRoute(responseObserver));
        }

        // Send numPoints points randomly selected from the features list.
        for (int i = 0; i < numPoints; ++i) {
            int index = random.nextInt(features.size());
            Point point = features.get(index).getLocation();
            info("Visiting point {0}, {1}", RouteGuideUtil.getLatitude(point), RouteGuideUtil.getLongitude(point));
            Iterator<StreamObserver<Point>> iterator = requestObservers.iterator();
            while (iterator.hasNext()) {
                StreamObserver<Point> requestObserver = iterator.next();
                try {
                    requestObserver.onNext(point);
                } catch (RuntimeException e) {
                    // Cancel RPC
                    requestObserver.onError(e);
                    iterator.remove();
                }
            }

            // Sleep for a bit before sending the next one.
            Thread.sleep(random.nextInt(1000) + 500);
            if (finishLatch.getCount() == 0) {
                // RPC completed or errored before we finished sending.
                // Sending further requests won't error, but they will just be thrown away.
                return;
            }
        }

        for (StreamObserver<Point> requestObserver : requestObservers) {
            // Mark the end of requests
            requestObserver.onCompleted();
        }

        // Receiving happens asynchronously
        if (!finishLatch.await(1, TimeUnit.MINUTES)) {
            warning("recordRoute can not finish within 1 minutes");
        }
    }

    /**
     * Bi-directional example, which can only be asynchronous. Send some chat messages, and print any chat messages that are sent from the server.
     */
    public CountDownLatch routeChat(int concurrency) {
        info("*** RouteChat");
        final CountDownLatch finishLatch = new CountDownLatch(concurrency);
        StreamObserver<RouteNote> responseObserver = new StreamObserver<RouteNote>() {
            @Override
            public void onNext(RouteNote note) {
                info("Got message \"{0}\" at {1}, {2}", note.getMessage(), note.getLocation().getLatitude(), note.getLocation().getLongitude());
                if (testHelper != null) {
                    testHelper.onMessage(note);
                }
            }

            @Override
            public void onError(Throwable t) {
                warning("RouteChat Failed: {0}", Status.fromThrowable(t));
                if (testHelper != null) {
                    testHelper.onRpcError(t);
                }
                finishLatch.countDown();
            }

            @Override
            public void onCompleted() {
                info("Finished RouteChat");
                finishLatch.countDown();
            }
        };

        List<StreamObserver<RouteNote>> requestObservers = new ArrayList<StreamObserver<RouteNote>>();

        for (int i = 0; i < concurrency; i++) {
            requestObservers.add(asyncStub.routeChat(responseObserver));
        }

        RouteNote[] requests = { newNote("First message", 0, 0), newNote("Second message", 0, 1), newNote("Third message", 1, 0), newNote("Fourth message", 1, 1) };

        for (RouteNote request : requests) {
            info("Sending message \"{0}\" at {1}, {2}", request.getMessage(), request.getLocation().getLatitude(), request.getLocation().getLongitude());

            Iterator<StreamObserver<RouteNote>> iterator = requestObservers.iterator();
            while (iterator.hasNext()) {
                StreamObserver<RouteNote> requestObserver = iterator.next();
                try {
                    requestObserver.onNext(request);
                } catch (RuntimeException e) {
                    // Cancel RPC
                    requestObserver.onError(e);
                    iterator.remove();
                }
            }
        }

        for (StreamObserver<RouteNote> requestObserver : requestObservers) {
            // Mark the end of requests
            requestObserver.onCompleted();
        }

        // return the latch while receiving happens asynchronously
        return finishLatch;
    }

    /**
     * Gets features using future stub and concurrent calls
     * 
     * @param pairs
     */
    private void getFeaturesFuture(int... pairs) {
        if (pairs.length % 2 != 0) {
            warning("not valid pairs");
        }
        List<ListenableFuture<Feature>> featureFutures = new ArrayList<>();

        for (int i = 0; i < pairs.length / 2; i++) {
            int lat = pairs[i * 2];
            int lon = pairs[i * 2 + 1];

            info("*** GetFeatures- {2}: lat={0} lon={1}", lat, lon, i);

            Point request = Point.newBuilder().setLatitude(lat).setLongitude(lon).build();
            featureFutures.add(futureStub.getFeature(request));
        }

        for (ListenableFuture<Feature> featureFuture : featureFutures) {
            featureFuture.addListener(() -> {
                try {
                    Feature feature = featureFuture.get();
                    if (testHelper != null) {
                        testHelper.onMessage(feature);
                    }
                    if (RouteGuideUtil.exists(feature)) {
                        info("Found feature called \"{0}\" at {1}, {2}", feature.getName(), RouteGuideUtil.getLatitude(feature.getLocation()),
                                RouteGuideUtil.getLongitude(feature.getLocation()));
                    } else {
                        info("Found no feature at {0}, {1}", RouteGuideUtil.getLatitude(feature.getLocation()), RouteGuideUtil.getLongitude(feature.getLocation()));
                    }
                } catch (Exception e) {
                    if (e.getCause() instanceof StatusRuntimeException) {
                        StatusRuntimeException statusRuntimeException = (StatusRuntimeException) e.getCause();
                        warning("RPC failed: {0}", statusRuntimeException.getStatus());
                        if (testHelper != null) {
                            testHelper.onRpcError(statusRuntimeException);
                        }
                    } else {
                        warning("RPC failed: {0}", e.getMessage());
                        if (testHelper != null) {
                            testHelper.onRpcError(e);
                        }
                    }
                }
            }, MoreExecutors.directExecutor());
        }

        for (ListenableFuture<Feature> featureFuture : featureFutures) {
            try {
                featureFuture.get();
            } catch (InterruptedException | ExecutionException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } // just block until all finishes
        }
    }

    /**
     * Gets features using async stub and concurrent calls
     * 
     * @param pairs
     * @throws InterruptedException
     */
    private void getFeaturesAsync(int... pairs) throws InterruptedException {
        if (pairs.length % 2 != 0) {
            warning("not valid pairs");
        }
        int concurrency = pairs.length / 2;

        final CountDownLatch finishLatch = new CountDownLatch(concurrency);

        StreamObserver<Feature> responseObserver = new StreamObserver<Feature>() {
            @Override
            public void onNext(Feature feature) {
                if (RouteGuideUtil.exists(feature)) {
                    info("Found feature called \"{0}\" at {1}, {2}", feature.getName(), RouteGuideUtil.getLatitude(feature.getLocation()),
                            RouteGuideUtil.getLongitude(feature.getLocation()));
                } else {
                    info("Found no feature at {0}, {1}", RouteGuideUtil.getLatitude(feature.getLocation()), RouteGuideUtil.getLongitude(feature.getLocation()));
                }
            }

            @Override
            public void onError(Throwable t) {
                warning("GetFeature Failed: {0}", Status.fromThrowable(t));
                if (testHelper != null) {
                    testHelper.onRpcError(t);
                }
                finishLatch.countDown();
            }

            @Override
            public void onCompleted() {
                info("Finished GetFeature");
                finishLatch.countDown();
            }
        };

        for (int i = 0; i < pairs.length / 2; i++) {
            int lat = pairs[i * 2];
            int lon = pairs[i * 2 + 1];

            info("*** GetFeatures- {2}: lat={0} lon={1}", lat, lon, i);

            Point request = Point.newBuilder().setLatitude(lat).setLongitude(lon).build();
            asyncStub.getFeature(request, responseObserver);
        }

        // Receiving happens asynchronously
        if (!finishLatch.await(1, TimeUnit.MINUTES)) {
            warning("recordRoute can not finish within 1 minutes");
        }

    }

    public void listFeaturesAsync(int lowLat, int lowLon, int hiLat, int hiLon, int concurrency) throws InterruptedException {
        info("*** ListFeatures: lowLat={0} lowLon={1} hiLat={2} hiLon={3}", lowLat, lowLon, hiLat, hiLon);
        final CountDownLatch finishLatch = new CountDownLatch(concurrency);
        StreamObserver<Feature> responseObserver = new StreamObserver<Feature>() {
            @Override
            public void onNext(Feature feature) {
                if (RouteGuideUtil.exists(feature)) {
                    info("Found feature called \"{0}\" at {1}, {2}", feature.getName(), RouteGuideUtil.getLatitude(feature.getLocation()),
                            RouteGuideUtil.getLongitude(feature.getLocation()));
                } else {
                    info("Found no feature at {0}, {1}", RouteGuideUtil.getLatitude(feature.getLocation()), RouteGuideUtil.getLongitude(feature.getLocation()));
                }
            }

            @Override
            public void onError(Throwable t) {
                warning("ListFeatures Failed: {0}", Status.fromThrowable(t));
                if (testHelper != null) {
                    testHelper.onRpcError(t);
                }
                finishLatch.countDown();
            }

            @Override
            public void onCompleted() {
                info("Finished ListFeatures");
                finishLatch.countDown();
            }
        };
        
        Rectangle request = Rectangle.newBuilder().setLo(Point.newBuilder().setLatitude(lowLat).setLongitude(lowLon).build())
                .setHi(Point.newBuilder().setLatitude(hiLat).setLongitude(hiLon).build()).build();
        
        
        for (int i = 0; i < concurrency; i++) {
            try {
                asyncStub.listFeatures(request, responseObserver);
            } catch (StatusRuntimeException e) {
                warning("RPC failed: {0}", e.getStatus());
                if (testHelper != null) {
                    testHelper.onRpcError(e);
                }
            }
        }
        
        // Receiving happens asynchronously
        if (!finishLatch.await(1, TimeUnit.MINUTES)) {
            warning("ListFeatures can not finish within 1 minutes");
        }
    }

    private void info(String msg, Object... params) {
        logger.log(Level.INFO, msg, params);
    }

    private void warning(String msg, Object... params) {
        logger.log(Level.WARNING, msg, params);
    }

    private RouteNote newNote(String message, int lat, int lon) {
        return RouteNote.newBuilder().setMessage(message).setLocation(Point.newBuilder().setLatitude(lat).setLongitude(lon).build()).build();
    }

    /**
     * Only used for unit test, as we do not want to introduce randomness in unit test.
     */
    @VisibleForTesting
    void setRandom(Random random) {
        this.random = random;
    }

    /**
     * Only used for helping unit test.
     */
    @VisibleForTesting
    interface TestHelper {
        /**
         * Used for verify/inspect message received from server.
         */
        void onMessage(Message message);

        /**
         * Used for verify/inspect error received from server.
         */
        void onRpcError(Throwable exception);
    }

    @VisibleForTesting
    void setTestHelper(TestHelper testHelper) {
        this.testHelper = testHelper;
    }
}