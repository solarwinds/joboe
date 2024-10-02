package com.tracelytics.joboe.rpc;

public class Result {
    private final String warning;
    private ResultCode resultCode;
    private String arg;
    public Result(ResultCode resultCode, String arg, String warning) {
        super();
        this.resultCode = resultCode;
        this.arg = arg;
        this.warning = warning;
    }
    
    public ResultCode getResultCode() {
        return resultCode;
    }
    
    public String getArg() {
        return arg;
    }

    public String getWarning() {
        return warning;
    }
}
