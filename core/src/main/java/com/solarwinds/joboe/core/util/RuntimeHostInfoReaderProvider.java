package com.solarwinds.joboe.core.util;

import com.google.auto.service.AutoService;
import com.solarwinds.joboe.core.lambda.AwsLambdaHostInfoReader;

@AutoService(HostInfoReaderProvider.class)
public class RuntimeHostInfoReaderProvider implements HostInfoReaderProvider{
    @Override
    public HostInfoReader getHostInfoReader() {
        if (HostTypeDetector.isLambda()){
            return new AwsLambdaHostInfoReader();
        }
        return ServerHostInfoReader.INSTANCE;
    }
}
