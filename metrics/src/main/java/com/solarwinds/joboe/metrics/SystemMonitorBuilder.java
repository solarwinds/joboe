package com.solarwinds.joboe.metrics;

import java.util.List;

public interface SystemMonitorBuilder {
    List<SystemMonitor<?, ?>> build();
}
