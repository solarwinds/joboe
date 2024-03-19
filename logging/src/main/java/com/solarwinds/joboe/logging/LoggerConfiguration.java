package com.solarwinds.joboe.logging;

import lombok.Builder;
import lombok.Value;

import java.nio.file.Path;

@Value
@Builder
public class LoggerConfiguration {
    LogSetting logSetting;
    boolean debug;
    Path logFile;
}
