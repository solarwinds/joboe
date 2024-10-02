package com.tracelytics.test.spring.queue;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.JmsException;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

@Component
public class QueueSender {
    @Autowired
    private JmsTemplate jmsTemplate;

    public void sendToQueue(final String message) {
        // instead of lambda you can just use new MessageCreator() (as an anonymous class)
//        MessageCreator messageCreator = (session) -> session.createTextMessage(message);
        MessageCreator messageCreator = new MessageCreator() {
            @Override
            public Message createMessage(Session session) throws JMSException {
                return session.createTextMessage(message);
            }
        };
        try {
            jmsTemplate.send("jms/TestJMSQueue", messageCreator);
        } catch (JmsException ex) {
            ex.printStackTrace();
        }
    }
}