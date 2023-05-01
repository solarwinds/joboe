package com.tracelytics.joboe.rpc.grpc;

import com.appoptics.ext.io.grpc.*;
import com.appoptics.ext.io.grpc.netty.GrpcSslContexts;
import com.appoptics.ext.io.grpc.netty.NettyChannelBuilder;
import com.solarwinds.trace.ingestion.proto.Collector;
import com.solarwinds.trace.ingestion.proto.TraceCollectorGrpc;
import com.tracelytics.ext.google.protobuf.ByteString;
import com.tracelytics.joboe.BsonBufferException;
import com.tracelytics.joboe.Event;
import com.tracelytics.joboe.HostId;
import com.tracelytics.joboe.config.ConfigManager;
import com.tracelytics.joboe.config.ConfigProperty;
import com.tracelytics.joboe.config.ProxyConfig;
import com.tracelytics.joboe.rpc.ResultCode;
import com.tracelytics.joboe.rpc.SettingsResult;
import com.tracelytics.joboe.rpc.*;
import com.tracelytics.joboe.settings.Settings;
import com.tracelytics.logging.Logger;
import com.tracelytics.logging.LoggerFactory;
import com.tracelytics.util.BsonUtils;
import com.tracelytics.util.HostInfoUtils;
import com.tracelytics.util.SslUtils;

import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URL;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static com.tracelytics.util.HostInfoUtils.getHostId;

/**
 * Collector protocol client that uses the gRPC protocol.
 * <p>
 * Wraps the gRPC client generated from the collector.proto definition
 */
public class GrpcClient implements ProtocolClient {
    private static Logger logger = LoggerFactory.getLogger();
    private final TraceCollectorGrpc.TraceCollectorBlockingStub client;
    private final GrpcHostIdManager hostIdManager = new GrpcHostIdManager();

    private static final int INITIAL_MESSAGE_SIZE = 64 * 1024; //64 kB


    GrpcClient(TraceCollectorGrpc.TraceCollectorBlockingStub blockingStub) {
        this.client = blockingStub;
    }


    @Override
    public void shutdown() {
        if (client != null && client.getChannel() instanceof ManagedChannel) {
            ((ManagedChannel) client.getChannel()).shutdown();
        }
    }

    @Override
    public Result doPostEvents(String serviceKey, List<Event> events) throws ClientException {
        return postInBatch(serviceKey, events, EVENT_SERIALIZER, POST_EVENTS_ACTION);
    }

    @Override
    public Result doPostMetrics(String serviceKey, List<Map<String, Object>> messages) throws ClientException {
        return postInBatch(serviceKey, messages, KEY_VALUE_MAP_SERIALIZER, POST_METRICS_ACTION);
    }

    @Override
    public Result doPostStatus(String serviceKey, List<Map<String, Object>> messages) throws ClientException {
        return postInBatch(serviceKey, messages, KEY_VALUE_MAP_SERIALIZER, POST_STATUS_ACTION);
    }

    @Override
    public SettingsResult doGetSettings(String serviceKey, String version) throws ClientException {
        //For getSettings call, we decided to fill in `hostname` only for `HostID` for consistency with other agent implementation
        //Unlike other language agent, this change does not give any performance boost to java agent
        Collector.SettingsRequest request = Collector.SettingsRequest.newBuilder().setApiKey(serviceKey).setClientVersion(version).setIdentity(hostIdManager.getHostnameOnlyHostID()).build();
        try {
            Collector.SettingsResult result = client.getSettings(request);

            List<Settings> settings = new ArrayList<Settings>();
            if (result.getResult() != null && result.getResult() == Collector.ResultCode.OK) {
                for (Collector.OboeSetting oboeSetting : result.getSettingsList()) {
                    settings.add(convertSetting(oboeSetting));
                }
            }

            return new SettingsResult(ResultCode.valueOf(result.getResult().name()), result.getArg(), result.getWarning(), settings);
        } catch (StatusRuntimeException e) {
            throw new ClientRecoverableException("gRPC Operation failed : [get settings] status [" + e.getStatus() + "]", e);
        }
    }

