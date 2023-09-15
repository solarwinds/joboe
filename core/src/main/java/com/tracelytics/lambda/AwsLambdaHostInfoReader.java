package com.tracelytics.lambda;

import com.tracelytics.joboe.HostId;
import com.tracelytics.joboe.rpc.HostType;
import com.tracelytics.util.HostInfoReader;
import com.tracelytics.util.HostNameReader;
import com.tracelytics.util.JavaProcessUtils;
import com.tracelytics.util.ServerHostInfoReader;

public class AwsLambdaHostInfoReader implements HostInfoReader, HostNameReader {

    @Override
    public String getHostName() {
        return ServerHostInfoReader.INSTANCE.getHostName();
    }

    @Override
    public HostId getHostId() {
        return HostId.builder()
                .hostname(getHostName())
                .hostType(HostType.AWS_LAMBDA)
                .pid(JavaProcessUtils.getPid())
                .build();
    }
}
