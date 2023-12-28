package com.solarwinds.joboe.core.util;

import com.solarwinds.joboe.core.HostId;
import com.solarwinds.joboe.core.rpc.HostType;
import com.solarwinds.joboe.core.util.AwsLambdaHostInfoReader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AwsLambdaHostInfoReaderTest {

    private final AwsLambdaHostInfoReader tested = new AwsLambdaHostInfoReader();

    @Test
    void testGetHostName() {
        assertNotNull(tested.getHostName());
    }

    @Test
    void testGetHostId() {
        HostId hostId = tested.getHostId();

        assertNotNull(hostId.getHostname());
        assertNotEquals(0, hostId.getPid());
        assertEquals(HostType.AWS_LAMBDA, hostId.getHostType());

        assertNull(hostId.getAwsMetadata());
        assertNull(hostId.getAzureVmMetadata());
        assertNull(hostId.getK8sMetadata());

        assertNull(hostId.getDockerContainerId());
        assertNull(hostId.getHerokuDynoId());
        assertNull(hostId.getEc2InstanceId());

        assertNull(hostId.getAzureAppServiceInstanceId());
        assertNull(hostId.getUamsClientId());
        assertNull(hostId.getEc2AvailabilityZone());
    }
}