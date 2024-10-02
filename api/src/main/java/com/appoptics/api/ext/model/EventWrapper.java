package com.appoptics.api.ext.model;

import java.util.Map;

import com.appoptics.api.ext.TraceEvent;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.joboe.Event;

public class EventWrapper implements TraceEvent {
    private Event event;
    
    public EventWrapper(Event event) {
        this.event = event;
    }

    public void addInfo(String key, Object value) {
        event.addInfo(key, value);
    }

    public void addInfo(Map<String, Object> infoMap) {
        event.addInfo(infoMap);
    }

    public void addInfo(Object... info) {
        event.addInfo(info);
    }

    public void setAsync() {
        event.setAsync();
    }

    public void addEdge(String xTraceID) {
        event.addEdge(xTraceID);
    }

    public void report() {
        event.report();
    }

    public void addBackTrace() {
        ClassInstrumentation.addBackTrace(event, 1);
    }
    
    
}
