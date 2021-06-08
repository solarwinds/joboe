package com.tracelytics.joboe;

import com.google.auto.service.AutoService;
import com.tracelytics.util.HostInfoReader;
import com.tracelytics.util.HostInfoReaderProvider;

@AutoService(HostInfoReaderProvider.class)
public class AgentHostInfoReaderProvider implements HostInfoReaderProvider {
    @Override
    public HostInfoReader getHostInfoReader() {
        return AgentHostInfoReader.INSTANCE;
    }
}
