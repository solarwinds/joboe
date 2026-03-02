package com.solarwinds.joboe.logging;

import java.nio.file.Path;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class LoggerConfiguration {
  @Builder.Default
  LogSetting logSetting = new LogSetting(Logger.Level.INFO, true, true, null, null, null);

  boolean debug;

  Path logFile;
}
