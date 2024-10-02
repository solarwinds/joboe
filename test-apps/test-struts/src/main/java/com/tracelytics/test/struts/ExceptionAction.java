package com.tracelytics.test.struts;

import com.opensymphony.xwork2.ActionSupport;

/**
 * This is a test Struts action.  It extends ActionSupport, which implements the Action interface.
 */
public class ExceptionAction extends ActionSupport {
	/** This execute method just sleeps a bit, logs some stuff, sleep again... */
	public String execute() {
		return ERROR;
	}
}
