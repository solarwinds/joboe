package com.tracelytics.instrumentation.logging;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.ext.javassist.NotFoundException;
import com.tracelytics.instrumentation.MethodMatcher;
import com.tracelytics.joboe.config.ConfigManager;
import com.tracelytics.joboe.config.ConfigProperty;
import com.tracelytics.joboe.config.LogTraceIdScope;
import com.tracelytics.joboe.config.LogTraceIdSetting;
import com.tracelytics.logging.Logger;
import com.tracelytics.logging.LoggerFactory;

/**
 * Patches `org.apache.logging.log4j.core.layout.AbstractJacksonLayout` to include trace context in structured logs automatically
 * @author pluk
 *
 */
public class Log4j2LayoutPatcher extends BaseLoggerPatcher {
    protected static final Logger logger = LoggerFactory.getLogger();
    
    private static final String PATCHER_CLASS_NAME = Log4j2LayoutPatcher.class.getName();
    private static final String LOG_EVENT_WRAPPER_JA_CLASS_NAME = "org.apache.logging.log4j.core.layout.AbstractJacksonLayout$LogEventWithAdditionalFields";
    private static final String LOG_EVENT_WRAPPER_CLASS_NAME = "org.apache.logging.log4j.core.layout.AbstractJacksonLayout.LogEventWithAdditionalFields";
    
    private enum OpType { WRAP_LOG_EVENT }
    
    private static final LogTraceIdScope AUTO_INSERT_SCOPE;
    
    static {
        LogTraceIdSetting settings = (LogTraceIdSetting) ConfigManager.getConfig(ConfigProperty.AGENT_LOGGING_TRACE_ID);
        AUTO_INSERT_SCOPE = settings != null ? settings.getAutoInsertScope() : LogTraceIdScope.DISABLED;
    }
    
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<OpType>> methodMatchers = Arrays.asList(new MethodMatcher<OpType>("wrapLogEvent", new String[] { "org.apache.logging.log4j.core.LogEvent" }, "java.lang.Object", OpType.WRAP_LOG_EVENT));
    
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes) throws Exception {
        if (AUTO_INSERT_SCOPE == LogTraceIdScope.DISABLED) {
            return false;
        }
        
        //make sure it has org.apache.logging.log4j.core.layout.AbstractJacksonLayout$LogEventWithAdditionalFields
        try {
            classPool.get(LOG_EVENT_WRAPPER_JA_CLASS_NAME);
        } catch (NotFoundException e) {
            logger.info("Not patching " + cc.getName() + " as " + LOG_EVENT_WRAPPER_CLASS_NAME + " is not found. Take note auto-insert trace context is only supported for log4j2 2.10 or above.");
            return false;
        }
        
        logger.debug("Patching AbstractLayout to insert ao.traceId into the layout.");
        
        for (Map.Entry<CtMethod, OpType> entry : findMatchingMethods(cc, methodMatchers).entrySet()) {
            CtMethod method = entry.getKey();
            String patch = "String logTraceId = " + PATCHER_CLASS_NAME + ".getLogTraceId();"
                    + "if (logTraceId != null) {"
                    + "    boolean inserted = false;"
                    + "    if ($_ instanceof " + LOG_EVENT_WRAPPER_CLASS_NAME + ") {"
                    + "        java.util.Map fields = ((" + LOG_EVENT_WRAPPER_CLASS_NAME + ") $_).getAdditionalFields();"
                    + "        if (fields != null) {"
                    + "            fields.put(\"" + BaseLoggerPatcher.TRACE_ID_KEY + "\", logTraceId);"
                    + "            inserted = true;"
                    + "        }"
                    + "    }"
                    + "    if (!inserted) {"
                    + "        java.util.Map fields = new java.util.LinkedHashMap();"
                    + "        fields.put(\"" + BaseLoggerPatcher.TRACE_ID_KEY + "\", logTraceId);"
                    + "        $_ = new " + LOG_EVENT_WRAPPER_CLASS_NAME + "($1, fields);"
                    + "    }"
                    + "}";
            
            insertAfter(method, patch, true, false);
        }
        return true;
    }
    
   
    public static String getLogTraceId() {
        return getLogTraceId(AUTO_INSERT_SCOPE);
    }
}