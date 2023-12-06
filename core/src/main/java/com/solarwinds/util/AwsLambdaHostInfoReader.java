package com.solarwinds.util;

import com.solarwinds.joboe.HostId;
import com.solarwinds.joboe.rpc.HostType;

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
