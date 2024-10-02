package com.tracelytics.joboe;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.tracelytics.joboe.rpc.Client;
import com.tracelytics.joboe.rpc.ClientException;
import com.tracelytics.joboe.rpc.Result;
import com.tracelytics.joboe.rpc.SettingsResult;
import com.tracelytics.joboe.rpc.Client.Callback;

public class TestExecutionExceptionRpcClient implements Client {
    private ExecutorService service = Executors.newSingleThreadExecutor();
    
    public Future<Result> postEvents(List<Event> events, Callback<Result> callback) {
        return service.submit(new ExceptionCallable<Result>());
    }

    public Future<Result> postMetrics(List<Map<String, Object>> messages, Callback<Result> callback) {
        return service.submit(new ExceptionCallable<Result>());
    }

    public Future<Result> postStatus(List<Map<String, Object>> messages, Callback<Result> callback) {
        return service.submit(new ExceptionCallable<Result>());
    }

    public Future<SettingsResult> getSettings(String version, Callback<SettingsResult> callback) {
        return service.submit(new ExceptionCallable<SettingsResult>());
    }
    
    private class ExceptionCallable<T> implements Callable<T> {
        public T call() throws Exception {
            throw new ClientException("testing exception from exception rpc client");
        }
    }
    
    public Status getStatus() {
        return Status.OK;
    }
    
    public void close() {
        service.shutdown();
    }
}