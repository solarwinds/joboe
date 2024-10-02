package com.tracelytics.test.action;

import java.io.IOException;

import org.apache.struts2.ServletActionContext;
import org.apache.struts2.convention.annotation.ParentPackage;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.MessageProperties;
import com.rabbitmq.client.ReturnListener;
import com.tracelytics.test.WebsocketOutputServer;

@SuppressWarnings("serial")
@org.apache.struts2.convention.annotation.Results({
    @org.apache.struts2.convention.annotation.Result(name="success", type="json"),
    @org.apache.struts2.convention.annotation.Result(name="error", type="json"),
})
@ParentPackage("json-default")
public class TestDirectMandatoryPublishAction extends AbstractTransientChannelAction {
    private boolean success;
    @Override
    public String execute(Channel channel) throws IOException, InterruptedException {
        channel.exchangeDeclare(EXCHANGE_DIRECT, "direct");

        success = true;
        channel.addReturnListener(new ReturnListener() {
            @Override
            public void handleReturn(int replyCode,
                    String replyText,
                    String exchange,
                    String routingKey,
                    AMQP.BasicProperties properties,
                    byte[] body)
                throws IOException {
                success = false;
            }
            
        });
        
        channel.confirmSelect();
        channel.basicPublish(EXCHANGE_DIRECT, mqForm.getRoutingKey(), true, false, MessageProperties.PERSISTENT_BASIC, mqForm.getMessage().getBytes()); //delivery as mandatory message
        channel.waitForConfirms();
        
        if (success) {
            WebsocketWriter.write("publish", String.format("Published routing key [%s] with message [%s] to direct exchange [%s]", mqForm.getRoutingKey(), mqForm.getMessage(), EXCHANGE_DIRECT));
        } else {
            WebsocketWriter.write("publish", String.format("Failed to publish routing key [%s] with message [%s] to direct exchange [%s] in mandatory mode. Either the routing key is not valid or no consumer is available!", mqForm.getRoutingKey(), mqForm.getMessage(), EXCHANGE_DIRECT));
        }
        
        return SUCCESS;
    }
}
