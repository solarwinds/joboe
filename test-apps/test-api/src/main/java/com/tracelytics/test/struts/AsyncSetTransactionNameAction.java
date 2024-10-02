package com.tracelytics.test.struts;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.appoptics.api.ext.Trace;
import com.opensymphony.xwork2.ActionSupport;

public class AsyncSetTransactionNameAction extends ActionSupport {

	public String execute() {
	    Future<?> future = Executors.newSingleThreadExecutor().submit(new Runnable() {
	        public void run() {
	            Trace.setTransactionName("asynchronous-set-transaction-name");
	        }    
	    });
	    
	    try {
            future.get();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ExecutionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
	    
		return SUCCESS;
	}
}
