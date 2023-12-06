package com.solarwinds.joboe;

import com.solarwinds.joboe.rpc.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;

public class TestSubmitRejectionRpcClient implements Client {
    public Future<Result> postEvents(List<Event> events, Callback<Result> callback) throws ClientException {
        throw new ClientRejectedExecutionException(new RejectedExecutionException("Testing submit client exception"));
    }

    public Future<Result> postMetrics(List<Map<String, Object>> messages, Callback<Result> callback) throws ClientException {
        throw new ClientRejectedExecutionException(new RejectedExecutionException("Testing submit client exception"));
    }

    public Future<Result> postStatus(List<Map<String, Object>> messages, Callback<Result> callback) throws ClientException {
        throw new ClientRejectedExecutionException(new RejectedExecutionException("Testing submit client exception"));
    }

    public Future<SettingsResult> getSettings(String version, Callback<SettingsResult> callback) throws ClientException {
        throw new ClientRejectedExecutionException(new RejectedExecutionException("Testing submit client exception"));
    }
    
    public void close() {
    }
    
    public Status getStatus() {
        return Status.OK;
    }
}