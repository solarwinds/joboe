package com.tracelytics.test.struts;

import com.appoptics.api.ext.Trace;
import com.appoptics.api.ext.TraceEvent;
import com.opensymphony.xwork2.ActionSupport;
import org.apache.struts2.ServletActionContext;


public class BackTraceAction extends ActionSupport {
    private int backTraceCount = 10;
    
	
	public String execute() {
		for (int i = 0 ; i < backTraceCount ; i++) {
	        TraceEvent infoEvent = Trace.createInfoEvent(null);
	        infoEvent.addBackTrace();
	        infoEvent.report();
	    }

	    ServletActionContext.getRequest().setAttribute("message", "Tried to report " + backTraceCount + " backtrace/info event(s)");
	    return SUCCESS;
	}
	
	public int getBackTraceCount() {
        return backTraceCount;
    }

	public void setBackTraceCount(int backTraceCount) {
        this.backTraceCount = backTraceCount;
    }
}
