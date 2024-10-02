package com.tracelytics.test.spring.queue;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

public class QueueConsumer implements MessageListener {

    @Override
    public void onMessage(Message message) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(new File("message.txt")))) {
            bw.write(((TextMessage) message).getText());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}