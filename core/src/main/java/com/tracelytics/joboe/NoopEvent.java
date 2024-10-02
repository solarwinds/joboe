package com.tracelytics.joboe;

import java.nio.ByteBuffer;
import java.util.Map;

public class NoopEvent extends Event {
    NoopEvent(Metadata metadata) {
        super(metadata);
    }
    
    public void addInfo(String key, Object value) {
    }

    public void addInfo(Map<String, ?> infoMap) {
    }

    public void addInfo(Object... info) {
    }

    public void addEdge(Metadata md) {
    }

    public void addEdge(String hexstr) {
    }

    public void setAsync() {
    }

    public void report(EventReporter reporter) {
    }

    public void report() {
    }

    public void report(Metadata md) {
    }

    public void report(Metadata md, EventReporter reporter) {
    }

    public byte[] toBytes() {
        return null;
    }

    public ByteBuffer toByteBuffer() {
        return null;
    }

    public void setTimestamp(long timestamp) {
    }

    @Override
    public void setThreadId(Long threadId) {
    }
}
