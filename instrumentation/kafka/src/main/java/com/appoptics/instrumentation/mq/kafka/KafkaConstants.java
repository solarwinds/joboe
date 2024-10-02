package com.appoptics.instrumentation.mq.kafka;

public class KafkaConstants {
    public static final boolean DEFAULT_CONTEXT_PROPAGATION_ENABLED = false; //disabled by default, as it could trigger exception for old consumer clients https://issues.apache.org/jira/browse/KAFKA-6739
}
