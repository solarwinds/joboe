package com.tracelytics.test.spring.controller;

import com.tracelytics.test.spring.queue.QueueSender;
import com.tracelytics.test.spring.queue.SyncConsumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class JMSConsume {
    @Autowired
    SyncConsumer consumer;

    @RequestMapping(value="/consume", method = RequestMethod.GET)
    public String consume(@RequestParam(defaultValue = "jms/TestJMSQueue") String queue) {
        return consumer.consumeQueue(queue);
    }
}
