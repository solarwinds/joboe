package com.solarwinds.joboe;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.solarwinds.joboe.rpc.Client;
import com.solarwinds.joboe.rpc.ClientException;
import com.solarwinds.joboe.rpc.Result;
import com.solarwinds.joboe.rpc.SettingsResult;

public class TestExecutionExceptionRpcClient implements Client {
    private final ExecutorService service = Executors.newSingleThreadExecutor();
    
    @Override
    public Future<Result> postEvents(List<Event> events, Callback<Result> callback) {
        return service.submit(new ExceptionCallable<Result>());
    }

    @Override
    public Future<Result> postMetrics(List<Map<String, Object>> messages, Callback<Result> callback) {
        return service.submit(new ExceptionCallable<Result>());
    }

    @Override
    public Future<Result> postStatus(List<Map<String, Object>> messages, Callback<Result> callback) {
        return service.submit(new ExceptionCallable<Result>());
    }

    @Override
    public Future<SettingsResult> getSettings(String version, Callback<SettingsResult> callback) {
        return service.submit(new ExceptionCallable<SettingsResult>());
    }

    private class ExceptionCallable<T> implements Callable<T> {
        @Override
        public T call() throws Exception {
            throw new ClientException("testing exception from exception rpc client");
        }
    }
    
    @Override
    public Status getStatus() {
        return Status.OK;
    }
    
    @Override
    public void close() {
        service.shutdown();
    }
}