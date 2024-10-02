package com.tracelytics.test.action;

import java.io.IOException;

import org.apache.struts2.ServletActionContext;
import org.apache.struts2.convention.annotation.ParentPackage;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.MessageProperties;
import com.tracelytics.test.WebsocketOutputServer;

@SuppressWarnings("serial")
@org.apache.struts2.convention.annotation.Results({
    @org.apache.struts2.convention.annotation.Result(name="success", type="json"),
    @org.apache.struts2.convention.annotation.Result(name="error", type="json"),
})
@ParentPackage("json-default")
public class TestDirectPublishTransactionModeAction extends AbstractTransientChannelAction {
    @Override
    public String execute(Channel channel) throws IOException, InterruptedException {
        channel.exchangeDeclare(EXCHANGE_DIRECT, "direct");
        
        channel.txSelect();
        channel.basicPublish(EXCHANGE_DIRECT, mqForm.getRoutingKey(), MessageProperties.PERSISTENT_BASIC, mqForm.getMessage().getBytes()); //delivery as mandatory message
        channel.txCommit();
        
        WebsocketWriter.write("publish", String.format("Published routing key [%s] with message [%s] to direct exchange [%s]", mqForm.getRoutingKey(), mqForm.getMessage(), EXCHANGE_DIRECT));
        return SUCCESS;
    }
    
}
