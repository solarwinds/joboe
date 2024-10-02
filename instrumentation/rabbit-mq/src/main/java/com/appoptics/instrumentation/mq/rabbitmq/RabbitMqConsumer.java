package com.appoptics.instrumentation.mq.rabbitmq;

public interface RabbitMqConsumer {
    String tvGetQueue();
    void tvSetQueue(String queue);

    String tvGetChannelHost();
    void tvSetChannelHost(String channelHost);
}
