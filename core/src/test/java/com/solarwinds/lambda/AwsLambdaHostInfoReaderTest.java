package com.solarwinds.lambda;

import com.solarwinds.joboe.HostId;
import com.solarwinds.joboe.rpc.HostType;
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