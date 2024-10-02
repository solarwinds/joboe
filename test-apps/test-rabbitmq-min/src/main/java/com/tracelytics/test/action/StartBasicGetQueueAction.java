package com.tracelytics.test.action;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import org.apache.struts2.convention.annotation.ParentPackage;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("serial")
@org.apache.struts2.convention.annotation.Results({
    @org.apache.struts2.convention.annotation.Result(name="success", type="json"),
    @org.apache.struts2.convention.annotation.Result(name="error", type="json"),
})
@ParentPackage("json-default")
public class StartBasicGetQueueAction extends AbstractTransientChannelAction {
    static final String QUEUE_NAME = "basic-get-queue";
    @Override
    public String execute(Channel channel) throws IOException {
        Map<String, Object> args = new HashMap<String, Object>();
        args.put("x-max-length", 2);
        String queueName = channel.queueDeclare(QUEUE_NAME, true, false, false, args).getQueue();
        AMQP.Queue.BindOk bindOk = channel.queueBind(queueName, EXCHANGE_DIRECT, mqForm.getRoutingKey());
        WebsocketWriter.write("consume", String.format("Created queue [%s] with routing key [%s] bound to direct exchange [%s]", QUEUE_NAME, mqForm.getRoutingKey(), EXCHANGE_DIRECT));

        return SUCCESS;
    }
}
