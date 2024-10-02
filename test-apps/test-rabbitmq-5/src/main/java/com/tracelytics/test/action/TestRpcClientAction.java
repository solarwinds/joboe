package com.tracelytics.test.action;

import com.rabbitmq.client.*;
import org.apache.struts2.convention.annotation.ParentPackage;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

@SuppressWarnings("serial")
@org.apache.struts2.convention.annotation.Results({
    @org.apache.struts2.convention.annotation.Result(name="success", type="json"),
    @org.apache.struts2.convention.annotation.Result(name="error", type="json"),
})
@ParentPackage("json-default")
public class TestRpcClientAction extends AbstractTransientChannelAction {
    @Override
    public String execute(Channel channel) throws IOException, ShutdownSignalException, ConsumerCancelledException, InterruptedException {
        RpcClient client = new RpcClient(new RpcClientParams().channel(channel).exchange(EXCHANGE_DIRECT).routingKey(mqForm.getRoutingKey()));

        WebsocketWriter.write("publish",  String.format("Publishing RPC call routing key [%s] with message [%s] to direct exchange [%s]." , mqForm.getRoutingKey(), mqForm.getMessage(), EXCHANGE_DIRECT));
        try {
            String response = client.stringCall(mqForm.getMessage());
            WebsocketWriter.write("publish", "RPC client received from reply queue the response [" + response + "]");
        } catch (TimeoutException e) {
            WebsocketWriter.write("publish",  String.format("Timeout while waiting for RPC call routing key [%s] with message [%s] to direct exchange [%s]." , mqForm.getRoutingKey(), mqForm.getMessage(), EXCHANGE_DIRECT));
        }
        return SUCCESS;
    }
}
