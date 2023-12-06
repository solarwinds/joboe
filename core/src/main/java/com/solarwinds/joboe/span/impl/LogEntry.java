package com.solarwinds.joboe.span.impl;

import lombok.Getter;

import java.util.Map;

/**
 * Structure to store an entry from {@link Span#log} methods
 * @author pluk
 *
 */
@Getter
public class LogEntry {
    private final long timestamp;
    private final Map<String, ?> fields;
    private final boolean error;
    
    public LogEntry(long timestamp, Map<String, ?> fields) {
        this(timestamp, fields, false);
    }
    
    public LogEntry(long timestamp, Map<String, ?> fields, boolean error) {
        super();
        this.timestamp = timestamp;
        this.fields = fields;
        this.error = error;
    }

}