    @Override
    public void doPing(String serviceKey) throws ClientException {
        try {
            client.ping(Collector.PingRequest.newBuilder().setApiKey(serviceKey).build());
        } catch (StatusRuntimeException e) {
            throw new ClientRecoverableException("gRPC Operation failed : [ping] status [" + e.getStatus() + "]", e);
        }
    }

    private Settings convertSetting(Collector.OboeSetting grpcOboeSetting) {
        Map<String, ByteBuffer> convertedArguments = new HashMap<String, ByteBuffer>();

        for (Entry<String, ByteString> argumentEntry : grpcOboeSetting.getArgumentsMap().entrySet()) {
            convertedArguments.put(argumentEntry.getKey(), argumentEntry.getValue().asReadOnlyByteBuffer());
        }

        com.tracelytics.joboe.rpc.Settings settings = new com.tracelytics.joboe.rpc.Settings(
                convertType(grpcOboeSetting.getType()),
                grpcOboeSetting.getFlags().toStringUtf8(),
                //oboeSetting.getTimestamp(),
                System.currentTimeMillis(), //use local timestamp for now, as it is easier to compare ttl with it
                grpcOboeSetting.getValue(),
                grpcOboeSetting.getTtl(),
                grpcOboeSetting.getLayer().toStringUtf8(),
                convertedArguments);

        return settings;
    }

    private short convertType(Collector.OboeSettingType grpcType) {
        switch (grpcType) {
            case DEFAULT_SAMPLE_RATE:
                return Settings.OBOE_SETTINGS_TYPE_DEFAULT_SAMPLE_RATE;
            case LAYER_SAMPLE_RATE:
                return Settings.OBOE_SETTINGS_TYPE_LAYER_SAMPLE_RATE;
            case LAYER_APP_SAMPLE_RATE:
                return Settings.OBOE_SETTINGS_TYPE_LAYER_APP_SAMPLE_RATE;
            case LAYER_HTTPHOST_SAMPLE_RATE:
                return Settings.OBOE_SETTINGS_TYPE_LAYER_HTTPHOST_SAMPLE_RATE;
            default:
                return -1;
        }
    }

    private <T> Result postInBatch(String serviceKey, List<T> items, Serializer<T> serializer, PostAction postAction) throws ClientException {
        List<List<ByteString>> itemsByCalls = new ArrayList<List<ByteString>>();
        List<ByteString> byteStrings = new ArrayList<ByteString>();
        itemsByCalls.add(byteStrings);

        long callSize = 0;
        for (T item : items) {
            try {
                ByteString itemByteString = serializer.serialize(item);
                int size = itemByteString.size();
                if (callSize + size > MAX_CALL_SIZE) {
                    byteStrings = new ArrayList<ByteString>();
                    itemsByCalls.add(byteStrings);
                    callSize = size;
                } else {
                    callSize += size;
                }
                byteStrings.add(itemByteString);

            } catch (BsonBufferException e) {
                logger.warn("Failed to perform action [" + postAction.getDescription() + "] due to buffer exception : " + e.getMessage(), e);
                throw new ClientFatalException(e);
            }
        }

        Collector.MessageResult resultMessage = null;
        for (List<ByteString> itemsAsByteString : itemsByCalls) {
            Collector.HostID hostId = hostIdManager.getHostID();
            Collector.MessageRequest.Builder builder = Collector.MessageRequest.newBuilder().setApiKey(serviceKey).setIdentity(hostId).setEncoding(Collector.EncodingType.BSON);
            logger.debug(postAction.getDescription() + " " + itemsAsByteString.size() + " item(s) using gRPC client " + GrpcClient.this + ", hostId=" + hostId);
            try {
                builder.addAllMessages(itemsAsByteString);
                resultMessage = postAction.post(client, builder.build());
            } catch (StatusRuntimeException e) {
                if (e.getStatus().getCode() == Status.RESOURCE_EXHAUSTED.getCode()) {
                    throw new ClientFatalException("gRPC Operation failed : [post events] status [" + e.getStatus() + "]. This is not recoverable due to exhausted resource.", e);
                } else {
                    throw new ClientRecoverableException("gRPC Operation failed : [post events] status [" + e.getStatus() + "]", e);
                }
            }
        }

        return new Result(resultMessage.getResult() != null ? ResultCode.valueOf(resultMessage.getResult().name()) : null, resultMessage.getArg(), resultMessage.getWarning());
    }

