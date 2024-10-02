package com.tracelytics.test.action;

import java.io.IOException;
import java.net.HttpURLConnection;

import com.opensymphony.xwork2.ActionSupport;

import okhttp3.Request;


@SuppressWarnings("serial")
public class Index extends ActionSupport  {
    @Override
    public String execute() throws Exception {
        return SUCCESS;
    }
}
