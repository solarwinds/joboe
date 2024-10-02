package com.tracelytics.joboe.rpc;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import com.tracelytics.joboe.Event;
import com.tracelytics.joboe.HostId;

/**
 * RPC client that provides various api methods to our collector
 * 
 * @author pluk
 *
 */
public interface Client {
//    Future<ResultCode> log(String...messages);
    Future<Result> postEvents(List<Event> events, Callback<Result> callback) throws ClientException;
    Future<Result> postMetrics(List<Map<String, Object>> messages, Callback<Result> callback) throws ClientException;
    Future<Result> postStatus(List<Map<String, Object>> messages, Callback<Result> callback) throws ClientException;
    Future<SettingsResult> getSettings(String version, Callback<SettingsResult> callback) throws ClientException;
    
    void close();
    Status getStatus();
    
    public enum ClientType { THRIFT, GRPC }
    public enum Status {
        NOT_CONNECTED, OK, FAILURE;
    }
    
    public static interface Callback<T> {
        void complete(T result);
        void fail(Exception e);
    }
}
