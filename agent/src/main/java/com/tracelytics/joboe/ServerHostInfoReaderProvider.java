package com.tracelytics.joboe;

import com.google.auto.service.AutoService;
import com.tracelytics.util.HostInfoReader;
import com.tracelytics.util.HostInfoReaderProvider;
import com.tracelytics.util.ServerHostInfoReader;

@AutoService(HostInfoReaderProvider.class)
public class ServerHostInfoReaderProvider implements HostInfoReaderProvider {
    @Override
    public HostInfoReader getHostInfoReader() {
        return ServerHostInfoReader.INSTANCE;
    }
}
