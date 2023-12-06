package com.solarwinds.util;

import com.solarwinds.joboe.HostId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class K8sReaderTest {
    private ServerHostInfoReader.K8sReader tested;

    @BeforeEach
    protected void setUp() throws Exception {
        ServerHostInfoReader.K8sReader.NAMESPACE_FILE_LOC_LINUX = "src/test/java/com/tracelytics/util/namespace";

        ServerHostInfoReader.K8sReader.POD_UUID_FILE_LOC = "src/test/java/com/tracelytics/util/poduid";
        ServerHostInfoReader.osType = HostInfoUtils.OsType.LINUX;
        tested = new ServerHostInfoReader.K8sReader();
    }

    @Test
    public void testReadK8sMetadata() {
        HostId.K8sMetadata actual = tested.getK8sMetadata();
        HostId.K8sMetadata expected = HostId.K8sMetadata.builder()
                .namespace("o11y-platform")
                .podUid("9dcdb600-4156-4b7b-afcc-f8c06cb0e474")
                .podName(ServerHostInfoReader.INSTANCE.getHostName())
                .build();

        assertEquals(expected, actual);
    }

}
