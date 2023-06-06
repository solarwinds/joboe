package com.tracelytics.util;

import java.io.IOException;

import com.tracelytics.util.ServerHostInfoReader.DockerInfoReader;

import junit.framework.TestCase;

public class DockerInfoReaderTest extends TestCase {
    private static final String TEST_FILE_PREFIX = "src/test/java/com/tracelytics/util/docker-cgroup-"; //using a rather static path. Using Class.getResourceAsStream does not work in test (vs main)

    @Override
    protected void tearDown() throws Exception {
        DockerInfoReader.SINGLETON.initializeLinux(DockerInfoReader.DEFAULT_LINUX_DOCKER_FILE_LOCATION); //reset to default
        super.tearDown();
    }
    
    public void testReadDockerContainerId() throws IOException {
        DockerInfoReader.SINGLETON.initializeLinux(TEST_FILE_PREFIX + "standard");
        assertEquals("0531ff3c6395131175507ac7e94fdf387f2a2dea81961e6c96f6ac5ccd7ede3f", DockerInfoReader.getDockerId());
        
        DockerInfoReader.SINGLETON.initializeLinux(TEST_FILE_PREFIX + "standard-2");
        assertEquals("0531ff3c6395131175507ac7e94fdf387f2a2dea81961e6c96f6ac5ccd7ede3f", DockerInfoReader.getDockerId());
        
        DockerInfoReader.SINGLETON.initializeLinux(TEST_FILE_PREFIX + "ce");
        assertEquals("93d377d55070d2463493706ba7194d119c3efb1c2e7929f36da183ffe71d72a8", DockerInfoReader.getDockerId());
        
        DockerInfoReader.SINGLETON.initializeLinux(TEST_FILE_PREFIX + "ecs");
        assertEquals("93d377d55070d2463493706ba7194d119c3efb1c2e7929f36da183ffe71d72a8", DockerInfoReader.getDockerId());

        DockerInfoReader.SINGLETON.initializeLinux(TEST_FILE_PREFIX + "cri-containerd");
        assertEquals("0531ff3c6395131175507ac7e94fdf387f2a2dea81961e6c96f6ac5ccd7ede3f", DockerInfoReader.getDockerId());

        DockerInfoReader.SINGLETON.initializeLinux(TEST_FILE_PREFIX + "kubepods");
        assertEquals("0531ff3c6395131175507ac7e94fdf387f2a2dea81961e6c96f6ac5ccd7ede3f", DockerInfoReader.getDockerId());
        
        DockerInfoReader.SINGLETON.initializeLinux(TEST_FILE_PREFIX + "empty");
        assertNull(DockerInfoReader.getDockerId());
        
        DockerInfoReader.SINGLETON.initializeLinux(TEST_FILE_PREFIX + "non-docker");
        assertNull(DockerInfoReader.getDockerId());
        
        DockerInfoReader.SINGLETON.initializeLinux(TEST_FILE_PREFIX + "invalid");
        assertNull(DockerInfoReader.getDockerId());
    }
}
