package com.tracelytics.monitor;

import java.util.List;

public interface SystemMonitorBuilder {
    List<SystemMonitor<?, ?>> build();
}
