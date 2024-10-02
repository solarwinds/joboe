package com.tracelytics.joboe;

/**
 * All event reporters must implement this interface.
 */
public interface EventReporter {
    public void send(Event event) throws EventReporterException;
    public EventReporterStats consumeStats();
    public void close();
}
