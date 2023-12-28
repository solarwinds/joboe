package com.solarwinds.joboe.core.rpc;

import lombok.Getter;

@Getter
public class Result {
    private final String warning;
    private final ResultCode resultCode;
    private final String arg;
    public Result(ResultCode resultCode, String arg, String warning) {
        super();
        this.resultCode = resultCode;
        this.arg = arg;
        this.warning = warning;
    }

}
