package com.tracelytics.util;

import com.tracelytics.joboe.HostId;

import java.util.Map;

public interface HostInfoReader {
    String getAzureInstanceId();

    String getHostName();

    HostId getHostId();

    Map<String, Object> getHostMetadata();

    HostInfoUtils.NetworkAddressInfo getNetworkAddressInfo();
}
