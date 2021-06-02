package com.tracelytics.joboe;

import com.tracelytics.util.HostInfoUtils.NetworkAddressInfo;
import junit.framework.TestCase;

import java.io.*;
import java.util.Map;

public class AgentHostInfoReaderTest extends TestCase {
    private static final String TEST_FILE_FOLDER = "src/test/java/com/tracelytics/joboe/"; //using a rather static path. Using Class.getResourceAsStream does not work in test (vs main)
    private AgentHostInfoReader reader = new AgentHostInfoReader();

    public void testGetDistroParsing() throws IOException {
        assertEquals("Red Hat Enterprise Linux Server release 6.5 (Santiago)", AgentHostInfoReader.getRedHatBasedDistro(getFileReader(AgentHostInfoReader.DistroType.REDHAT_BASED)));
        assertEquals("Amzn Linux 2015.09", AgentHostInfoReader.getAmazonLinuxDistro(getFileReader(AgentHostInfoReader.DistroType.AMAZON)));
        assertEquals("Ubuntu 10.04.2 LTS", AgentHostInfoReader.getUbuntuDistro(getFileReader(AgentHostInfoReader.DistroType.UBUNTU)));
        assertEquals("Debian 7.7", AgentHostInfoReader.getDebianDistro(getFileReader(AgentHostInfoReader.DistroType.DEBIAN)));
        assertEquals("SUSE Linux Enterprise Server 10 (x86_64)", AgentHostInfoReader.getNovellSuseDistro(getFileReader(AgentHostInfoReader.DistroType.SUSE)));
        assertEquals("Slackware-x86_64 13.0", AgentHostInfoReader.getSlackwareDistro(getFileReader(AgentHostInfoReader.DistroType.SLACKWARE)));
        assertEquals("Gentoo Base System release 2.1", AgentHostInfoReader.getGentooDistro(getFileReader(AgentHostInfoReader.DistroType.GENTOO)));
    }
    
    private BufferedReader getFileReader(AgentHostInfoReader.DistroType distroType) throws FileNotFoundException {
        String path = AgentHostInfoReader.DISTRO_FILE_NAMES.get(distroType);
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
