package com.tracelytics.joboe.rpc;

public enum ResultCode {
    OK, TRY_LATER, INVALID_API_KEY, LIMIT_EXCEEDED, REDIRECT;
    
    public boolean isError() {
        return !(this == OK || this == REDIRECT);
    }
}
