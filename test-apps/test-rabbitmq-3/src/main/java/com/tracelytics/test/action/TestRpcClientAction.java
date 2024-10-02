package com.tracelytics.test.action;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import org.apache.struts2.ServletActionContext;
import org.apache.struts2.convention.annotation.ParentPackage;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.ConsumerCancelledException;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.RpcClient;
import com.rabbitmq.client.ShutdownSignalException;
import com.tracelytics.test.WebsocketOutputServer;

@SuppressWarnings("serial")
@org.apache.struts2.convention.annotation.Results({
    @org.apache.struts2.convention.annotation.Result(name="success", type="json"),
    @org.apache.struts2.convention.annotation.Result(name="error", type="json"),
})
@ParentPackage("json-default")
public class TestRpcClientAction extends AbstractTransientChannelAction {
    @Override
    public String execute(Channel channel) throws IOException, ShutdownSignalException, ConsumerCancelledException, InterruptedException {
        RpcClient client = new RpcClient(channel, EXCHANGE_DIRECT, mqForm.getRoutingKey());
        
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
