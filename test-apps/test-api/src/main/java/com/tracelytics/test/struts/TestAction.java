package com.tracelytics.test.struts;

import java.util.Collections;
import java.util.Date;
import java.util.Map;

import com.appoptics.api.ext.LogMethod;
import com.appoptics.api.ext.ProfileMethod;
import com.appoptics.api.ext.Trace;
import com.appoptics.api.ext.TraceEvent;
import com.opensymphony.xwork2.ActionSupport;



/**
 * This is a test Struts action.  It extends ActionSupport, which implements the Action interface.
 */
public class TestAction extends ActionSupport {
	/** This execute method just sleeps a bit, logs some stuff, sleep again... */
    public static final String LAYER_NAME = "MyApiLayer";
    
	public String execute() {
	    TraceEvent event = Trace.createEntryEvent(LAYER_NAME);
        event.report();
        
        try {
            Thread.sleep(100);
        } catch (InterruptedException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        
        event = Trace.createInfoEvent(LAYER_NAME);
        event.addBackTrace();
        event.addInfo((Map<String, Object>)Collections.singletonMap("firstKey", (Object)1));
        event.addInfo("secondKey", "second");
        event.addInfo("thirdKey", 3.00);
        event.report();
        
        event = Trace.createExitEvent(LAYER_NAME);
        event.report();
        
        try {
            Thread.sleep(100);
        } catch (InterruptedException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        
        Trace.createEntryEvent(LAYER_NAME + "1").report();
        
        String xTraceId = Trace.getCurrentXTraceID();
        
        String nestedLayer = LAYER_NAME + "2";
        event = Trace.createEntryEvent(nestedLayer);
        event.report();
        try {
            Thread.sleep(100);
        } catch (InterruptedException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        Trace.createExitEvent(nestedLayer).report();
        
        event = Trace.createExitEvent(LAYER_NAME + "1");
        event.addEdge(xTraceId); //add edge to point at the entry event
        
        event.report();
        
        event = Trace.startTrace(LAYER_NAME); //this will just create an extra layer instead of a new trace
        event.report();
        try {
            Thread.sleep(100);
        } catch (InterruptedException ie) {
        }
        Trace.endTrace(LAYER_NAME);
             
        
        System.out.println("Executing test action, time=" + getTime());
		
		try {
			Thread.sleep(500);
		} catch (InterruptedException ie) {
			System.err.println("Interrupted, returning.");
			return ERROR;
		}
		
		System.out.println("Done executing test action, time=" + getTime());
		return SUCCESS;
	}
	

    /** @return The time now. */
	@LogMethod(layer = "GetTimeLayer")
	@ProfileMethod(profileName = "GetTimeProfile")
	public String getTime() {
	    try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
		return new Date().toString();
	}
}
