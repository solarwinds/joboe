package com.solarwinds.monitor;

import java.util.List;

public interface SystemMonitorFactory {
    List<SystemMonitor<?, ?>> buildMonitors();
}