package com.tracelytics.util;

import com.tracelytics.util.ServerHostInfoReader.DockerInfoReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class DockerInfoReaderTest {
    private static final String TEST_FILE_PREFIX = "src/test/java/com/tracelytics/util/docker-cgroup-"; //using a rather static path. Using Class.getResourceAsStream does not work in test (vs main)

    @BeforeEach
    protected void tearDown() throws Exception {
        DockerInfoReader.getInstance().initializeLinux(DockerInfoReader.DEFAULT_LINUX_DOCKER_FILE_LOCATION); //reset to default
    }

    @Test
    public void testReadDockerContainerId() throws IOException {
        DockerInfoReader.getInstance().initializeLinux(TEST_FILE_PREFIX + "standard");
        assertEquals("0531ff3c6395131175507ac7e94fdf387f2a2dea81961e6c96f6ac5ccd7ede3f", DockerInfoReader.getDockerId());
        
        DockerInfoReader.getInstance().initializeLinux(TEST_FILE_PREFIX + "standard-2");
        assertEquals("0531ff3c6395131175507ac7e94fdf387f2a2dea81961e6c96f6ac5ccd7ede3f", DockerInfoReader.getDockerId());
        
        DockerInfoReader.getInstance().initializeLinux(TEST_FILE_PREFIX + "ce");
        assertEquals("93d377d55070d2463493706ba7194d119c3efb1c2e7929f36da183ffe71d72a8", DockerInfoReader.getDockerId());
        
        DockerInfoReader.getInstance().initializeLinux(TEST_FILE_PREFIX + "ecs");
        assertEquals("93d377d55070d2463493706ba7194d119c3efb1c2e7929f36da183ffe71d72a8", DockerInfoReader.getDockerId());

        DockerInfoReader.getInstance().initializeLinux(TEST_FILE_PREFIX + "cri-containerd");
        assertEquals("0531ff3c6395131175507ac7e94fdf387f2a2dea81961e6c96f6ac5ccd7ede3f", DockerInfoReader.getDockerId());

        DockerInfoReader.getInstance().initializeLinux(TEST_FILE_PREFIX + "kubepods");
        assertEquals("0531ff3c6395131175507ac7e94fdf387f2a2dea81961e6c96f6ac5ccd7ede3f", DockerInfoReader.getDockerId());
        
        DockerInfoReader.getInstance().initializeLinux(TEST_FILE_PREFIX + "empty");
        assertNull(DockerInfoReader.getDockerId());
        
        DockerInfoReader.getInstance().initializeLinux(TEST_FILE_PREFIX + "non-docker");
        assertNull(DockerInfoReader.getDockerId());
        
        DockerInfoReader.getInstance().initializeLinux(TEST_FILE_PREFIX + "invalid");
        assertNull(DockerInfoReader.getDockerId());
    }
}
