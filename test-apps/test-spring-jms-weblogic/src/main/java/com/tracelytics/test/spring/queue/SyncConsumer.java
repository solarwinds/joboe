package com.tracelytics.test.spring.queue;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.TextMessage;

@Component
public class SyncConsumer {
    @Autowired
    private JmsTemplate jmsTemplate;

    public String consumeQueue(String queue) {
        String ans = "No message found!!";
        try {
            jmsTemplate.setReceiveTimeout(4);
            Message m = jmsTemplate.receive(queue);
            if (m instanceof TextMessage) {
                ans = ((TextMessage) m).getText();
            }
        } catch (JMSException ex) {
            ex.printStackTrace();
        }
        return "**"+ ans + "** from " + queue;
    }
}
