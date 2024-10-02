package com.appoptics.test.struts;

import java.util.Date;

import com.opensymphony.xwork2.ActionSupport;

/**
 * This is a test Struts action.  It extends ActionSupport, which implements the Action interface.
 */
public class TestAction extends ActionSupport {
	/** This execute method just sleeps a bit, logs some stuff, sleep again... */
	public String execute() {
		System.out.println("Executing test action, time=" + getTime());
		
		try {
			Thread.sleep(2000);
		} catch (InterruptedException ie) {
			System.err.println("Interrupted, returning.");
			return ERROR;
		}
		
		System.out.println("Done executing test action, time=" + getTime());
		return SUCCESS;
	}
	
	/** @return The time now. */
	public String getTime() {
		return new Date().toString();
	}
}
