package com.tracelytics.test.springboot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

@Controller
public class GreetingController {
    private Logger logger = LoggerFactory.getLogger(TestSpringBootApplication.class);
    @GetMapping(value = "/greeting", produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    public String greeting(@RequestParam(name="name", required=false, defaultValue="World") String name, @RequestHeader MultiValueMap<String, String> headers, Model model) {
        logger.info("Received request with name " + name);

        //do an outbound http call
        try {
            getURI("http://localhost:9000");
        } catch (IOException e) {
            e.printStackTrace();
        }

        return "greeting";
    }

    private static void getURI(String target) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(target).openConnection();
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(connection.getInputStream()))) {

            StringBuilder response = new StringBuilder();
            String line;

            while ((line = in.readLine()) != null) {
                response.append(line);
            }

            //print result
            System.out.println(response.toString());
        }
    }
}