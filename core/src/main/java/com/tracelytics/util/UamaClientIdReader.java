package com.tracelytics.util;

import com.tracelytics.logging.Logger;
import com.tracelytics.logging.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

public class UamaClientIdReader {
    private static final String UAMS_CLIENT_ID_PATH_LINUX = "/var/solarwinds/uamsclient/uamsclientid";
    private static final String UAMS_CLIENT_ID_PATH_WIN = "C:\\ProgramData\\Solarwinds\\uamsclient\\uamsclientid";
    private static final Logger logger = LoggerFactory.getLogger();
    private static final HostInfoUtils.OsType osType = HostInfoUtils.getOsType();
    private static final String uamsClientIdFile = osType == HostInfoUtils.OsType.WINDOWS ?
            UAMS_CLIENT_ID_PATH_WIN : UAMS_CLIENT_ID_PATH_LINUX;
    private static final AtomicReference<String> uamsClientId = new AtomicReference<>();
    private static final AtomicReference<FileTime> lastModified = new AtomicReference<>(FileTime.from(Instant.EPOCH));

    public static String getUamsClientId() {
        try {
            FileTime modifiedTime = Files.getLastModifiedTime(Paths.get(uamsClientIdFile));
            if (!lastModified.get().equals(modifiedTime)) {
                lastModified.set(modifiedTime);
                uamsClientId.set(sanitize(readFirstLine(uamsClientIdFile)));
                logger.debug("Updated uamsclientid to " + uamsClientId.get() + ", lastModifiedTime=" + modifiedTime);
            }
        } catch (IOException e) {
            logger.debug("I cannot read the file " + uamsClientIdFile + ", maybe it doesn't exist.");
        }
        return uamsClientId.get();
    }

    private static String readFirstLine(String filePath) throws IOException {
        String line = null;
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            line = br.readLine();
        }
        return line;
    }

    private static String sanitize(String id) {
        String res = null;
        try {
            if (id.length() != 36) { // UUID in 8-4-4-4-12 format
                throw new IllegalArgumentException("incorrect length");
            }

            String[] parts = id.split("-");
            if (parts.length != 5) {
                throw new IllegalArgumentException("incorrect format");
            }
            res = id;
        } catch (IllegalArgumentException e) {
            logger.debug("Discarded invalid UAMS client id: " + id, e);
        }
        return res;
    }
}
