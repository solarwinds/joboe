package com.appoptics.apploader.instrumenter.reactor;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;

import com.tracelytics.joboe.span.impl.Tracer;
import com.tracelytics.logging.Logger;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Operators;
import com.tracelytics.logging.LoggerFactory;

/**
 * The instrumentor for Reactor Flux/Mono/ParallelFlux.
 */
public class PublisherInstrumenter {
    private static final AtomicBoolean inited = new AtomicBoolean();
    private static final Logger logger = LoggerFactory.getLogger();

    public static void enterOnAssembly() {
        if (inited.get())
            return;

        synchronized (inited) {
            if (inited.get())
                return;

            try {
                Operators.class.getMethod("liftPublisher", BiFunction.class);
            }
            catch (final NoSuchMethodException e) {
                logger.warn("Reactor version is not supported");
                inited.set(true);
                return;
            }
            Hooks.onEachOperator(TracedSubscriber.asOperator(Tracer.INSTANCE));
            Hooks.onLastOperator(TracedSubscriber.asOperator(Tracer.INSTANCE));
            inited.set(true);
        }
    }
}
