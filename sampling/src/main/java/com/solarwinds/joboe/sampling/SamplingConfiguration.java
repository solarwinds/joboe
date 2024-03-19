package com.solarwinds.joboe.sampling;

import lombok.Builder;
import lombok.Value;


@Value
@Builder
public class SamplingConfiguration {
    @Builder.Default
    int ttl = 1_200_000; //20 minutes by default, in unit of millisecond;
    @Builder.Default
    int maxEvents = 100_000; // max 100k events per trace by default
    @Builder.Default
    int maxBacktraces = 1000; // max 1000 backtraces per trace by default
    @Builder.Default
    boolean triggerTraceEnabled = true;
    int sampleRate;
    TracingMode tracingMode;
    TraceConfigs internalTransactionSettings;
}
