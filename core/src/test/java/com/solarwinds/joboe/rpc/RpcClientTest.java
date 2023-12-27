package com.solarwinds.joboe.rpc;



import com.solarwinds.joboe.BsonBufferException;
import com.solarwinds.joboe.Constants;
import com.solarwinds.joboe.Context;
import com.solarwinds.joboe.Event;
import com.solarwinds.joboe.ebson.BsonDocument;
import com.solarwinds.joboe.ebson.BsonDocuments;
import com.solarwinds.joboe.rpc.RpcClient.TaskType;
import com.solarwinds.joboe.settings.PollingSettingsFetcherTest;
import com.solarwinds.joboe.settings.SettingsArg;
import com.solarwinds.util.TimeUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.GeneralSecurityException;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public abstract class RpcClientTest {
    private static final int TEST_SERVER_PORT_BASE = 10148;
    protected static final String TEST_SERVER_HOST = "localhost";
    protected static final String TEST_CLIENT_ID = "123";
    protected static final List<Event> TEST_EVENTS = generateTestEvents();
    protected static final Event BIG_EVENT = generateBigEvent();

    private static final String TEST_SERVER_CERT_LOCATION = "src/test/java/com/solarwinds/joboe/rpc/test-collector-public.pem";
    private static final String INVALID_CERT_LOCATION = "src/test/java/com/solarwinds/joboe/rpc/invalid-collector.crt";
    private static int portWalker = TEST_SERVER_PORT_BASE;
    private static int testServerPort = TEST_SERVER_PORT_BASE;
    protected static final List<Settings> TEST_SETTINGS = generateTestSettings();


    private static final RpcClient.RetryParamConstants QUICK_RETRY = new RpcClient.RetryParamConstants(100, 200, 3);

    protected TestCollector testCollector;

    @BeforeEach
    public void setUp() throws Exception {
        testServerPort = locateAvailablePort();
        testCollector = startCollector(testServerPort);
    }




    @AfterEach
    public void tearDown() throws Exception {
        testCollector.stop();
    }

    protected interface TestCollector {
        List<ByteBuffer> stop();
        List<ByteBuffer> flush();
        Map<TaskType, Long> getCallCountStats();
    }

    protected abstract TestCollector startCollector(int port) throws IOException;
    protected abstract TestCollector startRedirectCollector(int port, String redirectArg) throws IOException;
    protected abstract TestCollector startRatedCollector(int port, int processingTimePerMessage, ResultCode limitExceededCode) throws IOException;
    protected abstract TestCollector startBiasedTestCollector(int port, Map<TaskType, ResultCode> taskToResponseCode) throws IOException;
    //Test server that throws Runtime exception on every other message
    protected abstract TestCollector startErroneousTestCollector(int port, double errorPercentage) throws IOException;
    protected abstract TestCollector startSoftDisabledTestCollector(int port, String warning)  throws IOException;

    protected static String getServerPublicKeyLocation() {
        return TEST_SERVER_CERT_LOCATION;
    }

    private static List<Settings> generateTestSettings() {
        List<Settings> settings = new ArrayList<Settings>();

        Map<String, ByteBuffer> arguments = new HashMap<String, ByteBuffer>();

        arguments.put(SettingsArg.BUCKET_CAPACITY.getKey(), SettingsArg.BUCKET_CAPACITY.toByteBuffer(32.0));
        arguments.put(SettingsArg.BUCKET_RATE.getKey(), SettingsArg.BUCKET_RATE.toByteBuffer(2.0));

        settings.add(new Settings(Settings.OBOE_SETTINGS_TYPE_DEFAULT_SAMPLE_RATE, PollingSettingsFetcherTest.DEFAULT_FLAGS_STRING, TimeUtils.getTimestampMicroSeconds(), 1000000, 600, "test-layer", arguments));
        return settings;
    }

    private static List<Event> generateTestEvents() {
        List<Event> testEvents = new ArrayList<Event>();
        Context.getMetadata().randomize();
        Event entryEvent = Context.createEvent();
        entryEvent.addInfo("Layer", "test-thrift");
        entryEvent.addInfo("Label", "entry");
        testEvents.add(entryEvent);


        Event exitEvent = Context.createEvent();
        exitEvent.addInfo("Layer", "test-thrift");
        exitEvent.addInfo("Label", "exit");
        testEvents.add(exitEvent);

        return testEvents;
    }

    private static Event generateBigEvent() {
        Context.getMetadata().randomize();
        Event testEvent = Context.createEvent();
        for (int i = 0; i < 100; i ++) { //each event is around 100 kb
            testEvent.addInfo(String.valueOf(i), new byte[1024]);
        }

        return testEvent;
    }

    protected abstract ProtocolClientFactory<?> getProtocolClientFactory(URL certUrl) throws IOException, GeneralSecurityException;

    @Test
    public void testConnectValidServer() throws Exception {
        System.out.println("running testConnectValidServer");
        Client client = null;
        try {
            client = new RpcClient(TEST_SERVER_HOST, testServerPort, TEST_CLIENT_ID, getProtocolClientFactory(new File(getServerPublicKeyLocation()).toURI().toURL()));
            assertEquals(com.solarwinds.joboe.rpc.ResultCode.OK, client.postEvents(TEST_EVENTS, null).get().getResultCode());
            assertEventEquals(TEST_EVENTS, testCollector.flush());
        } finally {
            if (client != null) {
                client.close();
            }
        }

    }

    @Test
    public void testConnectInvalidServer() throws Exception {
        System.out.println("running testConnectInvalidServer");
        Client client = null;
        try {
            client = new RpcClient(TEST_SERVER_HOST, testServerPort, TEST_CLIENT_ID, QUICK_RETRY, getProtocolClientFactory(new File(INVALID_CERT_LOCATION).toURI().toURL()));
        } catch (Exception e) {
            //expected;
            System.out.println("Fret not! Expected ^^");
        } finally {
            if (client != null) {
                client.close();
            }
        }
    }

    @Test
    public void testConnectDeadServer() throws Exception {
        System.out.println("running testConnectDeadServer");
        Client client = null;
        try {
            client = new RpcClient(TEST_SERVER_HOST, 19876, TEST_CLIENT_ID, QUICK_RETRY, getProtocolClientFactory(new File(getServerPublicKeyLocation()).toURI().toURL()));
            client.postEvents(TEST_EVENTS, null).get(5, TimeUnit.SECONDS);
            fail("Expect exception thrown, but no exception found!");
        } catch (TimeoutException | ExecutionException e) {
            //expected;
            System.out.println("Fret not! Expected ^^");
        } finally {
            if (client != null) {
                client.close();
            }
        }
    }

    @Test
    public void testConnectLazyServer() throws Exception {
    	int lazyServerPort = locateAvailablePort();

        //TServer lazyServer = startTestCollector(tryLaterPort, tryLaterHandler); //DO NOT START SERVER YET
        TestCollector lazyServer = null;
        Client client = null;

        try {
            client = new RpcClient(TEST_SERVER_HOST, lazyServerPort, TEST_CLIENT_ID, getProtocolClientFactory(new File(getServerPublicKeyLocation()).toURI().toURL()));
            Future<Result> futureResult = client.postEvents(TEST_EVENTS, null);

            TimeUnit.SECONDS.sleep(5); //lazy!!
            lazyServer = startCollector(lazyServerPort); //now start the lazy server

            assertEquals(com.solarwinds.joboe.rpc.ResultCode.OK, futureResult.get().getResultCode());
            assertEventEquals(TEST_EVENTS, lazyServer.flush());


        } finally {
            if (client != null) {
                client.close();
            }
            lazyServer.stop();
        }
    }

    @Test
    public void testPostManyEvents() throws Exception {
        System.out.println("running testPostManyEvents");
        Client client = null;

        try {
            client = new RpcClient(TEST_SERVER_HOST, testServerPort, TEST_CLIENT_ID, getProtocolClientFactory(new File(getServerPublicKeyLocation()).toURI().toURL()));

            List<Event> events = new ArrayList<Event>();

            for (int i = 0 ; i < 1000; i ++) {
                events.addAll(TEST_EVENTS);
            }

            assertEquals(com.solarwinds.joboe.rpc.ResultCode.OK, client.postEvents(events, null).get().getResultCode());
            assertEventEquals(events, testCollector.flush());
        } finally {
            if (client != null) {
                client.close();
            }
        }
    }

    @Test
    public void testPostManyBigEvents() throws Exception {
        System.out.println("running testPostManyBigEvents");
        Client client = null;

        try {
            client = new RpcClient(TEST_SERVER_HOST, testServerPort, TEST_CLIENT_ID, getProtocolClientFactory(new File(getServerPublicKeyLocation()).toURI().toURL()));

            List<Event> events = new ArrayList<Event>();

            int eventCount = 1000;
            long totalSize = (long) eventCount * BIG_EVENT.toBytes().length; //just an approximation

            for (int i = 0 ; i < eventCount; i ++) { //1000 events, each event is around 100kb
                events.add(BIG_EVENT);
            }

            assertEquals(com.solarwinds.joboe.rpc.ResultCode.OK, client.postEvents(events, null).get().getResultCode());
            assertEventEquals(events, testCollector.flush());

            Map<TaskType, Long> callCountStats = testCollector.getCallCountStats();
            long minimumCallCount = totalSize / ProtocolClient.MAX_CALL_SIZE ; //just an approximation
            assert(callCountStats.get(TaskType.POST_EVENTS) >= minimumCallCount); //should be more than 10 calls for post Events due to the split up
        } finally {
            if (client != null) {
                client.close();
            }
        }
    }

    @Test
    public void testGetSettings() throws Exception  {
        System.out.println("running testGetSettings");
        Client client = null;

        try {
            client = new RpcClient(TEST_SERVER_HOST, testServerPort, TEST_CLIENT_ID, getProtocolClientFactory(new File(getServerPublicKeyLocation()).toURI().toURL()));

            com.solarwinds.joboe.rpc.SettingsResult result = client.getSettings("", null).get();
            assertEquals(com.solarwinds.joboe.rpc.ResultCode.OK, result.getResultCode());

            assertEquals(TEST_SETTINGS.size(), result.getSettings().size());

            for (int i = 0 ; i < TEST_SETTINGS.size(); i ++) {
                Settings expectedSetting = TEST_SETTINGS.get(i);
                com.solarwinds.joboe.settings.Settings receivedSetting = result.getSettings().get(i);
                assertEquals(expectedSetting.getType(), receivedSetting.getType());

                short expectedFlags = expectedSetting.getFlags();
                assertEquals(expectedFlags, receivedSetting.getFlags());
                assertEquals(expectedSetting.getLayer(), receivedSetting.getLayer());
                //assertEquals(expectedSetting.getTimestamp(), receivedSetting.getTimestamp()); timestamp for now we will use the machine's current timestamp instead of the incoming one as TTL is tricky (otherwise we would have to keep 2 timestamps...)
                assertEquals(expectedSetting.getValue(), receivedSetting.getValue());
                assertEquals(expectedSetting.getArgValue(SettingsArg.BUCKET_CAPACITY), receivedSetting.getArgValue(SettingsArg.BUCKET_CAPACITY), 0);
                assertEquals(expectedSetting.getArgValue(SettingsArg.BUCKET_RATE), receivedSetting.getArgValue(SettingsArg.BUCKET_RATE), 0);
            }
        } finally {
            if (client != null) {
                client.close();
            }
        }
    }

    @Test
    public void testGetSettingsDeadServer() throws Exception  {
        System.out.println("running testGetSettingsDeadServer");
        Client client = null;
        try {
            client = new RpcClient(TEST_SERVER_HOST, 19876, TEST_CLIENT_ID, getProtocolClientFactory(new File(getServerPublicKeyLocation()).toURI().toURL()));
            client.getSettings("", null).get(5, TimeUnit.SECONDS);
            fail("Expect exception thrown, but no exception found!");
        } catch (TimeoutException | ExecutionException e) {
            //expected;
            System.out.println("Fret not! Expected ^^");
        } finally {
            if (client != null) {
                client.close();
            }
        }
    }

    /**
     * With non empty warning
     * @throws Exception
     */
    @Test
    public void testGetSettingSoftDisabled() throws Exception  {
        System.out.println("running testGetSettings with warning (soft-disabled)");
        Client client = null;
        int softDisabledServerPort = locateAvailablePort();

        String warning = "Test warning";
        TestCollector softDisabledServer = startSoftDisabledTestCollector(softDisabledServerPort, warning);
        try {
            client = new RpcClient(TEST_SERVER_HOST, softDisabledServerPort, TEST_CLIENT_ID, getProtocolClientFactory(new File(getServerPublicKeyLocation()).toURI().toURL()));

            com.solarwinds.joboe.rpc.SettingsResult result = client.getSettings("", null).get();
            assertEquals(com.solarwinds.joboe.rpc.ResultCode.OK, result.getResultCode());
            assertEquals(warning, result.getWarning());
        } finally {
            if (client != null) {
                client.close();
            }
        }
    }

    @Test
    public void testPostStatus() throws Exception {
        System.out.println("running testPostStatus");
        Client client = null;

        try {
            client = new RpcClient(TEST_SERVER_HOST, testServerPort, TEST_CLIENT_ID, getProtocolClientFactory(new File(getServerPublicKeyLocation()).toURI().toURL()));

            Map<String, Object> testMessage = new HashMap<String, Object>();
            testMessage.put("SomeString", "123");
            testMessage.put("SomeInteger", 456);
            testMessage.put("SomeDouble", 0.789);
            testMessage.put("SomeBoolean", true);

            Map<String, Object> subMap = new HashMap<String, Object>(testMessage);
            testMessage.put("SomeMap", subMap);

            Result result = client.postStatus(Collections.singletonList(testMessage), null).get();
            assertEquals(com.solarwinds.joboe.rpc.ResultCode.OK, result.getResultCode());

            List<ByteBuffer> receivedMessages = testCollector.flush();
            assertEquals(1, receivedMessages.size());

            Set<Entry<String, Object>> recievedEntries = getEntriesFromBytes(receivedMessages.get(0).array());
            assertEquals(testMessage.entrySet(), recievedEntries);
        } finally {
            if (client != null) {
                client.close();
            }
        }
    }


    @Test
    public void testPostStatusBigMessage() throws Exception {
        System.out.println("running testPostStatusBigMessage");
        Client client = null;
        final int ENTRY_COUNT = 500;
        final int ENTRY_SIZE = 1024;
        try {
            client = new RpcClient(TEST_SERVER_HOST, testServerPort, TEST_CLIENT_ID, getProtocolClientFactory(new File(getServerPublicKeyLocation()).toURI().toURL()));

            List<Map<String, Object>> testMessages = new ArrayList<Map<String,Object>>();
            Map<String, Object> testMessage = new HashMap<String, Object>();
            for (int j = 0 ; j < ENTRY_COUNT; j ++) {
                testMessage.put(String.valueOf(j), new Byte[ENTRY_SIZE]);
            }
            testMessages.add(testMessage);

            client.postStatus(testMessages, null).get(); //big but it's within the max size defined in ThriftClient MAX_MESSAGE_SIZE
        } finally {
            if (client != null) {
                client.close();
            }
        }
    }


    @Test
    public void testPostStatusHugeMessage() throws Exception {
        System.out.println("running testPostStatusHugeMessage");
        Client client = null;
        final int ENTRY_COUNT = 2000;
        final int ENTRY_SIZE = 1024;
        try {
            client = new RpcClient(TEST_SERVER_HOST, testServerPort, TEST_CLIENT_ID, getProtocolClientFactory(new File(getServerPublicKeyLocation()).toURI().toURL()));

            List<Map<String, Object>> testMessages = new ArrayList<Map<String,Object>>();
            Map<String, Object> testMessage = new HashMap<String, Object>();
            for (int j = 0 ; j < ENTRY_COUNT; j ++) {
                testMessage.put(String.valueOf(j), new Byte[ENTRY_SIZE]);
            }
            testMessages.add(testMessage);

            client.postStatus(testMessages, null).get(); //should throw exception
            fail("Expected " + ExecutionException.class.getName() + " but it was not thrown");
        } catch (ExecutionException e) {
            if (!(e.getCause() instanceof ClientFatalException)) {
                fail("Expected instance of " + ClientFatalException.class.getName() + " but found " + e.getCause().getClass().getName());
            }
            //expected;
            System.out.println("Fret not! Expected ^^");
        } finally {
            if (client != null) {
                client.close();
            }
        }
    }

    //TODO isn't postMetrics essentially the same as postStatus???

    @Test
    public void testPostMetrics() throws Exception {
        System.out.println("running testPostMetrics");
        Client client = null;

        try {
            client = new RpcClient(TEST_SERVER_HOST, testServerPort, TEST_CLIENT_ID, getProtocolClientFactory(new File(getServerPublicKeyLocation()).toURI().toURL()));

            Map<String, Object> testMessage = new HashMap<String, Object>();
            testMessage.put("SomeString", "123");
            testMessage.put("SomeInteger", 456);
            testMessage.put("SomeDouble", 0.789);
            testMessage.put("SomeBoolean", true);

            Map<String, Object> subMap = new HashMap<String, Object>(testMessage);
            testMessage.put("SomeMap", subMap);

            Result result = client.postMetrics(Collections.singletonList(testMessage), null).get();
            assertEquals(com.solarwinds.joboe.rpc.ResultCode.OK, result.getResultCode());

            List<ByteBuffer> receivedMessages = testCollector.flush();
            assertEquals(1, receivedMessages.size());

            Set<Entry<String, Object>> recievedEntries = getEntriesFromBytes(receivedMessages.get(0).array());
            assertEquals(testMessage.entrySet(), recievedEntries);
        } finally {
            if (client != null) {
                client.close();
            }

        }
    }

    @Test
    public void testConnectionInit() throws Exception {
        System.out.println("running testConnectionInit");
        Client client = null;

        try {
            client = new RpcClient(TEST_SERVER_HOST, testServerPort, TEST_CLIENT_ID, getProtocolClientFactory(new File(getServerPublicKeyLocation()).toURI().toURL()));

            Map<String, Object> testMessage = new HashMap<String, Object>();
            Result result = client.postMetrics(Collections.singletonList(testMessage), null).get(); //post something to ensure connection init is triggered and sent
            assertEquals(com.solarwinds.joboe.rpc.ResultCode.OK, result.getResultCode());

            List<ByteBuffer> receivedMessages = testCollector.flush();
            assertEquals(1, receivedMessages.size());

        } finally {
            if (client != null) {
                client.close();
            }
        }
    }


    @Test
    public void testRedirectLoop() throws Exception {
        System.out.println("running testRedirectLoop");
        int redirectPort = locateAvailablePort();
        TestCollector redirectServer = startRedirectCollector(redirectPort, "localhost:" + redirectPort); //redirect loop

        Client client = null;

        try {
            client = new RpcClient(TEST_SERVER_HOST, redirectPort, TEST_CLIENT_ID, getProtocolClientFactory(new File(getServerPublicKeyLocation()).toURI().toURL()));

            assertEquals(com.solarwinds.joboe.rpc.ResultCode.REDIRECT, client.postEvents(TEST_EVENTS, null).get().getResultCode());
        } finally {
            if (client != null) {
                client.close();
            }
            redirectServer.stop();
        }
    }

    @Test
    public void testValidRedirect() throws Exception {
        System.out.println("running testValidRedirect");
        int redirectPort1 = locateAvailablePort();
        int redirectPort2 = redirectPort1 + 1;
        TestCollector redirectServer1 = startRedirectCollector(redirectPort1, "localhost:" + redirectPort2);
        TestCollector redirectServer2 = startRedirectCollector(redirectPort2, "localhost:" + testServerPort);//redirect back to correct server


        Client client = null;

        try {
            client = new RpcClient(TEST_SERVER_HOST, redirectPort1, TEST_CLIENT_ID, getProtocolClientFactory(new File(getServerPublicKeyLocation()).toURI().toURL()));

            assertEquals(com.solarwinds.joboe.rpc.ResultCode.OK, client.postEvents(TEST_EVENTS, null).get().getResultCode());
            assertEventEquals(TEST_EVENTS, testCollector.flush());
        } finally {
            if (client != null) {
                client.close();
            }
            redirectServer1.stop();
            redirectServer2.stop();
        }

    }

    @Test
    public void testInvalidRedirectArg() throws Exception {
        System.out.println("running testInvalidRedirectArg");
        int redirectPort = locateAvailablePort();
        TestCollector redirectServer = startRedirectCollector(redirectPort, "http://localhost:" + redirectPort); //invalid redirect arg format
        Client client = null;

        try {
            client = new RpcClient(TEST_SERVER_HOST, redirectPort, TEST_CLIENT_ID, getProtocolClientFactory(new File(getServerPublicKeyLocation()).toURI().toURL()));
            client.postEvents(TEST_EVENTS, null).get();
            fail("Expect exception thrown, but no exception found!");
        } catch (ExecutionException e) {
           //expected
            System.out.println("Fret not! Expected ^^");
        } finally {
            if (client != null) {
                client.close();
            }
            redirectServer.stop();
        }
    }

    @Test
    public void testInvalidRedirectTarget() throws Exception {
        System.out.println("running testInvalidRedirectTarget");
        int redirectPort = locateAvailablePort();
        TestCollector redirectServer = startRedirectCollector(redirectPort, "unknown-host-aieeeeeeee:" + redirectPort); //invalid redirect arg format
        Client client = null;

        try {
            client = new RpcClient(TEST_SERVER_HOST, redirectPort, TEST_CLIENT_ID, QUICK_RETRY, getProtocolClientFactory(new File(getServerPublicKeyLocation()).toURI().toURL()));

            client.postEvents(TEST_EVENTS, null).get(5, TimeUnit.SECONDS);

            fail("Expect exception thrown, but no exception found!");
        } catch (TimeoutException | ExecutionException e) {
            //expected;
            System.out.println("Fret not! Expected ^^");
        } finally {
            if (client != null) {
                client.close();
            }
            redirectServer.stop();
        }
    }

    /**
     * Tests against thrift server that processes on certain speed, once exceeded it will return try later.
     *
     * This verifies the retry mechanism of this client
     * @throws Exception
     */
    @Test
    public void testTryLater() throws Exception {
        System.out.println("running testTryLater");
        final int TIME_PER_EVENT = 10;
        int tryLaterPort = locateAvailablePort();

        TestCollector tryLaterCollector = startRatedCollector(tryLaterPort, TIME_PER_EVENT, ResultCode.TRY_LATER);
        Client client = null;

        try {
            client = new RpcClient(TEST_SERVER_HOST, tryLaterPort, TEST_CLIENT_ID, getProtocolClientFactory(new File(getServerPublicKeyLocation()).toURI().toURL()));

            List<Future<Result>> futures = new ArrayList<Future<Result>>();
            List<Event> sentEvents = new ArrayList<Event>();
            for (int i = 0 ; i < 10; i ++) {
                 futures.add(client.postEvents(TEST_EVENTS, null)); //post events, the handler will return TRY_LATER most of the time, but the client should be able to retry the request until it's OK eventually
                 sentEvents.addAll(TEST_EVENTS);
            }

            for (Future<Result> future : futures) {
                assertEquals(com.solarwinds.joboe.rpc.ResultCode.OK, future.get().getResultCode());
            }

             assertEventEquals(sentEvents, tryLaterCollector.flush());
        } finally {
            if (client != null) {
                client.close();
            }
            tryLaterCollector.stop();
        }
    }

    /**
     * Tests against thrift server that processes on certain speed, once exceeded it will return limit exceed
     *
     * This verifies the retry mechanism of this client
     * @throws Exception
     */
    @Test
    public void testLimitExceed() throws Exception {
        System.out.println("running testLimitExceed");
        final int TIME_PER_EVENT = 10;
        int tryLaterPort = locateAvailablePort();

        TestCollector tryLaterServer = startRatedCollector(tryLaterPort, TIME_PER_EVENT, ResultCode.LIMIT_EXCEEDED);
        Client client = null;

        try {
            client = new RpcClient(TEST_SERVER_HOST, tryLaterPort, TEST_CLIENT_ID, getProtocolClientFactory(new File(getServerPublicKeyLocation()).toURI().toURL()));

            List<Future<Result>> futures = new ArrayList<Future<Result>>();
            List<Event> sentEvents = new ArrayList<Event>();
            for (int i = 0 ; i < 10; i ++) {
                 futures.add(client.postEvents(TEST_EVENTS, null)); //post events, the handler will return LIMIT_EXCEEDED most of the time, but the client should be able to retry the request until it's OK eventually
                 sentEvents.addAll(TEST_EVENTS);
            }

            for (Future<Result> future : futures) {
                assertEquals(com.solarwinds.joboe.rpc.ResultCode.OK, future.get().getResultCode());
            }

             assertEventEquals(sentEvents, tryLaterServer.flush());
        } finally {
            if (client != null) {
                client.close();
            }
            tryLaterServer.stop();
        }
    }

    @Test
    public void testUnstableServer() throws Exception {
        System.out.println("running testUnstableServer");
        final int unstableServerPort = locateAvailablePort();

        final AtomicBoolean keepRunning = new AtomicBoolean(true);

        final List<ByteBuffer> collectorEvents = new ArrayList<ByteBuffer>();

        Thread serverThread = new Thread() {
            public void run() {
                while (keepRunning.get()) {
                    try {
                        while (!isPortAvailable(unstableServerPort)) {
                            System.out.println("unstable server port [" + unstableServerPort + "] is not yet available... sleeping");
                            TimeUnit.SECONDS.sleep(1);
                        }

                        TestCollector unstableServer = startCollector(unstableServerPort);
                        System.out.println("unstable server serving for 1 sec...");
                        Thread.sleep(1000);
                        System.out.println("unstable server coming down...");
                        List<ByteBuffer> receivedEvents = unstableServer.stop();
                        collectorEvents.addAll(receivedEvents);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        };

        serverThread.start();

        Client client = null;

        try {
            client = new RpcClient(TEST_SERVER_HOST, unstableServerPort, TEST_CLIENT_ID, getProtocolClientFactory(new File(getServerPublicKeyLocation()).toURI().toURL()));

            List<Future<Result>> futures = new ArrayList<Future<Result>>();
            List<Event> sentEvents = new ArrayList<Event>();
            for (int i = 0 ; i < 100; i ++) {
                Context.getMetadata().randomize();
                Event entryEvent = Context.createEvent();
                entryEvent.addInfo("Layer", "test-" + i);
                entryEvent.addInfo("Label", "info");

                futures.add(client.postEvents(Collections.singletonList(entryEvent), null)); //post alot of events, and it is supposed to retry if connection is gone intermittedly
                //futures.add(client.postEvents(TEST_EVENTS, null)); //post alot of events, and it is supposed to retry if connection is gone intermittedly
                Thread.sleep(100); //some sleep in between so the the post might hit the server downtime
                //sentEvents.addAll(TEST_EVENTS);
                sentEvents.add(entryEvent);
            }

            for (Future<Result> future : futures) {
                assertEquals(com.solarwinds.joboe.rpc.ResultCode.OK, future.get().getResultCode());
            }

            keepRunning.set(false);
            serverThread.join(); //wait for the server thread to complete (so server is stopped properly)

            assertEventEquals(sentEvents, collectorEvents);
        } finally {
            if (client != null) {
                client.close();
            }



        }
    }

    @Test
    public void testOccasionalErrorServer() throws Exception {
        System.out.println("running testOccasionalErrorServer");
        int errorServerPort = locateAvailablePort();

        TestCollector errorServer = startErroneousTestCollector(errorServerPort, 0.5); //half of them run into exception
        Client client = null;

        try {
            client = new RpcClient(TEST_SERVER_HOST, errorServerPort, TEST_CLIENT_ID, getProtocolClientFactory(new File(getServerPublicKeyLocation()).toURI().toURL()));

            List<Future<Result>> futures = new ArrayList<Future<Result>>();
            List<Event> sentEvents = new ArrayList<Event>();
            for (int i = 0 ; i < 10; i ++) {
                futures.add(client.postEvents(TEST_EVENTS, null)); //post events, the handler will return LIMIT_EXCEEDED most of the time, but the client should be able to retry the request until it's OK eventually
                sentEvents.addAll(TEST_EVENTS);
            }


            //TODO
            for (Future<Result> future : futures) { //should all be OK eventually after retry
                assertEquals(com.solarwinds.joboe.rpc.ResultCode.OK, future.get().getResultCode());
            }

            assertEventEquals(sentEvents, errorServer.flush());
        } finally {
            if (client != null) {
                client.close();
            }
            errorServer.stop();
        }
    }

    @Test
    public void testErrorServer() throws Exception {
        System.out.println("running testErrorServer");
        int errorServerPort = locateAvailablePort();

        TestCollector errorServer = startErroneousTestCollector(errorServerPort, 1); //always return error
        Client client = null;

        try {
            client = new RpcClient(TEST_SERVER_HOST, errorServerPort, TEST_CLIENT_ID, QUICK_RETRY, getProtocolClientFactory(new File(getServerPublicKeyLocation()).toURI().toURL()));

            List<Future<Result>> futures = new ArrayList<Future<Result>>();
            List<Event> sentEvents = new ArrayList<Event>();
            Result result = client.postEvents(TEST_EVENTS, null).get();//will fail and give up base on QUICK_RETRY

            fail("Expect exception because of retry failures, but it's not thrown!");
        } catch (ExecutionException e) {
            if (!(e.getCause() instanceof ClientException)) {
                fail("Expect ClientException because of retry failures, but found " + e.getCause());
            }
            //expected
        } finally {
            if (client != null) {
                client.close();
            }
            errorServer.stop();
        }
    }

    /**
     * Test different calls processing should block each other based on different status code. For example a server might return
     * TRY_LATER for postMetrics but get settings calls might be OK, therefore the getSettings calls should not get held up by
     * postMetrics's failure
     * @throws Exception
     */
    @Test
    public void testBiasedServer() throws Exception {
        System.out.println("running testBiasedServer");
        int biasedServerPort = locateAvailablePort();

        TestCollector basiedServer = startBiasedTestCollector(biasedServerPort, Collections.singletonMap(TaskType.POST_METRICS, ResultCode.TRY_LATER));
        Client client = null;

        try {
            client = new RpcClient(TEST_SERVER_HOST, biasedServerPort, TEST_CLIENT_ID, getProtocolClientFactory(new File(getServerPublicKeyLocation()).toURI().toURL()));

            List<Future<Result>> futures = new ArrayList<Future<Result>>();

            try {
                Result result = client.postMetrics(new ArrayList<Map<String,Object>>(), null).get(5, TimeUnit.SECONDS); //this is supposed to get held up because of TRY_LAYER
                fail("Not expecting to return any result for this call!");
            } catch (TimeoutException e) {
                //expected
            }

            assertEquals(com.solarwinds.joboe.rpc.ResultCode.OK, client.getSettings("", null).get().getResultCode()); //this should be successful

        } finally {
            if (client != null) {
                client.close();
            }
            basiedServer.stop();
        }

    }



    protected void assertEventEquals(List<Event> testEvents, List<ByteBuffer> receivedMessages) throws BsonBufferException {
        assertEquals(testEvents.size(), receivedMessages.size());
        for (int i = 0; i < testEvents.size() ; i++ ) {
            assertEquals(testEvents.get(i).toByteBuffer(), receivedMessages.get(i));
        }

    }

    protected static Set<Entry<String, Object>> getEntriesFromBytes(byte[] bytes) {
        return getBsonDocumentFromBytes(bytes).entrySet();
    }

    protected static BsonDocument getBsonDocumentFromBytes(byte[] bytes) {
        ByteBuffer buffer =  ByteBuffer.allocate(Constants.MAX_EVENT_BUFFER_SIZE).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(bytes);
        buffer.flip();

        return BsonDocuments.readFrom(buffer);
    }

    /**
     * Checks to see if a specific port is available.
     *
     */
    public static synchronized int locateAvailablePort() {
//        ServerSocket ss = null;
//        DatagramSocket ds = null;
//        int MAX_PORT = portWalker + 2000; //huh shouldnt hit it
//        while (portWalker <= MAX_PORT) {
//            try {
//                ss = new ServerSocket(portWalker);
//                ss.setReuseAddress(true);
//                ds = new DatagramSocket(portWalker);
//                ds.setReuseAddress(true);
//                return portWalker;
//            } catch (IOException e) {
//                portWalker ++;
//            } finally {
//                if (ds != null) {
//                    ds.close();
//                }
//
//                if (ss != null) {
//                    try {
//                        ss.close();
//                    } catch (IOException e) {
//                        /* should not be thrown */
//                    }
//                }
//                portWalker ++;
//            }
//        }

        int MAX_PORT = portWalker + 2000; //huh shouldn't hit it
        while (portWalker <= MAX_PORT) {
            if (!isPortAvailable(portWalker)) {
                portWalker ++;
            } else {
                return portWalker;
            }
        }

        return 0;
    }

    private static boolean isPortAvailable(int port) {
        Socket s = new Socket();
        try {
            s.connect(new InetSocketAddress("localhost", portWalker), 100);
            //that means the port is occupied
            return false;
        } catch (IOException e) {
            //that means port is available
            return true;
        } finally {
            if( s != null){
                try {
                    s.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    protected static class ErrorState {
        private final double errorPercentage;
        private double sum = 0.0;

        public ErrorState(double errorPercentage) {
            this.errorPercentage = errorPercentage;
        }

        public boolean isNextAsError() {
            sum += errorPercentage;
            boolean isError = sum >= 1.0;
            if (isError) {
                sum = sum - (int)sum;
            }
            return isError;
        }
    }
}
