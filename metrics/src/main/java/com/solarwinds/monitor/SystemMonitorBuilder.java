package com.solarwinds.monitor;

import java.util.List;

public interface SystemMonitorBuilder {
    List<SystemMonitor<?, ?>> build();
}
