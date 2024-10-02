package com.tracelytics.test.action;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConfirmListener;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.ReturnListener;


@SuppressWarnings("serial")
public abstract class AbstractTransientChannelAction extends AbstractMqAction {
    @Override
    public String execute() {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(mqForm.getHost());
        factory.setPort(mqForm.getPort());
        Connection connection = null;
        Channel channel = null;
        try {
            connection = factory.newConnection();
            channel = connection.createChannel();

            Thread.sleep(500); //a sleep so it triggers tracing on NR
            
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
            } catch (TimeoutException e) {
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
