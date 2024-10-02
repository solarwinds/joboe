package com.tracelytics.test.action;

import java.io.IOException;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;


@SuppressWarnings("serial")
public abstract class AbstractTransientChannelAction extends AbstractMqAction {
    @Override
    public String execute() {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(mqForm.getHost());
        Connection connection = null;
        Channel channel = null;
        try {
            connection = factory.newConnection();
            channel = connection.createChannel();
            
            return execute(channel);
        } catch (Exception e) {
            e.printStackTrace();
            printToOutput(e.getMessage(), e.getStackTrace());
            return ERROR;
        } finally {
            try {
                if (channel != null) {
                    channel.close();
                }
                if (connection != null) {
                    connection.close();
                }
            } catch (IOException e) {
                printToOutput(e.getMessage(), e.getStackTrace());
            }
        }
    }
    
    
   
    protected abstract String execute(Channel channel) throws Exception;
    /**
     * Overrides this method if response message is to be replied via json
     * @return
     */
    public String getResponseMessage() {
        return null; 
    }
}
