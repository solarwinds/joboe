package com.appoptics.api.ext;

import io.grpc.Context;

/**
 * See https://github.com/open-telemetry/opentelemetry-java/blob/3a937bf0cceb7f59868a9d073924d209d44cc4aa/contrib/context_interceptor/src/main/java/io/grpc/override/ContextStorageListener.java
 */
public interface ContextChangeListener {
    void onContextChange(Context oldContext, Context newContext);
}
