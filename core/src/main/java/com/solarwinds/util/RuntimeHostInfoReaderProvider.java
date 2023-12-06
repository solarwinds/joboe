package com.solarwinds.util;

import com.google.auto.service.AutoService;
import com.solarwinds.lambda.AwsLambdaHostInfoReader;

import static com.solarwinds.util.HostTypeDetector.isLambda;

@AutoService(HostInfoReaderProvider.class)
public class RuntimeHostInfoReaderProvider implements HostInfoReaderProvider{
    @Override
    public HostInfoReader getHostInfoReader() {
        if (isLambda()){
            return new AwsLambdaHostInfoReader();
        }
        return ServerHostInfoReader.INSTANCE;
    }
}
