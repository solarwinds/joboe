package com.tracelytics.test.action;

import okhttp3.Request;


@SuppressWarnings("serial")
@org.apache.struts2.convention.annotation.Results({
    @org.apache.struts2.convention.annotation.Result(name="success", location="index.jsp"),
    @org.apache.struts2.convention.annotation.Result(name="error", location="index.jsp"),
})
public class TestDelete extends BaseTestAction {
    @Override
    protected Request buildRequest(String urlString) {
        return new Request.Builder().url(urlString).delete().build();
    }
    
}
