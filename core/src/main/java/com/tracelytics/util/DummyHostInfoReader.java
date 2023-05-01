package com.tracelytics.util;

import com.tracelytics.joboe.HostId;
import com.tracelytics.joboe.rpc.HostType;

import java.util.Collections;
import java.util.Map;

public class DummyHostInfoReader implements HostInfoReader {
    @Override
    public String getAzureInstanceId() {
        return null;
    }

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

    @Override
    public Map<String, Object> getHostMetadata() {
        return Collections.emptyMap();
    }

    @Override
    public HostInfoUtils.NetworkAddressInfo getNetworkAddressInfo() {
        return new HostInfoUtils.NetworkAddressInfo(Collections.emptyList(), Collections.emptyList());
    }
}
