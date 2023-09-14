package com.tracelytics.util;

import com.tracelytics.joboe.HostId;
import com.tracelytics.joboe.rpc.HostType;

import java.util.Collections;
import java.util.Map;

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
