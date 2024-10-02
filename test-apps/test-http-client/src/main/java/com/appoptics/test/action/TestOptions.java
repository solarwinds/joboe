package com.appoptics.test.action;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import java.net.URI;
import java.net.http.HttpRequest;


@Controller
@RequestMapping("/test-options")
public class TestOptions extends BaseTestAction {
    @Override
    protected HttpRequest buildRequest(HttpRequest.Builder builder) {
        return builder.method("OPTIONS", HttpRequest.BodyPublishers.noBody()).build();
    }
}
