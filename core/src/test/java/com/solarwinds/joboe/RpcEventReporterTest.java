package com.solarwinds.joboe;

import com.solarwinds.joboe.rpc.Client;
import com.solarwinds.joboe.rpc.ResultCode;
import com.solarwinds.joboe.settings.SettingsArg;
import com.solarwinds.joboe.settings.SettingsManager;
import com.solarwinds.joboe.settings.SimpleSettingsFetcher;
import com.solarwinds.joboe.settings.TestSettingsReader;
import com.solarwinds.joboe.settings.TestSettingsReader.SettingsMockupBuilder;
import com.solarwinds.util.TestUtils;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RpcEventReporterTest {
    private final TestSettingsReader testSettingsReader = TestUtils.initSettingsReader();

    @Test
    public void testQuickClient() throws Exception {
        TestRpcClient rpcClient = new TestRpcClient(0);
        RpcEventReporter rpcReporter = new RpcEventReporter(rpcClient);
        
        Event event = Context.createEvent();
        
        int eventCount = QueuingEventReporter.QUEUE_CAPACITY;
        List<Event> sentEvents = new ArrayList<Event>();
        
        for (int i = 0 ; i < eventCount; i ++) {
            rpcReporter.send(event);
            sentEvents.add(event);
        }
        
        TimeUnit.SECONDS.sleep(QueuingEventReporter.DEFAULT_FLUSH_INTERVAL + 1);        
        
        assertEquals(sentEvents.size(), rpcClient.getPostedEvents().size());
        assertEquals(sentEvents, rpcClient.getPostedEvents());
        
        for (int i = 0 ; i < eventCount; i ++) { //sending more, and it should be ok
            rpcReporter.send(event);
        }
        
        TimeUnit.SECONDS.sleep(QueuingEventReporter.DEFAULT_FLUSH_INTERVAL + 1);
        assertStats(rpcReporter, (long)eventCount * 2, false, 0, eventCount * 2, null);
    }


    @Test
    public void testSlowClient() throws Exception {
        TestRpcClient rpcClient;
        RpcEventReporter rpcReporter;
        Event event = Context.createEvent();
        int eventCount = QueuingEventReporter.QUEUE_CAPACITY;
        
        List<Event> sentEvents = new ArrayList<Event>();
        rpcClient = new TestRpcClient(10);
        rpcReporter = new RpcEventReporter(rpcClient);
        
        for (int i = 0 ; i < eventCount; i ++) {
            rpcReporter.send(event);
            sentEvents.add(event);
        }
        
        Thread.sleep(5000); //even for slower client, it should take around (eventCount / SEND_CAPACITY * delay) ms to finish, SEND_CAPACITY is 1000 for now 
        assertEquals(sentEvents, rpcClient.getPostedEvents());
        
        for (int i = 0 ; i < eventCount; i ++) { //sending more, and it should be ok
            rpcReporter.send(event);
        }
        
        Thread.sleep(5000);
        assertStats(rpcReporter, (long)eventCount * 2, false, 0, eventCount * 2, null);
    }

    @Test
    public void testDeadSlowClient() throws Exception {
        TestRpcClient rpcClient;
        RpcEventReporter rpcReporter;
        Event event = Context.createEvent();
        int eventCount = QueuingEventReporter.QUEUE_CAPACITY;
        
        
        rpcClient = new TestRpcClient(1000);
        rpcReporter = new RpcEventReporter(rpcClient);
        
        for (int i = 0 ; i < eventCount; i ++) {
            rpcReporter.send(event);
        }
        
        //should not be enough time to finish all requests 
        
        boolean hasRejection = false;
        for (int i = 0 ; i < eventCount; i ++) { //sending more, and it should start rejecting eventually
            try {
                rpcReporter.send(event);
            } catch (EventReporterQueueFullException e) {
                //expected for some of the send calls
                hasRejection = true;
            }
        }
        assertEquals(true, hasRejection);
        
        Thread.sleep(20000);
        assertStats(rpcReporter, null, true, 0, eventCount * 2, (long)QueuingEventReporter.QUEUE_CAPACITY);
    }
    
    /**
     * RPC Client that throws exceptions upon task execution
     * @throws Exception
     */
    @Test
    public void testExecutionExceptionClient() throws Exception {
        Client rpcClient = new TestExecutionExceptionRpcClient();
        RpcEventReporter rpcReporter = new RpcEventReporter(rpcClient);
        Event event = Context.createEvent();
        
        int eventCount = QueuingEventReporter.QUEUE_CAPACITY;
        
        for (int i = 0 ; i < eventCount; i ++) {
            rpcReporter.send(event); //take note that this should still be true as the event was properly queued within the QueuingEventReporter even tho it throws exception during execution in the rpc client
        }

        TimeUnit.SECONDS.sleep(QueuingEventReporter.DEFAULT_FLUSH_INTERVAL + 1);
        assertStats(rpcReporter, 0l, false, eventCount, eventCount, null);
    }
    
    /**
     * RPC Client that throws exceptions upon task submission
     * @throws Exception 
     */
    @Test
    public void testSubmitExceptionClient() throws Exception {
        Client rpcClient = new TestSubmitRejectionRpcClient();
        RpcEventReporter rpcReporter = new RpcEventReporter(rpcClient);
        Event event = Context.createEvent();
        
        int eventCount = QueuingEventReporter.QUEUE_CAPACITY;
        
        for (int i = 0 ; i < eventCount; i ++) {
            rpcReporter.send(event); //take note that this should still be true as the event was properly queued within the QueuingEventReporter even tho it throws exception when submitted to the rpc client
        }
        
        TimeUnit.SECONDS.sleep(QueuingEventReporter.DEFAULT_FLUSH_INTERVAL + 1);
        assertStats(rpcReporter, 0l, false, eventCount, eventCount, null); //rejection from the client does not count as overflow - overflow refers to the queue within the QueuingEventReporter
    }


    @Test
    public void testInvalidApiKeyClient() throws Exception {
        Client rpcClient = new TestRpcClient(0, ResultCode.INVALID_API_KEY);
        
        RpcEventReporter rpcReporter = new RpcEventReporter(rpcClient);
        Event event = Context.createEvent();
        
        int eventCount = QueuingEventReporter.QUEUE_CAPACITY;
        
        for (int i = 0 ; i < eventCount; i ++) {
            rpcReporter.send(event); //should still accept it but warning will be print to console once
        }
    }

    @Test
    public void testLimitExceedClient() throws Exception {
        Client rpcClient = new TestRpcClient(0, ResultCode.LIMIT_EXCEEDED);
        
        RpcEventReporter rpcReporter = new RpcEventReporter(rpcClient);
        Event event = Context.createEvent();
        
        int eventCount = QueuingEventReporter.QUEUE_CAPACITY;
        
        for (int i = 0 ; i < eventCount; i ++) {
            rpcReporter.send(event); //should still accept it but warning will be print to console once
        }
    }


    @Test
    public void testFlushInterval() throws Exception {
        TestRpcClient rpcClient = new TestRpcClient(0);
        
        //test default
        RpcEventReporter rpcReporter = new RpcEventReporter(rpcClient);
        
        Event event = Context.createEvent();
        
        int eventCount = QueuingEventReporter.SEND_CAPACITY - 1;
        List<Event> sentEvents = new ArrayList<Event>();
        
        for (int i = 0 ; i < eventCount; i ++) {
            rpcReporter.send(event);
            sentEvents.add(event);
        }
        
        TimeUnit.SECONDS.sleep(QueuingEventReporter.DEFAULT_FLUSH_INTERVAL - 1);        
        assertEquals(0, rpcClient.getPostedEvents().size());
        TimeUnit.SECONDS.sleep(2); //sleep for another 2 seconds, should all arrive now
        assertEquals(sentEvents.size(), rpcClient.getPostedEvents().size());
        rpcClient.reset();
        
        //test explicit flush interval
        QueuingEventReporter.setFlushInterval(4);
        for (int i = 0 ; i < eventCount; i ++) { //sending more, and it should be ok
            rpcReporter.send(event);
        }
        
        TimeUnit.SECONDS.sleep(3); //no events yet        
        assertEquals(0, rpcClient.getPostedEvents().size());
        TimeUnit.SECONDS.sleep(2); //sleep for another 2 seconds, should all arrive now
        assertEquals(sentEvents.size(), rpcClient.getPostedEvents().size());
        rpcClient.reset();
        
        
        //simulate a flush interval change
        SimpleSettingsFetcher fetcher = (SimpleSettingsFetcher) SettingsManager.getFetcher();
        testSettingsReader.put(new SettingsMockupBuilder().withFlags(TracingMode.ALWAYS).withSampleRate(TraceDecisionUtil.SAMPLE_RESOLUTION).withSettingsArg(SettingsArg.EVENTS_FLUSH_INTERVAL, 3).build());  //every 3 secs
        
        for (int i = 0 ; i < eventCount; i ++) { //sending more, and it should be ok
            rpcReporter.send(event);
        }
        
        TimeUnit.SECONDS.sleep(2); //no events yet        
        assertEquals(0, rpcClient.getPostedEvents().size());
        TimeUnit.SECONDS.sleep(2); //sleep for another 2 seconds, should all arrive now
        assertEquals(sentEvents.size(), rpcClient.getPostedEvents().size());
        rpcClient.reset();
        
        
        QueuingEventReporter.flushInterval = QueuingEventReporter.DEFAULT_FLUSH_INTERVAL; //reset
    }

    @Test
    public void testSendEventNow() throws Exception {
        TestRpcClient rpcClient = new TestRpcClient(0);
        
        //test default
        RpcEventReporter rpcReporter = new RpcEventReporter(rpcClient);
        
        //simulate a flush interval change
        SimpleSettingsFetcher fetcher = (SimpleSettingsFetcher) SettingsManager.getFetcher();
        testSettingsReader.put(new SettingsMockupBuilder().withFlags(TracingMode.ALWAYS).withSampleRate(TraceDecisionUtil.SAMPLE_RESOLUTION).withSettingsArg(SettingsArg.EVENTS_FLUSH_INTERVAL, 10).build());  //long flush interval
        
        Event event = Context.createEvent();
        
        int eventCount = QueuingEventReporter.SEND_CAPACITY - 1;
        
        for (int i = 0 ; i < eventCount; i ++) {
            rpcReporter.send(event);
        }
        
        TimeUnit.SECONDS.sleep(3); //no events yet as it's at SEND_CAPACITY - 1      
        assertEquals(0, rpcClient.getPostedEvents().size());
        for (int i = 0 ; i < QueuingEventReporter.SEND_CAPACITY; i ++) { //sending more, take note that we need more than 1 event as the first few events could have been drained off the queue before the sleep 
            rpcReporter.send(event); //now it should flush as it reaches SEND_CAPACITY
        }
        TimeUnit.SECONDS.sleep(1); //sleep for another second, should get the first batch now
        assertEquals(QueuingEventReporter.SEND_CAPACITY, rpcClient.getPostedEvents().size());
        rpcClient.reset();
        
        QueuingEventReporter.flushInterval = QueuingEventReporter.DEFAULT_FLUSH_INTERVAL; //reset
    }

    @Test
    private void assertStats(RpcEventReporter rpcReporter, Long sentCount, boolean hasOverflowCount, long failedCount, long totalCount, Long queueLargest) throws InterruptedException {
        EventReporterStats stats = rpcReporter.consumeStats();
        
        if (sentCount != null) {
            assertEquals(sentCount.longValue(), stats.getSentCount());
        }
        assertEquals(hasOverflowCount, stats.getOverflowedCount() > 0);
        assertEquals(failedCount, stats.getFailedCount());
        assertEquals(totalCount, stats.getProcessedCount());
        
        if (queueLargest != null) {
            assertEquals(queueLargest.longValue(), stats.getQueueLargestCount());
        }
    }
}
