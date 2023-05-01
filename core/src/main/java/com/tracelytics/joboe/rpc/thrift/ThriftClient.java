package com.tracelytics.joboe.rpc.thrift;

import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.appoptics.ext.org.apache.thrift.TException;
import com.appoptics.ext.org.apache.thrift.protocol.TBinaryProtocol;
import com.appoptics.ext.org.apache.thrift.protocol.TProtocol;
import com.appoptics.ext.org.apache.thrift.transport.TSocket;
import com.appoptics.ext.org.apache.thrift.transport.TTransport;
import com.appoptics.ext.org.apache.thrift.transport.TZlibTransport;
import com.appoptics.ext.thriftgenerated.*;
import com.appoptics.ext.thriftgenerated.EncodingType;
import com.appoptics.ext.thriftgenerated.HostType;
import com.tracelytics.ext.apache.thrift.transport.TZlibTransport2;
import com.appoptics.ext.thriftgenerated.Collector.Client;
import com.tracelytics.joboe.BsonBufferException;
import com.tracelytics.joboe.Event;
import com.tracelytics.joboe.HostId;
import com.tracelytics.joboe.rpc.*;
import com.tracelytics.joboe.rpc.ResultCode;
import com.tracelytics.joboe.rpc.SettingsResult;
import com.tracelytics.joboe.settings.Settings;
import com.tracelytics.logging.Logger;
import com.tracelytics.logging.LoggerFactory;
import com.tracelytics.util.BsonUtils;
import com.tracelytics.util.HostInfoUtils;

/**
 * Collector protocol client that uses the Thrift protocol.
 *
 * Wraps the Thrift client generated from the collector.proto definition
 */
public class ThriftClient implements ProtocolClient {
    private static final Logger logger = LoggerFactory.getLogger();
    private final Client client; // the underlying thrift low level client

    ThriftClient(Client underlyingClient) {
        this.client = underlyingClient;
    }

    @Override
    public void shutdown() {
        if (client != null) {
            if (client.getInputProtocol() != null && client.getInputProtocol().getTransport() != null) {
                client.getInputProtocol().getTransport().close();
            }
            if (client.getOutputProtocol() != null && client.getOutputProtocol().getTransport() != null) {
                client.getOutputProtocol().getTransport().close();
            }
        }
    }

     @Override
    public Result doPostEvents(String serviceKey, List<Event> events) throws ThriftServerException, ThriftClientBufferException {
        return postInBatch(serviceKey, events, EVENT_SERIALIZER, POST_EVENTS_ACTION);
    }

    @Override
    public Result doPostMetrics(String serviceKey, List<Map<String, Object>> messages) throws ThriftServerException, ThriftClientBufferException {
        return postInBatch(serviceKey, messages, KEY_VALUE_MAP_SERIALIZER, POST_METRICS_ACTION);
    }

    @Override
    public Result doPostStatus(String serviceKey, List<Map<String, Object>> messages) throws ThriftServerException, ThriftClientBufferException {
        return postInBatch(serviceKey, messages, KEY_VALUE_MAP_SERIALIZER, POST_STATUS_ACTION);
    }

    private <T> Result postInBatch(String serviceKey, List<T> items, Serializer<T> serializer, PostAction postAction) throws ThriftClientBufferException, ThriftServerException {
        List<List<ByteBuffer>> itemsByCalls = new ArrayList<List<ByteBuffer>>();
        List<ByteBuffer> byteBuffers = new ArrayList<ByteBuffer>();
        itemsByCalls.add(byteBuffers);

        long callSize = 0;
        for (T item : items) {
            try {
                ByteBuffer itemByteBuffer = serializer.serialize(item);
                int size = itemByteBuffer.remaining();
                if (callSize + size > MAX_CALL_SIZE) {
                    byteBuffers = new ArrayList<ByteBuffer>();
                    itemsByCalls.add(byteBuffers);
                    callSize = size;
                } else {
                    callSize += size;
                }
                byteBuffers.add(itemByteBuffer);

            } catch (BsonBufferException e) {
                logger.warn("Failed to perform action [" + postAction.getDescription() +"] due to buffer exception : " + e.getMessage(), e);
                throw new ThriftClientBufferException(e);
            }
        }

        ResultMessage resultMessage = null;
        for (List<ByteBuffer> itemsAsByteBuffer : itemsByCalls) {
            logger.trace(postAction.getDescription() + " " + itemsAsByteBuffer.size() + " item(s) using thrift client " + ThriftClient.this);
            try {
                resultMessage = postAction.post(client, serviceKey, itemsAsByteBuffer);
            } catch (TException e) {
                throw new ThriftServerException(e);
            }
        }

        return new Result(resultMessage.getResult() != null ? ResultCode.valueOf(resultMessage.getResult().name()) : null, resultMessage.getArg(), resultMessage.getWarning());
    }

