package com.tracelytics.test.action;

import java.io.IOException;
import java.net.HttpURLConnection;

import okhttp3.Request;


@SuppressWarnings("serial")
@org.apache.struts2.convention.annotation.Results({
    @org.apache.struts2.convention.annotation.Result(name="success", location="index.jsp"),
    @org.apache.struts2.convention.annotation.Result(name="error", location="index.jsp"),
})
public class TestHead extends BaseTestAction {
    @Override
    protected Request buildRequest(String urlString) {
        return new Request.Builder().url(urlString).head().build();
    }
}
