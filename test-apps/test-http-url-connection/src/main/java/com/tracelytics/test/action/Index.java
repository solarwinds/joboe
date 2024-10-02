package com.tracelytics.test.action;

import java.io.IOException;
import java.net.HttpURLConnection;


@SuppressWarnings("serial")
public class Index extends BaseTestAction {
    @Override
    public String execute() throws Exception {
        return SUCCESS;
    }

    @Override
    protected void preConnect(HttpURLConnection connection) throws IOException {
        // TODO Auto-generated method stub
        
    }
}
