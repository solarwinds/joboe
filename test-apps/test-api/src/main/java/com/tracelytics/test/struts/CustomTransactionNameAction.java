package com.tracelytics.test.struts;

import org.apache.struts2.ServletActionContext;

import com.appoptics.api.ext.Trace;
import com.opensymphony.xwork2.ActionSupport;



public class CustomTransactionNameAction extends ActionSupport {
    private String customTransactionName;
    
	
	public String execute() {
	    Trace.setTransactionName(customTransactionName);
	    
	    ServletActionContext.getRequest().setAttribute("message", "Set transaction name: " + customTransactionName);
	    return SUCCESS;
	}
	
	public String getCustomTransactionName() {
        return customTransactionName;
    }
	
	public void setCustomTransactionName(String customTransactionName) {
        this.customTransactionName = customTransactionName;
    }
}
