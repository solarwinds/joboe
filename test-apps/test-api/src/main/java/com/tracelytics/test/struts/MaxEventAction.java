package com.tracelytics.test.struts;

import org.apache.struts2.ServletActionContext;

import com.appoptics.api.ext.Trace;
import com.appoptics.api.ext.TraceEvent;
import com.opensymphony.xwork2.ActionSupport;



public class MaxEventAction extends ActionSupport {
    private int eventCount;
    
	
	public String execute() {
	    for (int i = 0 ; i < eventCount ; i++) {
	        TraceEvent infoEvent = Trace.createInfoEvent(null);
	        infoEvent.report();
	    }
	    
	    ServletActionContext.getRequest().setAttribute("message", "Tried to report " + eventCount + " info event(s)");
	    return SUCCESS;
	}
	
	public int getEventCount() {
        return eventCount;
    }
	
	public void setEventCount(int eventCount) {
        this.eventCount = eventCount;
    }
}