    interface Serializer<T> {
        ByteString serialize(T item) throws BsonBufferException;
    }

    interface PostAction {
        Collector.MessageResult post(TraceCollectorGrpc.TraceCollectorBlockingStub client, Collector.MessageRequest messageRequest) throws StatusRuntimeException;
        String getDescription();
    }

    private static final Serializer<Map<String, Object>> KEY_VALUE_MAP_SERIALIZER = new Serializer<Map<String, Object>>() {
        @Override
        public ByteString serialize(Map<String, Object> item) throws BsonBufferException {
            return ByteString.copyFrom(BsonUtils.convertMapToBson(item, INITIAL_MESSAGE_SIZE, MAX_MESSAGE_SIZE));
        }
    };

    private static final Serializer<Event> EVENT_SERIALIZER = new Serializer<Event>() {
        @Override
        public ByteString serialize(Event event) throws BsonBufferException {
            return ByteString.copyFrom(event.toBytes());
        }
    };

    private static final PostAction POST_EVENTS_ACTION = new PostAction() {
        @Override
        public Collector.MessageResult post(TraceCollectorGrpc.TraceCollectorBlockingStub client, Collector.MessageRequest request) {
            return client.postEvents(request);
        }

        @Override
        public String getDescription() {
            return "Post Events";
        }
    };

    private static final PostAction POST_STATUS_ACTION = new PostAction() {
        @Override
        public Collector.MessageResult post(TraceCollectorGrpc.TraceCollectorBlockingStub client, Collector.MessageRequest request) {
            return client.postStatus(request);
        }

        @Override
        public String getDescription() {
            return "Post Status Message";
        }
    };

    private static final PostAction POST_METRICS_ACTION = new PostAction() {
        @Override
        public Collector.MessageResult post(TraceCollectorGrpc.TraceCollectorBlockingStub client, Collector.MessageRequest request) {
            return client.postMetrics(request);
        }

        @Override
        public String getDescription() {
            return "Post Metrics";
        }
    };

    static class GrpcProtocolClientFactory implements ProtocolClientFactory<GrpcClient> {
        private final TrustManagerFactory trustManagerFactory;
        private final ProxyConfig proxyConfig = (ProxyConfig) ConfigManager.getConfig(ConfigProperty.AGENT_PROXY);
        private static final String compression;
        private static final String DEFAULT_COMPRESSION = "gzip";

        static {
            String compressionString = ((String) ConfigManager.getConfig(ConfigProperty.AGENT_GRPC_COMPRESSION));
            compression = compressionString != null ? compressionString.toLowerCase() : DEFAULT_COMPRESSION;
            logger.debug("Using compression " + compression + " for gRPC client");
        }

        GrpcProtocolClientFactory(URL serverCertLocation) throws IOException, GeneralSecurityException {
            TrustManagerFactory factory = null;
            if (serverCertLocation != null) {
//                factory = InsecureTrustManagerFactory.INSTANCE; //for testing
                try {
                    factory = SslUtils.getTrustManagerFactory(serverCertLocation);
                } catch (GeneralSecurityException e) {
                    logger.warn("Failed to initialize trust manager factory for GRPC client: " + e.getMessage(), e);
                } catch (IOException e) {
                    logger.warn("Failed to initialize trust manager factory for GRPC client: " + e.getMessage(), e);
                }
            }

            this.trustManagerFactory = factory;
        }


