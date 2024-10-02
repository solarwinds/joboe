package com.tracelytics.test.action;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.util.Random;

import okhttp3.Request;
import okhttp3.RequestBody;


@SuppressWarnings("serial")
@org.apache.struts2.convention.annotation.Results({
    @org.apache.struts2.convention.annotation.Result(name="success", location="index.jsp"),
    @org.apache.struts2.convention.annotation.Result(name="error", location="index.jsp"),
})
public class TestPut extends BaseTestAction {
    private static String workloadString;
    
    static  {
//        try {
//            JSONObject object = new JSONObject();
//            byte[] byteArray = new byte[10000];
//            new Random().nextBytes(byteArray);
//            object.put("array", new JSONArray(byteArray));
//            workloadString = object.toString();
//        } catch (UnsupportedClassVersionError e) { //jdk 1.6-
            workloadString = "{ \"key1\" : 1, \"key2\" : \"abc\" }";
//        }
    }
    
    @Override
    protected Request buildRequest(String urlString) {
        RequestBody body = RequestBody.create(JSON, workloadString.getBytes());
        return new Request.Builder().url(urlString).post(body).build();
    }
}
