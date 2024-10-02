package com.tracelytics.test.action;


public abstract class AbstractMongoDbStressAction extends AbstractMongoDbAction {
 private static final int MAX_RUN_COUNT = 100000;
    
    private int runCount;

    public int getRunCount() {
        return runCount;
    }

    public void setRunCount(int runCount) {
        this.runCount = runCount;
    }
    
    @Override
    public void validate() {
        super.validate();
        if (runCount < 0) { 
            runCount = 0;
        } else if (runCount > MAX_RUN_COUNT) {
            runCount = MAX_RUN_COUNT;
        }
    }
    

    @Override
    public final String execute() throws Exception {
        for (int i = 0 ; i < runCount; i++) {
            unitExecute(i);
        }
        
        addActionMessage("Run [" + runCount + "] of operation [" + getOperation() + "]");
                
        return SUCCESS;
    }

    protected abstract void unitExecute(int currentRun) throws Exception;
    protected abstract String getOperation();
    
}
