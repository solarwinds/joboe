package com.solarwinds.joboe.logging;

import lombok.Builder;
import lombok.Value;

import java.nio.file.Path;

@Value
@Builder
public class LoggerConfiguration {
    @Builder.Default
    LogSetting logSetting = new LogSetting(Logger.Level.INFO, true, true, null, null, null);

    boolean debug;

    Path logFile;
}
