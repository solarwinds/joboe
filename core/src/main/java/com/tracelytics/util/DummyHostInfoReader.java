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
        return new HostId(getHostName(), 0, Collections.EMPTY_LIST, null, null, null, null, null, HostType.PERSISTENT, null, null);

    }

    @Override
    public Map<String, Object> getHostMetadata() {
        return Collections.EMPTY_MAP;
    }

    @Override
    public HostInfoUtils.NetworkAddressInfo getNetworkAddressInfo() {
        return new HostInfoUtils.NetworkAddressInfo(Collections.EMPTY_LIST, Collections.EMPTY_LIST);
    }
}
