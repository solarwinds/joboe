package com.solarwinds.joboe.core.util;

import com.google.auto.service.AutoService;

@AutoService(HostInfoReaderProvider.class)
public class RuntimeHostInfoReaderProvider implements HostInfoReaderProvider{
    @Override
    public HostInfoReader getHostInfoReader() {
        return ServerHostInfoReader.INSTANCE;
    }
}
