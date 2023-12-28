package com.solarwinds.joboe.core.util;

import com.solarwinds.joboe.core.HostId;

public class DummyHostInfoReader implements HostInfoReader, HostNameReader {

    @Override
    public String getHostName() {
        return "";
    }

    @Override
    public HostId getHostId() {
        return  HostId.builder()
                .hostname(getHostName())
                .build();
    }
}
