package com.solarwinds.joboe;

import com.solarwinds.joboe.rpc.Client;
import com.solarwinds.joboe.rpc.Result;
import com.solarwinds.joboe.rpc.ResultCode;
import com.solarwinds.joboe.rpc.SettingsResult;
import com.solarwinds.joboe.settings.Settings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public class TestRpcClient implements Client {
    private long delay;
    
    private List<Event> postedEvents = new ArrayList<Event>();
    private List<Map<String, Object>> postedMetrics = new ArrayList<Map<String, Object>>();
    private List<Map<String, Object>> postedStatus = new ArrayList<Map<String, Object>>();
    
    private Result stringResult;
    private SettingsResult settingsResult;
    
    public TestRpcClient(long delay) {
        this(delay, ResultCode.OK);
    }
    
    public TestRpcClient(long delay, ResultCode resultCode) {
        super();
        this.delay = delay;
        stringResult = new Result(resultCode, "", "");
        settingsResult = new SettingsResult(resultCode, "", "", Collections.<Settings>emptyList());
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
    
    
    public List<Event> getPostedEvents() {
        return postedEvents;
    }
    
    public List<Map<String, Object>> getPostedMetrics() {
        return postedMetrics;
    }
    
    public List<Map<String, Object>> getPostedStatus() {
        return postedStatus;
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