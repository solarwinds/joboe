package com.tracelytics.test.action;

import java.io.IOException;

import org.apache.struts2.ServletActionContext;
import org.apache.struts2.convention.annotation.ParentPackage;

import com.rabbitmq.client.Channel;
import com.tracelytics.test.WebsocketOutputServer;

@SuppressWarnings("serial")
@org.apache.struts2.convention.annotation.Results({
                                                   @org.apache.struts2.convention.annotation.Result(name = "success", type = "json"),
                                                   @org.apache.struts2.convention.annotation.Result(name = "error", type = "json"),
})
@ParentPackage("json-default")
public class TestFanoutPublishAction extends AbstractTransientChannelAction {
    @Override
    public String execute(Channel channel) throws IOException {
        channel.exchangeDeclare(EXCHANGE_FANOUT, "fanout");

        channel.basicPublish(EXCHANGE_FANOUT, "", null, mqForm.getMessage().getBytes());

        WebsocketWriter.write("publish", String.format("Published message [%s] to fanout exchange", mqForm.getMessage(), EXCHANGE_FANOUT));
        return SUCCESS;
    }

}
