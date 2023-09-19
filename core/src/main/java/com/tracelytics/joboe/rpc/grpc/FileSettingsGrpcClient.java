package com.tracelytics.joboe.rpc.grpc;

import com.solarwinds.trace.ingestion.proto.Collector;
import com.tracelytics.joboe.Event;
import com.tracelytics.joboe.rpc.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static com.tracelytics.util.HostTypeDetector.isLambda;

public class FileSettingsGrpcClient implements ProtocolClient {
    private final GrpcClient grpcClient;

    private final String settingsFile;

    public FileSettingsGrpcClient(GrpcClient grpcClient, String settingsFile) {
        this.grpcClient = grpcClient;
        this.settingsFile = settingsFile;
    }

    @Override
    public void shutdown() {
        grpcClient.shutdown();
    }

    @Override
    public Result doPostEvents(String serviceKey, List<Event> events) throws ClientException {
        return grpcClient.doPostEvents(serviceKey, events);
    }

    @Override
    public Result doPostMetrics(String serviceKey, List<Map<String, Object>> messages) throws ClientException {
        return grpcClient.doPostMetrics(serviceKey, messages);
    }

    @Override
    public Result doPostStatus(String serviceKey, List<Map<String, Object>> messages) throws ClientException {
        return grpcClient.doPostStatus(serviceKey, messages);
    }

    @Override
    public SettingsResult doGetSettings(String serviceKey, String version) throws ClientException {
        if (isLambda()) {
            try {
                byte[] bytes = Files.readAllBytes(Paths.get(settingsFile));
                return grpcClient.transformToLocalSettings(Collector.SettingsResult.parseFrom(bytes));
            } catch (IOException e) {
                throw new ClientRecoverableException("Error reading settings from file]", e);
            }
        }

        return grpcClient.doGetSettings(serviceKey, version);
    }

    @Override
    public void doPing(String serviceKey) throws Exception {
        grpcClient.doPing(serviceKey);
    }
}
