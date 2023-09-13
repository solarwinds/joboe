package com.tracelytics.util;

import com.tracelytics.joboe.rpc.HostType;

public final class HostTypeDetector {
    public static HostType getHostType() {
        boolean lambda = isLambda();
        if (lambda) return HostType.AWS_LAMBDA;

        return HostType.PERSISTENT;
    }

    public static boolean isLambda() {
        return System.getenv("LAMBDA_TASK_ROOT") != null && System.getenv("AWS_LAMBDA_FUNCTION_NAME") != null;
    }
}
