package com.solarwinds.joboe.core.util;

import com.solarwinds.joboe.core.HostId;
import com.solarwinds.joboe.core.rpc.HostType;

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
