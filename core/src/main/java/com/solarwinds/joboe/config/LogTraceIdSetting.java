package com.solarwinds.joboe.config;

import lombok.Getter;

/**
 * Setting that contains scope information for each log injection category.
 * 
 * There are 2 categories right now - "autoInsert" and "mdc"
 * 
 * @author pluk
 */
@Getter
public class LogTraceIdSetting {
    private final LogTraceIdScope autoInsertScope;
    private final LogTraceIdScope mdcScope;

    public LogTraceIdSetting(LogTraceIdScope autoInsertScope, LogTraceIdScope mdcScope) {
        this.autoInsertScope = autoInsertScope;
        this.mdcScope = mdcScope;
    }

}
