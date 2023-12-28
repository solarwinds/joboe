package com.solarwinds.joboe.core.rpc;

import com.solarwinds.joboe.core.Event;

import java.util.List;
import java.util.Map;

/**
 * Protocol (Thrift/gRPC etc) specific client that usually wraps and forward call to the underlying code generated client
 *
 * @see com.solarwinds.joboe.core.rpc.Client.ClientType
 */
public interface ProtocolClient {
    int MAX_CALL_SIZE = 4 * 1024 * 1024; //in bytes, an approximation
    int INITIAL_MESSAGE_SIZE = 64 * 1024; //starts with 64 kB per message
    int MAX_MESSAGE_SIZE = MAX_CALL_SIZE; //the max buffer used for the message can at most be the same as max call size
    /**
     * Shuts down this protocol client, should do cleanup to shutdown any underlying generated client/connection if applicable
     */
    void shutdown();

    Result doPostEvents(String serviceKey, List<Event> events) throws ClientException;
    Result doPostMetrics(String serviceKey, List<Map<String, Object>> messages) throws ClientException;
    Result doPostStatus(String serviceKey, List<Map<String, Object>> messages) throws ClientException;
    SettingsResult doGetSettings(String serviceKey, String version) throws ClientException;
    void doPing(String serviceKey) throws Exception;
}
