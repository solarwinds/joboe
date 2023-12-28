package com.solarwinds.joboe.core.rpc;

public enum ResultCode {
    OK, TRY_LATER, INVALID_API_KEY, LIMIT_EXCEEDED, REDIRECT;
    
    public boolean isError() {
        return !(this == OK || this == REDIRECT);
    }
}
