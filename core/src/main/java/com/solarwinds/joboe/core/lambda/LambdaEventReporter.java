package com.solarwinds.joboe.core.lambda;

import com.solarwinds.joboe.core.AtomicEventReporterStats;
import com.solarwinds.joboe.core.Event;
import com.solarwinds.joboe.core.EventReporter;
import com.solarwinds.joboe.core.EventReporterStats;
import com.solarwinds.joboe.core.logging.LoggerFactory;
import com.solarwinds.joboe.core.rpc.Client;
import com.solarwinds.joboe.core.rpc.ClientException;
import com.solarwinds.joboe.core.rpc.Result;

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
