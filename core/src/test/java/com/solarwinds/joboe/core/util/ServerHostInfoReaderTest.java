package com.solarwinds.joboe.core.util;

import com.solarwinds.joboe.core.HostId;
import com.solarwinds.joboe.core.util.HostInfoUtils;
import com.solarwinds.joboe.core.util.HostInfoUtils.NetworkAddressInfo;
import com.solarwinds.joboe.core.util.ServerHostInfoReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ServerHostInfoReaderTest {
    private static final String TEST_FILE_FOLDER = "src/test/java/com/solarwinds/joboe/core/util/"; //using a rather static path. Using Class.getResourceAsStream does not work in test (vs main)
    private final ServerHostInfoReader reader = ServerHostInfoReader.INSTANCE;

    @BeforeEach
    void setup(){
        ServerHostInfoReader.DockerInfoReader.getInstance().initializeLinux(ServerHostInfoReader.DockerInfoReader.DEFAULT_LINUX_DOCKER_FILE_LOCATION); //reset to default
    }

    @Test
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

    @Test
    public void testGetHostMetadata() {
        Map<String, Object> hostMetadata = reader.getHostMetadata();
        
        assert(hostMetadata.containsKey("UnameSysName"));
        assert(hostMetadata.containsKey("UnameVersion"));
        HostInfoUtils.OsType osType = HostInfoUtils.getOsType();
        assert osType != HostInfoUtils.OsType.LINUX && osType != HostInfoUtils.OsType.WINDOWS || (hostMetadata.containsKey("Distro"));
        NetworkAddressInfo networkInfo = reader.getNetworkAddressInfo();
        if (networkInfo != null) {
            assert networkInfo.getIpAddresses().isEmpty() || (hostMetadata.containsKey("IPAddresses"));
        }
    }

    @Test
    public void testGetHostId() {
        HostId hostId = reader.getHostId();
        assertEquals(reader.getHostName(), hostId.getHostname());
        assertEquals(reader.getAwsInstanceId(), hostId.getEc2InstanceId());
        assertEquals(reader.getAwsAvailabilityZone(), hostId.getEc2AvailabilityZone());
        assertEquals(reader.getHerokuDynoId(), hostId.getHerokuDynoId());
        assertEquals(reader.getNetworkAddressInfo().getMacAddresses(), hostId.getMacAddresses());
        
    }
    
    
    

}
