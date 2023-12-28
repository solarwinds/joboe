package com.solarwinds.joboe.metrics;

import java.util.List;

public interface SystemMonitorFactory {
    List<SystemMonitor<?, ?>> buildMonitors();
}