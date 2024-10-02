package com.appoptics.test.action;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Controller
@RequestMapping(value = "/test-get")
public class TestGet extends BaseTestAction {

    @Override
    protected HttpRequest buildRequest(HttpRequest.Builder builder) {
        return builder.GET().build();
    }
}
