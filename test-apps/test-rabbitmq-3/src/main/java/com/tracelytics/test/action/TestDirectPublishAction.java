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
public class TestDirectPublishAction extends AbstractTransientChannelAction {
    @Override
    public String execute(Channel channel) throws IOException {
        channel.exchangeDeclare(EXCHANGE_DIRECT, "direct");
        channel.basicPublish(EXCHANGE_DIRECT, mqForm.getRoutingKey(), null, mqForm.getMessage().getBytes());
        
        WebsocketWriter.write("publish", String.format("Published routing key [%s] with message [%s] to direct exchange [%s]", mqForm.getRoutingKey(), mqForm.getMessage(), EXCHANGE_DIRECT));
        return SUCCESS;
    }
}
