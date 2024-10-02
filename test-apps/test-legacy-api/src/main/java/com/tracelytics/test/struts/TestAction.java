package com.tracelytics.test.struts;

import java.util.Collections;
import java.util.Date;
import java.util.Map;

import com.tracelytics.api.ext.LogMethod;
import com.tracelytics.api.ext.ProfileMethod;
import com.tracelytics.api.ext.Trace;
import com.tracelytics.api.ext.TraceEvent;
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
                
        System.out.println("Executing test action, time=" + getTime());
		
		try {
			Thread.sleep(500);
		} catch (InterruptedException ie) {
			System.err.println("Interrupted, returning.");
			return ERROR;
		}
		
		System.out.println("Done executing test action, time=" + getTime());
		
		Trace.createExitEvent(LAYER_NAME).report();
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
