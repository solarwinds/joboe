package com.tracelytics.instrumentation.logging;

import com.tracelytics.joboe.config.ConfigManager;
import com.tracelytics.joboe.config.ConfigProperty;
import com.tracelytics.joboe.config.LogTraceIdScope;
import com.tracelytics.joboe.config.LogTraceIdSetting;

/**
 * Base class for patching loggers to include trace context in MDC
 * @author pluk
 *
 */
public abstract class BaseMdcPatcher extends BaseLoggerPatcher {
    protected static final LogTraceIdScope scope;
    
    static {
        LogTraceIdSetting logTraceIdSetting = (LogTraceIdSetting) ConfigManager.getConfig(ConfigProperty.AGENT_LOGGING_TRACE_ID);
        scope = logTraceIdSetting != null ? logTraceIdSetting.getMdcScope() : LogTraceIdScope.DISABLED;
    }
    
    public static String getLogTraceId() {
        return getLogTraceId(scope);
    }
}
