package com.appoptics.test.webflux;

import com.appoptics.api.ext.Trace;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.Arrays;
import java.util.List;

@RestController
public class RestHandler {
    @RequestMapping(value = "/name/{name}", method = RequestMethod.GET) public Flux<String> helloName(@PathVariable("name") String name) {
        return Flux.fromIterable(getStringList());
    }
    
    public List<String> getStringList() {
//        try {
//            Thread.sleep(20);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
        //SDK test span to ensure context is set properly
        Trace.createEntryEvent("sdk-test-span").report();
        Trace.createExitEvent("sdk-test-span").report();
        
        String[] array = { "one", "two", "three" };
        return Arrays.asList(array);
    }
}
