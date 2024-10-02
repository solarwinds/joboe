package com.tracelytics.test.action;



public abstract class AbstractTestStress extends AbstractHBaseTest {
    protected static final byte[] ROW_NAME = "stress".getBytes();
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
    
}
