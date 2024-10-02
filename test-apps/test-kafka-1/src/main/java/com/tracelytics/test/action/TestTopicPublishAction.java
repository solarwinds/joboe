package com.tracelytics.test.action;

import java.util.Properties;

import com.newrelic.api.agent.Trace;
import kafka.producer.KeyedMessage;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.LongSerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.struts2.convention.annotation.ParentPackage;

@SuppressWarnings("serial")
@org.apache.struts2.convention.annotation.Results({
    @org.apache.struts2.convention.annotation.Result(name="success", type="json"),
    @org.apache.struts2.convention.annotation.Result(name="error", type="json"),
})
@ParentPackage("json-default")
public class TestTopicPublishAction extends AbstractMqAction {
    @Override
    public String execute() throws Exception {
        ProducerRecord<String, String> producerRecord = new ProducerRecord<>(mqForm.getTopic(), mqForm.getMessageKey(), mqForm.getMessage());
        KafkaProducer<String, String> producer = createKafkaProducer(mqForm.getHost() + ":" + mqForm.getPort());

        publish(producer, producerRecord);

        Thread.sleep(1000); //so NR would capture trace
        WebsocketWriter.write("publish", "Published to topic [" + mqForm.getTopic() + "] message key [" + mqForm.getMessageKey() + "] message [" + mqForm.getMessage() + "]");
        
        return SUCCESS;
    }

//    @Trace(dispatcher = true)
    private void publish(Producer<String, String> producer, ProducerRecord<String, String> record) {
        producer.send(record);
    }
    
    private static KafkaProducer<String, String> createKafkaProducer(String brokerList) {
        Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, brokerList);
//        properties.put("metadata.broker.list",  brokerList);
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class.getName());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class.getName());
        
        return new KafkaProducer<String, String>(properties);

    }
}
