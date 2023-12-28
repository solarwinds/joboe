package com.solarwinds.joboe.core;

/**
 * All event reporters must implement this interface.
 */
public interface EventReporter {
    void send(Event event) throws EventReporterException;

    EventReporterStats consumeStats();

    void close();
}
