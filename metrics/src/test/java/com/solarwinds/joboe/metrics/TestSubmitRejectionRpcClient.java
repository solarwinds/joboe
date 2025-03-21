package com.solarwinds.joboe.metrics;

import com.solarwinds.joboe.core.Event;
import com.solarwinds.joboe.core.rpc.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;

public class TestSubmitRejectionRpcClient implements Client {
    @Override
    public Future<Result> postEvents(List<Event> events, Callback<Result> callback) throws ClientException {
        throw new ClientRejectedExecutionException(new RejectedExecutionException("Testing submit client exception"));
    }

    @Override
    public Future<Result> postMetrics(List<Map<String, Object>> messages, Callback<Result> callback) throws ClientException {
        throw new ClientRejectedExecutionException(new RejectedExecutionException("Testing submit client exception"));
    }

    @Override
    public Future<Result> postStatus(List<Map<String, Object>> messages, Callback<Result> callback) throws ClientException {
        throw new ClientRejectedExecutionException(new RejectedExecutionException("Testing submit client exception"));
    }

    @Override
    public Future<SettingsResult> getSettings(String version, Callback<SettingsResult> callback) throws ClientException {
        throw new ClientRejectedExecutionException(new RejectedExecutionException("Testing submit client exception"));
    }
    
    @Override
    public void close() {
    }
    
    @Override
    public Status getStatus() {
        return Status.OK;
    }
}