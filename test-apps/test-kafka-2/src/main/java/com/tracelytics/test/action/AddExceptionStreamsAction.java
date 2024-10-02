package com.tracelytics.test.action;

//import com.newrelic.api.agent.Trace;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.processor.Processor;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.struts2.ServletActionContext;
import org.apache.struts2.convention.annotation.ParentPackage;

import java.io.IOException;
import java.util.Properties;

@SuppressWarnings("serial")

@org.apache.struts2.convention.annotation.Results({
    @org.apache.struts2.convention.annotation.Result(name="success", type="json"),
    @org.apache.struts2.convention.annotation.Result(name="error", type="json"),
})
@ParentPackage("json-default")
public class AddExceptionStreamsAction extends AbstractMqAction {
    @Override
    public String execute() throws Exception {
        //final String consumerGroupId = mqForm.getConsumerGroupId();
        final String topic = mqForm.getTopic();

        KafkaStreams streams = getStreamsOnTopic(mqForm.getHost() + ":" + mqForm.getPort(), topic);

        WebsocketWriter.write("consume", "Created exception stream for topic [" + topic + "]");


        new Thread() { //TODO rewrite
            public void run() {
                streams.start();
            }
        }.start();


        return SUCCESS;
    }
    
    private static KafkaStreams getStreamsOnTopic(String hostList, String topic) {
        final String sessionId = ServletActionContext.getRequest().getSession().getId();

        StreamsBuilder builder = new StreamsBuilder();
        KStream<String, String> topicStream = builder.stream(topic);

        KStream<String, String> mappedStream = topicStream.map((key, value) -> {
            return processStreamMessage(topic, key, value, sessionId);
        });

        mappedStream.to(topic + "-output");

        
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "streams-sleep");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, hostList);
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());


        KafkaStreams streams = new KafkaStreams(builder.build(), props);

        return streams;
    }


//    @Trace(dispatcher = true)
    private static KeyValue<? extends String, ? extends String> processStreamMessage(String topic, String key, String value, String sessionId)  {
        try {
            System.out.println("Processing stream item " + key + " :" + value);
            int sleepTime;
            try {
                sleepTime = Integer.parseInt(value);
            } catch (NumberFormatException e) {
                sleepTime = 1000;
            }
            Thread.sleep(sleepTime);
            WebsocketWriter.write(sessionId, "consume", "Throw exception for stream processing on topic " + topic + " key " + key + " message " + value);

            throw new RuntimeException("Testing exception from stream");
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return KeyValue.pair(key, value);
    }


}
