package com.appoptics.test.testspringboot;

import com.appoptics.api.ext.LogMethod;
import com.appoptics.api.ext.ProfileMethod;
import com.appoptics.api.ext.Trace;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.extension.annotations.WithSpan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestMapping;



import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.Random;

@RestController
public class GreetingController {
    private static final String GET_URL = "http://localhost:8080/listHeaders";

    private Logger logger = LoggerFactory.getLogger(TestSpringBootApplication.class);
    @GetMapping("/greeting")
    public String greeting(@RequestParam(name="name", required=false, defaultValue="World") String name, Model model) {
        logger.info("Received request with name " + name);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        model.addAttribute("name", name);
        return "greeting";
    }
    
    @GetMapping("/listHeaders")
    public ResponseEntity<String> listAllHeaders(
            @RequestHeader Map<String, String> headers) {
        StringBuilder sb = new StringBuilder();

        headers.forEach((key, value) -> {
            sb.append(String.format("---Header '%s' = %s", key, value));
            sb.append("\n");
        });
        
        return new ResponseEntity<String>(
                sb.toString(), HttpStatus.OK);
    }

    @GetMapping("/sdk")
    public String sdk(@RequestParam(name="name", required=false, defaultValue="World") String name, Model model) {
        System.out.println(Span.current());
        System.out.println(Span.current().getSpanContext());
        model.addAttribute("name", name);

        Trace.setTransactionName("my-sdk-transaction");
        try {
            myMethod();
        } catch (Throwable e) {
            e.printStackTrace();
        }
        myMethod2();

        return "greeting";
    }

    @Deprecated
    @ProfileMethod(profileName = "ao-profile")
    @WithSpan(value="from-ot")
    @LogMethod(layer="my-layer", backTrace = true, storeReturn = true)
    private int myMethod() {
        return new Random().nextInt();
    }

    private int myMethod2() {
        return new Random().nextInt();
    }


    @RequestMapping(value = "/http-call", method = RequestMethod.GET)
    public String httpCall() {
        StringBuffer response = new StringBuffer();

        try {
            URL obj = new URL(GET_URL);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("GET");
            int responseCode = con.getResponseCode();
            System.out.println("GET Response Code :: " + responseCode);
            if (responseCode == HttpURLConnection.HTTP_OK) { // success
                BufferedReader in = new BufferedReader(new InputStreamReader(
                        con.getInputStream()));
                String inputLine;

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                    response.append("\n");
                }
                in.close();

                // print result
//                System.out.println(response.toString());
            } else {
                System.out.println("GET request not worked");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "=======\n\n" + response.toString() + "\n\n======";
    }
}