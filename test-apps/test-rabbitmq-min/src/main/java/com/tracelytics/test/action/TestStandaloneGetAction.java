package com.tracelytics.test.action;

import com.appoptics.api.ext.TraceContext;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.GetResponse;
import org.apache.struts2.convention.annotation.ParentPackage;

import java.io.IOException;

@SuppressWarnings("serial")
@org.apache.struts2.convention.annotation.Results({
    @org.apache.struts2.convention.annotation.Result(name="success", type="json"),
    @org.apache.struts2.convention.annotation.Result(name="error", type="json"),
})
@ParentPackage("json-default")
public class TestStandaloneGetAction extends AbstractTransientChannelAction {
    @Override
    public String execute(Channel channel) throws IOException {
        TraceContext context = TraceContext.getDefault();
        TraceContext.clearDefault();
        GetResponse getResponse = channel.basicGet(StartBasicGetQueueAction.QUEUE_NAME, true);
        context.setAsDefault();

        if (getResponse != null) {
            WebsocketWriter.write("consume", String.format("Basic get (standalone) on routing key [%s] with message [%s] from direct exchange [%s], queue [%s]", getResponse.getEnvelope().getRoutingKey(), new String(getResponse.getBody()), getResponse.getEnvelope().getExchange(), StartBasicGetQueueAction.QUEUE_NAME));
        } else {
            WebsocketWriter.write("consume", String.format("Basic get (standalone) yields no message from queue [%s]", StartBasicGetQueueAction.QUEUE_NAME));
        }
        return SUCCESS;
    }
}
