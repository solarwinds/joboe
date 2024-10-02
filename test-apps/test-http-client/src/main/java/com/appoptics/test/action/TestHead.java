package com.appoptics.test.action;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import java.net.URI;
import java.net.http.HttpRequest;


@Controller
@RequestMapping("/test-head")
public class TestHead extends BaseTestAction {
    @Override
    protected HttpRequest buildRequest(HttpRequest.Builder builder) {
        return builder.method("HEAD", HttpRequest.BodyPublishers.noBody()).build();
   }
}
