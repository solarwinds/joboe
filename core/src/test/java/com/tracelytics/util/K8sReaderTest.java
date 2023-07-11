package com.tracelytics.util;

import com.tracelytics.joboe.HostId;
import junit.framework.TestCase;

public class K8sReaderTest extends TestCase {
    private ServerHostInfoReader.K8sReader tested;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        ServerHostInfoReader.K8sReader.NAMESPACE_FILE_LOC_LINUX = "src/test/java/com/tracelytics/util/namespace";

        ServerHostInfoReader.K8sReader.POD_UUID_FILE_LOC = "src/test/java/com/tracelytics/util/poduid";
        ServerHostInfoReader.osType = HostInfoUtils.OsType.LINUX;
        tested = new ServerHostInfoReader.K8sReader();
    }

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
