package com.tracelytics.instrumentation.jms;

import com.tracelytics.instrumentation.MethodMatcher;

import java.util.Arrays;
import java.util.List;


/**
 * The instrumentation class for JMS MessageConsumer. The MessageConsumer is the interface for synchronously
 * consuming the messages (while MessageListener is for asynchronous purpose). Listeners will then be notified
 * with the message (instrumented by MessageListenerInstrumentation as a separate trace)
 *
 *
 * Please Note that in this class the trace is started in the method `layerExit`. The method `layerEntry`
 * only records the start timestamp. The reason is that previously we want to extract the xTrace ID from the
 * producer side to build a distributed trace. This is not the case now as the SourceTrace KV is used instead,
 * - it doesn't hurt even we choose `SourceTrace` and I'd keep it for the ease of any future change.
 */
public class JakartaMessageConsumerInstrumentation extends MessageConsumerInstrumentation {
    private static final List<MethodMatcher<Type>> methodMatchers = Arrays.asList(
            new MethodMatcher<Type>("receive", new String[]{},"jakarta.jms.Message", Type.RECEIVE),
            new MethodMatcher<Type>("receiveNoWait", new String[]{},"jakarta.jms.Message", Type.RECEIVE_NOWAIT),
            new MethodMatcher<Type>("dequeue", new String[]{ "long" }, "org.apache.activemq.command.MessageDispatch", Type.DEQUEUE)
    );

    @Override
    protected String getPackagePrefix() {
        return "jakarta";
    }

    @Override
    protected List<MethodMatcher<Type>> getMethodMatchers() {
        return methodMatchers;
    }
}
