package com.tracelytics.joboe.rpc;

import com.tracelytics.logging.LoggerFactory;
import com.tracelytics.util.DaemonThreadFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class KeepAliveMonitor implements HeartbeatScheduler {
    private final ScheduledExecutorService keepAliveService;
    private ScheduledFuture<?> keepAliveFuture;
    private final Runnable keepAliveRunnable;
    private static final long KEEP_ALIVE_INTERVAL = 20; //in seconds

    public KeepAliveMonitor(Supplier<ProtocolClient> protocolClient, String serviceKey, Object lock) {
        keepAliveService = Executors.newScheduledThreadPool(1, DaemonThreadFactory.newInstance("keep-alive"));
        keepAliveRunnable = () -> {
            synchronized (lock) {
                try {
                    protocolClient.get().doPing(serviceKey);
                    schedule(); //reschedule another keep alive ping
                } catch (Exception e) {
                    LoggerFactory.getLogger().debug("Keep alive ping failed [" + e.getMessage() + "]", e);
                    //do not re-schedule another keep alive ping if it was having issues
                }
            }
        };
        schedule();
    }

    public synchronized void schedule() {
        if (keepAliveFuture != null) {
            keepAliveFuture.cancel(false);
        }

        keepAliveFuture = keepAliveService.schedule(keepAliveRunnable, KEEP_ALIVE_INTERVAL, TimeUnit.SECONDS);
    }
}