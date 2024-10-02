package com.tracelytics.test.action;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.*;
import org.apache.struts2.convention.annotation.ParentPackage;

import java.io.IOException;

@SuppressWarnings("serial")
@org.apache.struts2.convention.annotation.Results({
    @org.apache.struts2.convention.annotation.Result(name="success", type="json"),
    @org.apache.struts2.convention.annotation.Result(name="error", type="json"),
})
@ParentPackage("json-default")
public class TestDirectRpcAction extends AbstractTransientChannelAction {
    @Override
    public String execute(Channel channel) throws IOException, ShutdownSignalException, ConsumerCancelledException, InterruptedException {
        channel.exchangeDeclare(EXCHANGE_DIRECT, "direct");
        final String correlationId = java.util.UUID.randomUUID().toString();
        //bind reply consumer to the reply queue
        String replyQueueName = channel.queueDeclare().getQueue(); 
        //QueueingConsumer replyConsumer = new QueueingConsumer(channel);
        channel.basicConsume(replyQueueName, true, new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, BasicProperties properties, byte[] body) throws IOException {
                String incomingCorrelationId = properties.getCorrelationId();
                if (incomingCorrelationId.equals(correlationId)) {
                    WebsocketWriter.write("publish", "RPC client received from reply queue the response [" + new String(body) + "]");
                }
            }
        });
        
        BasicProperties props;
        props= new BasicProperties.Builder().correlationId(correlationId).replyTo(replyQueueName).build();
        channel.basicPublish(EXCHANGE_DIRECT, mqForm.getRoutingKey(), props, mqForm.getMessage().getBytes());
        
//            props= new BasicProperties.Builder().correlationId(java.util.UUID.randomUUID().toString()).replyTo(REPLY_QUEUE_NAME).build();
//            channel.basicPublish(EXCHANGE_NAME, mqForm.getRoutingKey(), props, mqForm.getMessage().getBytes()); //TODO publish twice and see what happens
        WebsocketWriter.write("publish",  String.format("Published RPC call routing key [%s] with message [%s] to direct exchange [%s]. Now waiting for response from the reply queue", mqForm.getRoutingKey(), mqForm.getMessage(), EXCHANGE_DIRECT));
        return SUCCESS;
    }
    
}
