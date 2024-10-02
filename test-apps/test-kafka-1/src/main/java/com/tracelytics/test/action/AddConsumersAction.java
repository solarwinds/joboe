package com.tracelytics.test.action;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import kafka.consumer.ConsumerIterator;
import kafka.consumer.KafkaStream;

import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.struts2.ServletActionContext;
import org.apache.struts2.convention.annotation.ParentPackage;

@SuppressWarnings("serial")

@org.apache.struts2.convention.annotation.Results({
    @org.apache.struts2.convention.annotation.Result(name="success", type="json"),
    @org.apache.struts2.convention.annotation.Result(name="error", type="json"),
})
@ParentPackage("json-default")
public class AddConsumersAction extends AbstractMqAction {
    private static Consumer<String, String> createKafkaConsumer(String brokerList, String consumerGroupId) {
        Properties properties = new Properties();
        properties.put("group.id",  consumerGroupId);
        properties.put("bootstrap.servers", brokerList);
        properties.put("partition.assignment.strategy", RoundRobinAssignor.class.getName());
        properties.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        properties.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        return new KafkaConsumer<String, String>(properties);
    }




    public static void main(String[] args) throws Exception {
        Consumer<String, String> consumer = createKafkaConsumer("localhost:9092", "test");
        ConsumerRecords<String, String> messages;
        while (!(messages = consumer.poll(0)).isEmpty()) {
            for (ConsumerRecord<String, String> message : messages) {
                String topic = message.topic();
                System.out.println("[" + topic + "] [" + message.key() + "]" + message.value());
            }
        }
    }


    @Override
    public String execute() throws Exception {
        final String consumerGroupId = mqForm.getConsumerGroupId();
        final String topic = mqForm.getTopic();
        final Consumer<String, String> consumer = createKafkaConsumer(mqForm.getHost() + ":" + mqForm.getPort(), consumerGroupId);
        consumer.subscribe(Collections.singletonList(topic));
        WebsocketWriter.write("consume", "Created consumer group [" + consumerGroupId + "] for topic [" + topic + "]");

        final String sessionId = ServletActionContext.getRequest().getSession().getId();

        new Thread() { //TODO rewrite
            public void run() {
                ConsumerRecords<String, String> messages;
                while (true) {
                    messages = consumer.poll(1000);
                    for (ConsumerRecord<String, String> message : messages) {
                        processMessage(message);
                        String topic = message.topic();
                        try {
                            WebsocketWriter.write(sessionId, "consume", "consumer from group [" + consumerGroupId + "] on topic [" + topic + "] got message [" + new String(message.value()) + "]");
                        } catch (Exception e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                    consumer.commitAsync();
                }
            }
        }.start();


        return SUCCESS;
    }

//    @Trace(dispatcher = true)
    private static void processMessage(ConsumerRecord<String, String> rec){
        Iterable<Header> headers = rec.headers().headers("newrelic");
        for(Header header: headers) {
            NewRelic.getAgent().getTransaction().acceptDistributedTracePayload(new String(header.value(), StandardCharsets.UTF_8));
        }

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