    @Override
    public SettingsResult doGetSettings(String serviceKey, String version) throws ThriftServerException {
        //For getSettings call, we decided to fill in `hostname` only for `HostID` for consistency with other agent implementation
        //Unlike other language agent, this change does not give any performance boost to java agent
        com.appoptics.ext.thriftgenerated.SettingsResult result;
        try {
            result = client.getSettings(serviceKey, getHostNameOnlyHostId(), version);
        } catch (TException e) {
            throw new ThriftServerException(e);
        }
        List<Settings> settings = new ArrayList<Settings>();
        if (result.getResult() != null && result.getResult() == com.appoptics.ext.thriftgenerated.ResultCode.OK) {
            for (OboeSetting oboeSetting : result.settings) {
                settings.add(new com.tracelytics.joboe.rpc.Settings((short)oboeSetting.getType().getValue(),
                        oboeSetting.getFlags(),
                        //oboeSetting.getTimestamp(),
                        System.currentTimeMillis(), //use local timestamp for now, as it is easier to compare ttl with it
                        oboeSetting.getValue(),
                        oboeSetting.getTtl(),
                        oboeSetting.getLayer(),
                        oboeSetting.getArguments()));
            }
        }

        return new SettingsResult(ResultCode.valueOf(result.getResult().name()), result.getArg(), result.getWarning(), settings);
    }

    @Override
    public void doPing(String serviceKey) throws ThriftServerException {
        try {
            client.ping(serviceKey);
        } catch (TException e) {
            throw new ThriftServerException(e);
        }
    }

    private static HostID getHostId() {
        HostId hostId = HostInfoUtils.getHostId();

        HostID thriftHostID = new HostID(hostId.getHostname(),
                null,
                null, hostId.getPid(),
                hostId.getEc2InstanceId(),
                hostId.getEc2AvailabilityZone(),
                hostId.getDockerContainerId(),
                hostId.getMacAddresses(),
                hostId.getHerokuDynoId(),
                hostId.getAzureAppServiceInstanceId(),
                HostType.valueOf(hostId.getHostType().name()));

        return thriftHostID;
    }
    
    private static HostID getHostNameOnlyHostId() {
        return new HostID(HostInfoUtils.getHostName(), null, null, 0, null, null, null, null, null, null, HostType.PERSISTENT);
    }

    interface Serializer<T> {
        ByteBuffer serialize(T item) throws BsonBufferException;
    }

    interface PostAction {
        ResultMessage post(Client client, String serviceKey, List<ByteBuffer> items) throws TException;
        String getDescription();
    }

    private static final Serializer<Map<String, Object>> KEY_VALUE_MAP_SERIALIZER = new Serializer<Map<String, Object>>() {
        @Override
        public ByteBuffer serialize(Map<String, Object> item) throws BsonBufferException {
            return BsonUtils.convertMapToBson(item, INITIAL_MESSAGE_SIZE, MAX_MESSAGE_SIZE);
        }
    };

    private static final Serializer<Event> EVENT_SERIALIZER = new Serializer<Event>() {
        @Override
        public ByteBuffer serialize(Event event) throws BsonBufferException {
            return event.toByteBuffer();
        }
    };

    private static final PostAction POST_EVENTS_ACTION = new PostAction() {
        @Override
        public ResultMessage post(Client client, String serviceKey, List<ByteBuffer> events) throws TException {
            return client.postEvents(serviceKey, events, EncodingType.BSON, getHostId());
        }

        @Override
        public String getDescription() {
            return "Post Events";
        }
    };

    private static final PostAction POST_STATUS_ACTION = new PostAction() {
        @Override
        public ResultMessage post(Client client, String serviceKey, List<ByteBuffer> messages) throws TException {
            return client.postStatus(serviceKey, messages, EncodingType.BSON, getHostId());
        }

        @Override
        public String getDescription() {
            return "Post Status Message";
        }
    };

    private static final PostAction POST_METRICS_ACTION = new PostAction() {
        @Override
        public ResultMessage post(Client client, String serviceKey, List<ByteBuffer> messages) throws TException {
            return client.postMetrics(serviceKey, messages, EncodingType.BSON, getHostId());
        }

        @Override
        public String getDescription() {
            return "Post Metrics";
        }
    };

    static class ThriftProtocolClientFactory implements ProtocolClientFactory<ThriftClient> {
        private final ThriftSslFactory sslFactory;
        private static final int TIMEOUT = 10 * 1000; //10 secs

        ThriftProtocolClientFactory(URL serverCertLocation, String explicitHostCheck) throws IOException, GeneralSecurityException {
            this.sslFactory = new ThriftSslFactory(serverCertLocation, explicitHostCheck);
        }
        @Override
        public ThriftClient buildClient(String host, int port) throws ThriftClientConnectException {
            Client underlyingClient = new Client(getThriftProtocol(host, port));
            return new ThriftClient(underlyingClient);
        }

        private TProtocol getThriftProtocol(String host, int port) throws ThriftClientConnectException {
            TTransport transport = sslFactory != null ? sslFactory.getSslTSocket(host, port, TIMEOUT) : new TSocket(host, port, TIMEOUT);

            //TProtocol protocol = new TBinaryProtocol(new TZlibTransport(transport));
            TProtocol protocol;
            TTransport compressedTransport;
            try {
                compressedTransport = new TZlibTransport(transport);
            } catch (NoSuchMethodError e) { //jdk 1.6-
                logger.debug("Running in jdk 1.6 or earlier, using TZlibTransport2 for backward compatability");
                try {
                    compressedTransport = new TZlibTransport2(transport);
                } catch (IOException e1) {
                    throw new ThriftClientConnectException(e);
                }
            }
            protocol = new TBinaryProtocol(compressedTransport);
            return protocol;
        }
    }
}

