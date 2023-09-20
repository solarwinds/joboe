package com.tracelytics.util;

import com.tracelytics.ext.json.JSONException;
import com.tracelytics.ext.json.JSONObject;
import com.tracelytics.logging.Logger;
import com.tracelytics.logging.LoggerFactory;
import com.appoptics.ext.okhttp3.OkHttpClient;
import com.appoptics.ext.okhttp3.Request;
import com.appoptics.ext.okhttp3.Response;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class UamsClientIdReader {
    private static final Logger logger = LoggerFactory.getLogger();
    private static final HostInfoUtils.OsType osType = HostInfoUtils.getOsType();
    private static final Path uamsClientIdPath;
    private static final AtomicReference<String> uamsClientId = new AtomicReference<>();
    private static final AtomicReference<FileTime> lastModified = new AtomicReference<>(FileTime.from(Instant.EPOCH));

    private static final OkHttpClient restClient = new OkHttpClient.Builder().build();

    static {
        if (osType == HostInfoUtils.OsType.WINDOWS) {
            String programData = System.getenv("PROGRAMDATA");
            if (programData == null) {
                programData = "C:\\ProgramData\\";
            }
            uamsClientIdPath = Paths.get(programData, "SolarWinds", "UAMSClient", "uamsclientid");
        } else {
            uamsClientIdPath = Paths.get("/", "opt", "solarwinds", "uamsclient", "var", "uamsclientid");
        }
        logger.debug("Set uamsclientid path to " + uamsClientIdPath);
    }

    public static String getUamsClientId() {
        try {
            FileTime modifiedTime = Files.getLastModifiedTime(uamsClientIdPath);
            if (!lastModified.get().equals(modifiedTime)) {
                lastModified.set(modifiedTime);
                uamsClientId.set(sanitize(readFirstLine(uamsClientIdPath)));
                logger.debug("Updated uamsclientid to " + uamsClientId.get() + ", lastModifiedTime=" + modifiedTime);
            }
        } catch (IOException e) {
            logger.debug(String.format("Cannot read the file[%s] due error: %s", uamsClientIdPath, e));
            getUamsClientIdViaRestApi().ifPresent(uamsClientId::set);
        }
        return uamsClientId.get();
    }

    static Optional<String> getUamsClientIdViaRestApi() {
        Request request = new Request.Builder()
                .url("http://127.0.0.1:2113/info/uamsclient")
                .get()
                .build();

        try (Response response = restClient.newCall(request).execute()) {
            String payload = null;
            if (response.isSuccessful() && response.body() != null) {
                payload = response.body().string();
                JSONObject jsonPayload = new JSONObject(payload);
                String clientId = jsonPayload.getString("uamsclient_id");

                logger.debug(String.format("Got UAMS client ID(%s) from API, using hardcoded endpoint", clientId));
                return Optional.ofNullable(clientId);
            }

            logger.debug(String.format("Request to UAMS REST endpoint failed. Status=%d, payload=%s", response.code(), payload));

        } catch (IOException | JSONException exception) {
            logger.debug(String.format("Error reading from UAMS REST endpoint\n%s", exception));
        }

        return Optional.empty();
    }

    private static String readFirstLine(Path filePath) throws IOException {
        String line = null;
        try (BufferedReader br = new BufferedReader(new FileReader(filePath.toFile()))) {
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
