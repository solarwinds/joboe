package com.tracelytics.test;

import com.tracelytics.agent.Agent;
import com.tracelytics.joboe.*;

/**
 * Base class for testing instrumented code.
 *
 * Starts a trace then calls run. This code assumes it is running with the java agent loaded in
 * the standard way (-javaagent.) and does not attempt to initialize the agent.
 */
public abstract class TracedApp {

    private String layerName = "";

    public void setLayerName(String layerName) {
        this.layerName = layerName;
    }

    public void runTrace() throws Exception {
        Event event = Context.startTrace();
        event.addInfo("Layer", layerName, "Label", "entry", "URL", "/"+layerName);        
        event.report(Agent.reporter());

        run(); // calls into subclass

        event = Context.createEvent();
        event.addInfo("Layer", layerName, "Label", "exit");        
        event.report(Agent.reporter());
    }

    public abstract void run() throws Exception ; // should be implemented by subclass
}
