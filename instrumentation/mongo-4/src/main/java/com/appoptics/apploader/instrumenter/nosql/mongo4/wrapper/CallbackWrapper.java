package com.appoptics.apploader.instrumenter.nosql.mongo4.wrapper;

import com.appoptics.instrumentation.nosql.mongo3.Mongo3CallbackInstrumentation;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import com.mongodb.internal.async.SingleResultCallback;
import com.tracelytics.instrumentation.SpanAware;
import com.tracelytics.joboe.span.impl.Span;

public class CallbackWrapper<T> implements SingleResultCallback<T>, SpanAware {
    private Span span;
    private final SingleResultCallback<T> wrapped;

    public CallbackWrapper(SingleResultCallback<T> wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public void onResult(T t, Throwable throwable) {
        if (span != null) {
            String remoteHost = Mongo3CallbackInstrumentation.getActiveRemoteHost();
            if (remoteHost != null) {
                span.setTag("RemoteHost", remoteHost);
            }

            //report KV NumDocumentsAffected for various operations
            if (t instanceof UpdateResult) {
                UpdateResult result = (UpdateResult) t;
                if (result.wasAcknowledged()) {
                    span.setTag("NumDocumentsAffected", result.getModifiedCount());
                }
            } else if (t instanceof DeleteResult) {
                DeleteResult result = (DeleteResult) t;
                if (result.wasAcknowledged()) {
                    span.setTag("NumDocumentsAffected", result.getDeletedCount());
                }
            } else if (t instanceof BulkWriteResult) {
                BulkWriteResult result = (BulkWriteResult) t;
                if (result.wasAcknowledged()) {
                    span.setTag("NumDocumentsAffected", result.getDeletedCount() + result.getInsertedCount() + result.getModifiedCount());
                }
            }

            span.finish();
        }
        wrapped.onResult(t, throwable);
    }

    @Override
    public void tvSetSpan(Span span) {
        this.span = span;
    }

    @Override
    public Span tvGetSpan() {
        return span;
    }
}
