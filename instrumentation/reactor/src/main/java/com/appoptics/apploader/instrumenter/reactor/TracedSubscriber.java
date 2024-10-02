package com.appoptics.apploader.instrumenter.reactor;

import java.util.function.Function;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import com.tracelytics.joboe.span.Tracer;
import com.tracelytics.joboe.span.Span;
import com.tracelytics.joboe.span.Scope;
import reactor.core.CoreSubscriber;
import reactor.core.Fuseable;
import reactor.core.publisher.Operators;
import reactor.util.context.Context;

/**
 * The tracer subscriber. As a wrapper to the real subscriber it retrieves the trace context from either the reactor's context
 * or from the current active span and wraps the subscriber actions with the trace context. This helps propagate and activate the
 * trace context when the publisher is subscribed by a subscriber in a different thread (via the reactor's scheduler. see
 * reactor.core.scheduler.Scheduler and Mono#subscribeOn() or Flux#subscribeOn() for more details).
 * It's based on this OpenTracing implementation: https://github.com/opentracing-contrib/java-reactor/blob/79752079162f1569779ba8d8fe33a16e4d91e984/src/main/java/io/opentracing/contrib/reactor/
 * @param <T>
 */
public class TracedSubscriber<T> implements CoreSubscriber<T>, Fuseable.QueueSubscription<T> {
    private final Span span;
    private final Subscriber<? super T> subscriber;
    private final Context context;
    private final com.tracelytics.joboe.span.impl.Tracer tracer;
    private Subscription subscription;


    public TracedSubscriber(Subscriber<? super T> subscriber, Context ctx, com.tracelytics.joboe.span.impl.Tracer tracer) {
        this.subscriber = subscriber;
        this.tracer = tracer;

        this.span = ctx != null ?
                ctx.getOrDefault(Span.class, this.tracer.activeSpan()) : null;

        this.context = ctx != null && this.span != null ?
                ctx.put(Span.class, this.span) : ctx != null ?
                ctx : Context.empty();
    }

    @Override
     public T poll() {
        return null;
    }

    @Override
     public int requestFusion(int i) {
        return Fuseable.NONE; //always negotiate to no fusion
    }

    @Override
     public int size() {
        return 0;
    }

    @Override
     public boolean isEmpty() {
        return true;
    }

    @Override
     public void clear() {
        //NO-OP
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        this.subscription = subscription;
        withActiveSpan(() -> subscriber.onSubscribe(this));
    }

    @Override
    public void request(long n) {
        withActiveSpan(() -> subscription.request(n));
    }

    @Override
    public void onNext(T o) {
        withActiveSpan(() -> subscriber.onNext(o));
    }

    @Override
    public void cancel() {
        withActiveSpan(() -> subscription.cancel());
    }

    @Override
    public void onError(Throwable throwable) {
        withActiveSpan(() -> subscriber.onError(throwable));
    }

    @Override
    public void onComplete() {
        withActiveSpan(() -> subscriber.onComplete());
    }

    @Override
    public Context currentContext() {
        return context;
    }

    private void withActiveSpan(Runnable runnable) {
        if (span != null) {
            try (Scope inScope = tracer.scopeManager().activate(span, false, true)) {
                runnable.run();
            }
        } else {
            runnable.run();
        }
    }

    /**
     * Based on Spring Sleuth's Reactor instrumentation.
     * <p>
     * Return a span operator pointcut given a {@link Tracer}. This can be used in reactor
     * via {@link reactor.core.publisher.Flux#transform(Function)}, {@link
     * reactor.core.publisher.Mono#transform(Function)}, {@link
     * reactor.core.publisher.Hooks#onEachOperator(Function)} or {@link
     * reactor.core.publisher.Hooks#onLastOperator(Function)}. The Span operator
     * pointcut will pass the Scope of the Span without ever creating any new spans.
     *
     * @param <T> an arbitrary type that is left unchanged by the span operator
     * @return a new span operator pointcut
     */
    public static <T> Function<? super Publisher<T>, ? extends Publisher<T>> asOperator(com.tracelytics.joboe.span.impl.Tracer tracer) {
        return Operators.liftPublisher((publisher, sub) -> {
            // if Flux/Mono #just, #empty, #error
            if (publisher instanceof Fuseable.ScalarCallable) {
                return sub;
            }

            return new TracedSubscriber<>(sub, sub.currentContext(), tracer);
        });
    }
}
