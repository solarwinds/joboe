package com.appoptics.test;

import com.appoptics.api.ext.Trace;

import javax.batch.api.AbstractBatchlet;
import javax.batch.runtime.BatchStatus;
import javax.inject.Named;

@Named
public class SimpleBatchLet extends AbstractBatchlet {
  
    @Override
    public String process() throws Exception {
        System.out.println("Doing nothing!");
        Trace.createEntryEvent("test-batchlet").report();

        Trace.createExitEvent("test-batchlet").report();
        return BatchStatus.COMPLETED.toString();
    }
}