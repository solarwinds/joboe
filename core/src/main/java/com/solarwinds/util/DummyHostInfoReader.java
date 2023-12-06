package com.solarwinds.util;

import com.solarwinds.joboe.HostId;

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
