package com.tracelytics.joboe;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.tracelytics.joboe.config.ConfigManager;
import com.tracelytics.joboe.config.ConfigProperty;
import com.tracelytics.joboe.rpc.Result;
import com.tracelytics.joboe.rpc.ResultCode;
import com.tracelytics.joboe.settings.SettingsArg;
import com.tracelytics.joboe.settings.SettingsArgChangeListener;
import com.tracelytics.joboe.settings.SettingsManager;
import com.tracelytics.logging.Logger;
import com.tracelytics.logging.LoggerFactory;
import com.tracelytics.util.DaemonThreadFactory;

/**
 * A reporter that accepts events into a queue w/o blocking but sends out events synchronously. 
 * 
 * Accepts events are inserted into a queue and being consumed and sent out synchronously. 
 * If sending rate is slower than the queuing rate, then this report will attempt to send out events in batches
 * 
 * @author pluk
 *
 */
public abstract class QueuingEventReporter implements EventReporter {
    static final int QUEUE_CAPACITY = 10000;
    static final int SEND_CAPACITY = 1000;
    private BlockingQueue<Event> eventQueue = new LinkedBlockingQueue<Event>(QUEUE_CAPACITY);
    protected ExecutorService executorService = Executors.newSingleThreadExecutor(DaemonThreadFactory.newInstance("queuing-event-reporter"));
    
    
    protected static Logger logger = LoggerFactory.getLogger();
    private AtomicEventReporterStats stats = new AtomicEventReporterStats();
    
    private static final long REPORT_QUEUE_FULL_INTERVAL = 60 * 1000; //1 minute
    static final int DEFAULT_FLUSH_INTERVAL = 2; //2 second
    
    static int flushInterval = DEFAULT_FLUSH_INTERVAL; //in unit of second
    
    static {
        if (ConfigManager.getConfig(ConfigProperty.AGENT_EVENTS_FLUSH_INTERVAL) instanceof Integer) {
            setFlushInterval((Integer) ConfigManager.getConfig(ConfigProperty.AGENT_EVENTS_FLUSH_INTERVAL));
        }
        SettingsManager.registerListener(new SettingsArgChangeListener<Integer>(SettingsArg.EVENTS_FLUSH_INTERVAL) {
            @Override
            public void onChange(Integer newValue) {
                if (newValue != null) {
                    setFlushInterval(newValue);
                } else { //reset to default
                    setFlushInterval(DEFAULT_FLUSH_INTERVAL);
                }
            }
        });
    }
    
    private boolean reportedQueueFull = false;
    private long reportedQueueFullTime = 0;

    private final SendRunnable sendRunnable;
    
    static void setFlushInterval(int eventsFlushInterval) {
        if (eventsFlushInterval >= 0) {
            flushInterval = eventsFlushInterval;
            logger.debug("Event flush interval set to " + eventsFlushInterval + "s");
        } else {
            logger.warn("Event flush interval value " + eventsFlushInterval + " is not valid");
        }
    }
    
    protected QueuingEventReporter() {
        sendRunnable = new SendRunnable();
        executorService.execute(sendRunnable);
    }
    
    private class SendRunnable implements Runnable {
        private boolean exitSignalled = false;
        private CountDownLatch countDownLatch = new CountDownLatch(1);
        private ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1, DaemonThreadFactory.newInstance("send-event-delay"));
        public void run() {
            while (!exitSignalled || !eventQueue.isEmpty()) {
                List<Event> sendingEvents = new ArrayList<Event>();
                try {
                    sendingEvents.add(eventQueue.take()); //this blocks until at least one event is available; 
                    
                    eventQueue.drainTo(sendingEvents, SEND_CAPACITY - sendingEvents.size()); //drain the rest of the queue
                    
                    //sleep a while to batch up events, but only sleep if there's no build up, if the sendingEvents reaches capacity, we should send events right the way
                    if (sendingEvents.size() < SEND_CAPACITY) {
                        try {
                            waitForNextSend();
                        } catch (InterruptedException e) {
                            logger.debug("Queuing event wait interrupted");
                        }
                        eventQueue.drainTo(sendingEvents, SEND_CAPACITY - sendingEvents.size()); //try draining again
                    }
                    
                    Result result = synchronousSend(sendingEvents);
                    ResultCode resultCode = result.getResultCode();
                    
                    if (resultCode.isError()) {
                        logger.debug("Failed to send out " + sendingEvents.size() + " events");
                        stats.incrementFailedCount(sendingEvents.size());
                    } else {
                        stats.incrementSentCount(sendingEvents.size());
                    }
        
                } catch (Exception e) {
                    //do not retry the message, just log the problem
                    logger.debug("Failed to send " + sendingEvents.size() + " events, exception found: " + e.getMessage()); //Should not be too noisy
                    stats.incrementFailedCount(sendingEvents.size());
                } finally {
                    stats.incrementProcessedCount(sendingEvents.size());
                }
            }
            scheduledExecutorService.shutdownNow();
        }
        
