package com.tracelytics.util;

import com.google.auto.service.AutoService;

import static com.tracelytics.util.HostTypeDetector.isLambda;

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
