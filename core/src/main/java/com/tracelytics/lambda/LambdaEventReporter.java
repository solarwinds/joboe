package com.tracelytics.lambda;

import com.tracelytics.joboe.*;
import com.tracelytics.joboe.rpc.Client;
import com.tracelytics.joboe.rpc.ClientException;
import com.tracelytics.joboe.rpc.Result;
import com.tracelytics.logging.LoggerFactory;

import java.util.Collections;
import java.util.concurrent.ExecutionException;

public class LambdaEventReporter implements EventReporter {
    private final Client client;

    private final Client.Callback<Result> loggingCallback;

    private final AtomicEventReporterStats eventReporterStats;

    public LambdaEventReporter(Client client, Client.Callback<Result> loggingCallback, AtomicEventReporterStats eventReporterStats) {
        this.client = client;
        this.loggingCallback = loggingCallback;
        this.eventReporterStats = eventReporterStats;
    }

    @Override
    public void send(Event event) {
        try {
            Result result = client.postEvents(Collections.singletonList(event), loggingCallback).get();
            if (result.getResultCode().isError()) {
                eventReporterStats.incrementFailedCount(1);
            } else {
                eventReporterStats.incrementSentCount(1);
            }

        } catch (InterruptedException | ExecutionException | ClientException e) {
            LoggerFactory.getLogger().error("Unable to send event", e);
            eventReporterStats.incrementFailedCount(1);
        } finally {
            eventReporterStats.incrementProcessedCount(1);
        }
    }

    @Override
    public EventReporterStats consumeStats() {
        return eventReporterStats.consumeStats();
    }

    @Override
    public void close() {
        client.close();
    }
}
