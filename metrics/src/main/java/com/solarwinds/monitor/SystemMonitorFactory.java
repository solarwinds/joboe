package com.solarwinds.monitor;

import java.util.List;

public interface SystemMonitorFactory {
    public List<SystemMonitor<?, ?>> buildMonitors();
}