        /**
         * Signals sending out events immediately 
         */
        protected void sendNow() {
            if (countDownLatch != null && countDownLatch.getCount() > 0) {
                logger.debug("SendNow signaled for event reporter");
                countDownLatch.countDown();
            }
        }
        
        /**
         * Blocks until the flushInterval elapsed or sendNow is signaled
         */
        private void waitForNextSend() throws InterruptedException {
            countDownLatch = new CountDownLatch(1);
            scheduledExecutorService.schedule(new Runnable() {
               @Override
                public void run() {
                   countDownLatch.countDown();
                }
            }, flushInterval, TimeUnit.SECONDS);
            
            countDownLatch.await();
        }
        
    }
    
    /**
     * Signals to sends an event via this reporter. Take note that the actual outbound request might be sent later on
     * 
     * @throws EventReporterException   if the reporter cannot accepts this event, for example the queue is full
     */
    public void send(Event event) throws EventReporterException {
        if (!eventQueue.offer(event)) {
            stats.incrementOverflowedCount(1);
            stats.incrementProcessedCount(1);
            
            if (!reportedQueueFull) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - reportedQueueFullTime >= REPORT_QUEUE_FULL_INTERVAL) { //at most once every minute
                    logger.warn("Fail to report tracing event as the event queue is full in the reporter"); //only print warning on first queue overflow
                    reportedQueueFull = true;
                    reportedQueueFullTime = currentTime;
                }
            }
            
            throw new EventReporterQueueFullException("Cannot send event as the reporter queue is full");
        } else {
            stats.setQueueCount(eventQueue.size());
            
            if (eventQueue.size() >= SEND_CAPACITY) { //should start sending early (if sleeping) as it's filling up
                sendRunnable.sendNow();
            }
            
            reportedQueueFull = false;
        }
    }
    
    /**
     * Gets and clears the current reporter stats
     * @return
     */
    public EventReporterStats consumeStats() {
        return stats.consumeStats();
    }
    
    
    /**
     * Closes this reporter orderly. Allow all submitted events to be processed with a default timeout
     */
    public void close() {
        logger.debug("Closing queueing event reporter, signaling shut down after sending all events");
        sendRunnable.exitSignalled = true;
        executorService.shutdown();
        try {
            executorService.awaitTermination(5, TimeUnit.SECONDS);
            logger.debug("Event reporter service shut down");
        } catch (InterruptedException e) {
            //ok, it's shutting down anyway
        }
        
    }
    
    /**
     * This should only returns upon completion of sending all the provided events (success or failure)
     * @param events
     * @return
     * @throws Exception 
     */
    public abstract Result synchronousSend(List<Event> events) throws Exception;

    /**
     * Stats on reporter
     * @author pluk
     *
     */
    private class AtomicEventReporterStats {
        private final AtomicLong sentCount = new AtomicLong();
        private final AtomicLong overflowedCount = new AtomicLong();
        private final AtomicLong failedCount = new AtomicLong();
        private final AtomicLong queueLargestCount = new AtomicLong();
        private final AtomicLong processedCount = new AtomicLong();
        
        public AtomicEventReporterStats() {
            super();
        }

        private void incrementSentCount(long increment) {
            sentCount.addAndGet(increment);
        }

        private void incrementOverflowedCount(long increment) {
            overflowedCount.addAndGet(increment);
        }

        private void incrementFailedCount(long increment) {
            failedCount.addAndGet(increment);
        }
        
        private void incrementProcessedCount(long increment) {
            processedCount.addAndGet(increment);
        }

        private void setQueueCount(long currentCount) {
            synchronized(queueLargestCount) {
                if (currentCount > queueLargestCount.get()) {
                    queueLargestCount.set(currentCount);
                }
            }
        }
        
        private EventReporterStats consumeStats() {
            long sentCount = this.sentCount.getAndSet(0);
            long overflowedCount = this.overflowedCount.getAndSet(0);
            long failedCount = this.failedCount.getAndSet(0);
            long queueLargestCount = this.queueLargestCount.getAndSet(eventQueue.size()); //reset to current queue size as the largest
            long processedCount = this.processedCount.getAndSet(0);
            return new EventReporterStats(sentCount, overflowedCount, failedCount, processedCount, queueLargestCount);
        }
    }
}
