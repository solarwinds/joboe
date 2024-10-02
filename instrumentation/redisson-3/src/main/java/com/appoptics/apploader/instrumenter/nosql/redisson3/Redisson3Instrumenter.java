package com.appoptics.apploader.instrumenter.nosql.redisson3;

import com.appoptics.apploader.instrumenter.nosql.redisson2.Redisson2Instrumenter;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.joboe.span.impl.Span;
import org.redisson.api.RFuture;
import org.redisson.misc.RPromise;

import java.util.function.BiConsumer;

/**
 * Handle various captured operation from instrumentation. This class has access to the actual redission 3 framework
 */
public class Redisson3Instrumenter extends Redisson2Instrumenter {
    public static final Redisson3Instrumenter SINGLETON = new Redisson3Instrumenter();

    @Override
    protected void registerFutureListener(Span span, Object futureObject) {
        if (futureObject instanceof RFuture) {
            ((RFuture) futureObject).onComplete(new TracingAction(span));
        }
    }

    protected void addCommandListener(final Span span, RPromise promise, final boolean checkResult) {
        promise.whenComplete(new BiConsumer<Object, Throwable>() {
            @Override
            public void accept(Object o, Throwable throwable) {
                if (throwable != null) {
                    ClassInstrumentation.reportError(span, throwable);
                }
                if (checkResult) {
                    span.setTag("KVHit", o != null);
                }
                span.finish();
            }
        });
    }

    private static class TracingAction implements BiConsumer<Object, Throwable> {
        private final Span span;

        private TracingAction(Span span) {
            this.span = span;
        }

        @Override
        public void accept(Object o, Throwable throwable) {
            if (throwable != null) {
                ClassInstrumentation.reportError(this.span, throwable);
            }
            this.span.finish();
        }
    }


}
