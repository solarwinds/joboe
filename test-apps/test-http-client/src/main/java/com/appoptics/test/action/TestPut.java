package com.appoptics.test.action;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import java.net.URI;
import java.net.http.HttpRequest;


@Controller
@RequestMapping("/test-put")
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
    protected HttpRequest buildRequest(HttpRequest.Builder builder) {
        return builder.PUT(HttpRequest.BodyPublishers.ofString(workloadString)).build();
    }

}
