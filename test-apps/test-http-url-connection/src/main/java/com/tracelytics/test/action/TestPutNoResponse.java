package com.tracelytics.test.action;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.util.Random;


@SuppressWarnings("serial")
@org.apache.struts2.convention.annotation.Results({
    @org.apache.struts2.convention.annotation.Result(name="success", location="index.jsp"),
    @org.apache.struts2.convention.annotation.Result(name="error", location="index.jsp"),
})
public class TestPutNoResponse extends BaseTestAction {
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
    protected void preConnect(HttpURLConnection connection) throws IOException {
        connection.setRequestMethod("PUT");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
    }
    
    @Override
    protected void writePayload(HttpURLConnection connection) throws IOException {
        connection.setFixedLengthStreamingMode(workloadString.getBytes().length);
        OutputStream out = connection.getOutputStream();
        out.write(workloadString.getBytes());
        out.close();
    }
    
    @Override
    protected void readResponse(HttpURLConnection connection) throws IOException {
        //do not trigger getInputStream
        printToOutput("PUT request w/o triggering getInputStream");
    }   

}
