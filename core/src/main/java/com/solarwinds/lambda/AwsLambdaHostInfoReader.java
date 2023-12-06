package com.solarwinds.lambda;

import com.solarwinds.joboe.HostId;
import com.solarwinds.joboe.rpc.HostType;
import com.solarwinds.util.HostInfoReader;
import com.solarwinds.util.HostNameReader;
import com.solarwinds.util.JavaProcessUtils;
import com.solarwinds.util.ServerHostInfoReader;

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
