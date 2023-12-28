package com.solarwinds.joboe.core.logging.setting;

import com.solarwinds.joboe.core.logging.Logger;
import lombok.Getter;

import java.io.Serializable;
import java.nio.file.Path;
import java.util.Objects;

@Getter
public class LogSetting implements Serializable {
    private final Logger.Level level;
    private final boolean stdoutEnabled;
    private final boolean stderrEnabled;
    private final Path logFilePath;
    private final int logFileMaxSize;
    private final int logFileMaxBackup;

    public static final int DEFAULT_FILE_MAX_SIZE = 10; //in MB
    public static final int DEFAULT_FILE_MAX_BACKUP = 5; //files to be kept

    public LogSetting(Logger.Level level, boolean stdoutEnabled, boolean stderrEnabled, Path logFilePath, Integer logFileMaxSize, Integer logFileMaxBackup) {
        this.level = level;
        this.stdoutEnabled = stdoutEnabled;
        this.stderrEnabled = stderrEnabled;
        this.logFilePath = logFilePath;
        this.logFileMaxSize = logFileMaxSize != null ? logFileMaxSize : DEFAULT_FILE_MAX_SIZE;
        this.logFileMaxBackup = logFileMaxBackup != null ? logFileMaxBackup : DEFAULT_FILE_MAX_BACKUP;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LogSetting that = (LogSetting) o;
        return stdoutEnabled == that.stdoutEnabled &&
                stderrEnabled == that.stderrEnabled &&
                logFileMaxSize == that.logFileMaxSize &&
                logFileMaxBackup == that.logFileMaxBackup &&
                level == that.level &&
                Objects.equals(logFilePath, that.logFilePath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(level, stdoutEnabled, stderrEnabled, logFilePath, logFileMaxSize, logFileMaxBackup);
    }

    @Override
    public String toString() {
        return "LogSetting{" +
                "level=" + level +
                ", stdoutEnabled=" + stdoutEnabled +
                ", stderrEnabled=" + stderrEnabled +
                ", logFilePath=" + logFilePath +
                ", logFileMaxSize=" + logFileMaxSize +
                ", logFileMaxBackup=" + logFileMaxBackup +
                '}';
    }
}
