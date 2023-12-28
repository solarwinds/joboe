package com.solarwinds.joboe.core;

import java.nio.ByteBuffer;
import java.util.Map;

public class NoopEvent extends Event {
    NoopEvent(Metadata metadata) {
        super(metadata);
    }
    
    @Override
    public void addInfo(String key, Object value) {
    }

    @Override
    public void addInfo(Map<String, ?> infoMap) {
    }

    @Override
    public void addInfo(Object... info) {
    }

    @Override
    public void addEdge(Metadata md) {
    }

    @Override
    public void addEdge(String hexstr) {
    }

    @Override
    public void setAsync() {
    }

    @Override
    public void report(EventReporter reporter) {
    }

    @Override
    public void report() {
    }

    @Override
    public void report(Metadata md) {
    }

    @Override
    public void report(Metadata md, EventReporter reporter) {
    }

    @Override
    public byte[] toBytes() {
        return null;
    }

    @Override
    public ByteBuffer toByteBuffer() {
        return null;
    }

    @Override
    public void setTimestamp(long timestamp) {
    }

    @Override
    public void setThreadId(Long threadId) {
    }
}
