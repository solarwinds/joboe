package com.solarwinds.joboe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import com.solarwinds.joboe.rpc.Client;
import com.solarwinds.joboe.rpc.Result;
import com.solarwinds.joboe.rpc.ResultCode;
import com.solarwinds.joboe.rpc.SettingsResult;
import com.solarwinds.joboe.settings.Settings;
import lombok.Getter;

public class TestRpcClient implements Client {
    private final long delay;
    
    @Getter
    private final List<Event> postedEvents = new ArrayList<Event>();
    @Getter
    private final List<Map<String, Object>> postedMetrics = new ArrayList<Map<String, Object>>();
    @Getter
    private final List<Map<String, Object>> postedStatus = new ArrayList<Map<String, Object>>();
    
    private final Result stringResult;
    private final SettingsResult settingsResult;
    
    public TestRpcClient(long delay) {
        this(delay, ResultCode.OK);
    }
    
    public TestRpcClient(long delay, ResultCode resultCode) {
        super();
        this.delay = delay;
        stringResult = new Result(resultCode, "", "");
        settingsResult = new SettingsResult(resultCode, "", "", Collections.emptyList());
    }

    public Future<Result> postEvents(List<Event> events, Callback<Result> callback) {
        sleep();
        postedEvents.addAll(events);
        return CompletableFuture.completedFuture(stringResult);
    }

    public Future<Result> postMetrics(List<Map<String, Object>> messages, Callback<Result> callback) {
        sleep();
        postedMetrics.addAll(messages);
        return CompletableFuture.completedFuture(stringResult);
    }

    public Future<Result> postStatus(List<Map<String, Object>> messages, Callback<Result> callback) {
        sleep();
        postedStatus.addAll(messages);
        return CompletableFuture.completedFuture(stringResult);
    }

    public Future<SettingsResult> getSettings(String version, Callback<SettingsResult> callback) {
        sleep();
        return CompletableFuture.completedFuture(settingsResult);
    }
    
    public void close() {
    }
    
    public Status getStatus() {
        return Status.OK;
    }


    private void sleep() {
        if (delay > 0) {
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    
    public void reset() {
        postedEvents.clear();
        postedMetrics.clear();
        postedStatus.clear();
    }
}