        @Override
        public GrpcClient buildClient(String host, int port) throws ClientFatalException {
            //netty
            NettyChannelBuilder channelBuilder = NettyChannelBuilder.forAddress(host, port);

            if (proxyConfig != null) {
                channelBuilder = channelBuilder.proxyDetector(new ProxyDetector() {
                    @Override
                    public ProxiedSocketAddress proxyFor(SocketAddress targetServerAddress) throws IOException {
                        HttpConnectProxiedSocketAddress.Builder builder = HttpConnectProxiedSocketAddress.newBuilder().setProxyAddress(new InetSocketAddress(proxyConfig.getHost(), proxyConfig.getPort())).setTargetAddress((InetSocketAddress) targetServerAddress);
                        if (proxyConfig.getUsername() != null) {
                            builder = builder.setUsername(proxyConfig.getUsername());
                        }
                        if (proxyConfig.getPassword() != null) {
                            builder = builder.setPassword(proxyConfig.getPassword());
                        }
                        return builder.build();
                    }
                });
            }

            if (trustManagerFactory != null) {
                try {
                    channelBuilder = channelBuilder.sslContext(GrpcSslContexts.forClient().trustManager(trustManagerFactory).build());
                } catch (SSLException e) {
                    throw new ClientFatalException("Failed to init client SSL");
                }
            }
            ManagedChannel channel = channelBuilder.build();


            TraceCollectorGrpc.TraceCollectorBlockingStub stub = TraceCollectorGrpc.newBlockingStub(channel);
            if (!"none".equals(compression)) {
                stub = stub.withCompression(compression);
            }

            return new GrpcClient(stub);
        }
    }

    private static class GrpcHostIdManager {
        private HostId localHostId;
        private Collector.HostID grpcHostID;
        private Collector.HostID grpcHostnameOnlyHostID;
        private String localHostname;

        private GrpcHostIdManager() {
        }

        private Collector.HostID getHostID() {
            HostId hostId = getHostId();
            boolean loadGrpcHostId;
            if (hostId == localHostId || hostId.equals(localHostId)) {
                loadGrpcHostId = grpcHostID == null;
            } else {
                localHostId = hostId;
                loadGrpcHostId = true;
            }

            if (loadGrpcHostId) {
                grpcHostID = toGrpcHostID(localHostId);
            }

            return grpcHostID;
        }

        private Collector.HostID getHostnameOnlyHostID() {
            String hostname = HostInfoUtils.getHostName();
            boolean loadGrpcHostId;
            if (hostname.equals(localHostname)) {
                loadGrpcHostId = grpcHostnameOnlyHostID == null;
            } else {
                localHostname = hostname;
                loadGrpcHostId = true;
            }

            if (loadGrpcHostId) {
                grpcHostnameOnlyHostID = toGrpcHostnameOnlyHostID(localHostname);
            }
            return grpcHostnameOnlyHostID;
        }

        private static Collector.HostID toGrpcHostID(HostId hostId) {
            Collector.HostID.Builder builder = Collector.HostID.newBuilder();

            if (hostId.getHostname() != null) {
                builder.setHostname(hostId.getHostname());
            }

            if (hostId.getEc2InstanceId() != null) {
                builder.setEc2InstanceID(hostId.getEc2InstanceId());
            }

            if (hostId.getEc2AvailabilityZone() != null) {
                builder.setEc2AvailabilityZone(hostId.getEc2AvailabilityZone());
            }

            if (hostId.getDockerContainerId() != null) {
                builder.setDockerContainerID(hostId.getDockerContainerId());
            }

            if (hostId.getHerokuDynoId() != null) {
                builder.setHerokuDynoID(hostId.getHerokuDynoId());
            }

            if (hostId.getAzureAppServiceInstanceId() != null) {
                builder.setAzAppServiceInstanceID(hostId.getAzureAppServiceInstanceId());
            }

            if (hostId.getUamsClientId() != null) {
                builder.setUamsClientID(hostId.getUamsClientId());
            }

            if (hostId.getUuid() != null) {
                builder.setUuid(hostId.getUuid());
            }

            if (hostId.getAzureVmMetadata() != null) {
                builder.setAzureMetadata(hostId.getAzureVmMetadata().toGrpc());
            }

            if (hostId.getAwsMetadata() != null) {
                builder.setAwsMetadata(hostId.getAwsMetadata().toGrpc());
            }

            return builder
                    .setHostType(Collector.HostType.valueOf(hostId.getHostType().name()))
                    .addAllMacAddresses(hostId.getMacAddresses())
                    .setPid(hostId.getPid())
                    .build();
        }

        /**
         * For getSettings call, we decided to fill in `hostname` only for `HostID` for consistency with other agent implementation.
         * <p>
         * Unlike other language agent, this change does not give any performance boost to java agent.
         *
         * @return
         */
        private static Collector.HostID toGrpcHostnameOnlyHostID(String hostname) {
            return Collector.HostID.newBuilder()
                    .setHostname(hostname)
                    .build();
        }
    }
}