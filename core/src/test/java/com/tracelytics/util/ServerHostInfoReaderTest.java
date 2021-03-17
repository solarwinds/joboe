package com.tracelytics.util;

import com.tracelytics.joboe.HostId;
import com.tracelytics.util.HostInfoUtils.NetworkAddressInfo;
import junit.framework.TestCase;

import java.io.*;
import java.util.Map;

public class ServerHostInfoReaderTest extends TestCase {
    private static final String TEST_FILE_FOLDER = "src/test/java/com/tracelytics/util/"; //using a rather static path. Using Class.getResourceAsStream does not work in test (vs main)
    private ServerHostInfoReader reader = ServerHostInfoReader.INSTANCE;

    public void testGetDistroParsing() throws IOException {
        assertEquals("Red Hat Enterprise Linux Server release 6.5 (Santiago)", ServerHostInfoReader.getRedHatBasedDistro(getFileReader(ServerHostInfoReader.DistroType.REDHAT_BASED)));
        assertEquals("Amzn Linux 2015.09", ServerHostInfoReader.getAmazonLinuxDistro(getFileReader(ServerHostInfoReader.DistroType.AMAZON)));
        assertEquals("Ubuntu 10.04.2 LTS", ServerHostInfoReader.getUbuntuDistro(getFileReader(ServerHostInfoReader.DistroType.UBUNTU)));
        assertEquals("Debian 7.7", ServerHostInfoReader.getDebianDistro(getFileReader(ServerHostInfoReader.DistroType.DEBIAN)));
        assertEquals("SUSE Linux Enterprise Server 10 (x86_64)", ServerHostInfoReader.getNovellSuseDistro(getFileReader(ServerHostInfoReader.DistroType.SUSE)));
        assertEquals("Slackware-x86_64 13.0", ServerHostInfoReader.getSlackwareDistro(getFileReader(ServerHostInfoReader.DistroType.SLACKWARE)));
        assertEquals("Gentoo Base System release 2.1", ServerHostInfoReader.getGentooDistro(getFileReader(ServerHostInfoReader.DistroType.GENTOO)));
    }
    
    private BufferedReader getFileReader(ServerHostInfoReader.DistroType distroType) throws FileNotFoundException {
        String path = ServerHostInfoReader.DISTRO_FILE_NAMES.get(distroType);
        String fileName = new File(path).getName();
        
        return new BufferedReader(new FileReader(TEST_FILE_FOLDER + fileName));
    }
    
    public void testGetHostMetadata() {
        Map<String, Object> hostMetadata = reader.getHostMetadata();
        
        assert(hostMetadata.containsKey("UnameSysName"));
        assert(hostMetadata.containsKey("UnameVersion"));
        assert(hostMetadata.containsKey("Distro"));
        NetworkAddressInfo networkInfo = reader.getNetworkAddressInfo();
        if (networkInfo != null) {
            if (!networkInfo.getIpAddresses().isEmpty()) {
                assert(hostMetadata.containsKey("IPAddresses"));
            }
        }
    }
    
    public void testGetHostId() {
        HostId hostId = reader.getHostId();
        assertEquals(reader.getHostName(), hostId.getHostname());
        assertEquals(reader.getAwsInstanceId(), hostId.getEc2InstanceId());
        assertEquals(reader.getAwsAvailabilityZone(), hostId.getEc2AvailabilityZone());
        assertEquals(reader.getDockerContainerId(), hostId.getDockerContainerId());
        assertEquals(reader.getHerokuDynoId(), hostId.getHerokuDynoId());
        assertEquals(reader.getNetworkAddressInfo().getMacAddresses(), hostId.getMacAddresses());
        
    }
    
    
    

}
