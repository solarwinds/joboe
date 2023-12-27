package com.solarwinds.logging;

import com.solarwinds.joboe.config.ConfigContainer;
import com.solarwinds.joboe.config.ConfigProperty;
import com.solarwinds.logging.setting.LogSetting;
import com.solarwinds.util.DaemonThreadFactory;
import com.solarwinds.util.HostInfoUtils;
import lombok.Getter;

import java.io.*;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

/**
 * A wrapper class around the java.util.logger. Take note that this is very similar to other logging frameworks (commons.logging, log4j etc)'s Logger and
 * should be replaced by those implementations if we later on switch to those frameworks
 *
 * @author Patson Luk
 */
public class Logger {
    public static final String SOLARWINDSS_TAG = "[SolarWinds APM]";

    private static final Level DEFAULT_LOGGING = Level.INFO;
    private static final ThreadLocal<DateFormat> DATE_FORMAT = new ThreadLocal<DateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat("MMM dd, yyyy hh:mm:ss.SSS aa");
        }
    };
    private static final LogSetting DEFAULT_LOG_SETTING = new LogSetting(DEFAULT_LOGGING, true, true, null, null, null);

    static final Logger INSTANCE = new Logger();
    static final Logger STD_STREAM_LOGGER = new Logger(); //an internal logger that uses stderr and stdout streams only

    @Getter
    private Level loggerLevel = DEFAULT_LOGGING;
    private LoggerStream errorStream = new SystemErrStream();
    private LoggerStream infoStream = new SystemOutStream();


    Logger() {

    }

    LoggerStream getLogFileStream(Path logFilePath, int logFileMaxSize, int logFileMaxBackup) {
        //special case for azure, prefix the instance id to the log file name
        String azureInstanceId = HostInfoUtils.getAzureInstanceId();
        if (azureInstanceId != null) {
            Path fileName = logFilePath.getFileName();
            String prefixedFileName = azureInstanceId + "-" + fileName.toString();
            logFilePath = logFilePath.getParent() != null ?
                    Paths.get(logFilePath.getParent().toString(), prefixedFileName) :
                    Paths.get(prefixedFileName);
        }

        try {
            info("Java agent log location: " + logFilePath.toAbsolutePath());
            return new FileLoggerStream(logFilePath, logFileMaxSize * 1024 * 1024, logFileMaxBackup);
        } catch (IOException e) {
            warn("Failed to redirect logs to [" + logFilePath.toAbsolutePath() + "] : " + e.getMessage(), e);
            return null;
        }
    }

    LoggerStream getErrorStream() {
        return errorStream;
    }

    LoggerStream getInfoStream() {
        return infoStream;
    }

    public void fatal(String message) {
        this.log(Level.FATAL, message);
    }

    public void fatal(String message, Throwable throwable) {
        this.log(Level.FATAL, message, throwable);
    }

    public void error(String message) {
        this.log(Level.ERROR, message);
    }

    public void error(String message, Throwable throwable) {
        this.log(Level.ERROR, message, throwable);
    }

    public void warn(String message) {
        this.log(Level.WARNING, message);
    }

    public void warn(String message, Throwable throwable) {
        this.log(Level.WARNING, message, throwable);
    }

    public void info(String message) {
        this.log(Level.INFO, message);
    }

    public void info(String message, Throwable throwable) {
        this.log(Level.INFO, message, throwable);
    }

    public void debug(String message) {
        this.log(Level.DEBUG, message);
    }

    public void debug(String message, Throwable throwable) {
        this.log(Level.DEBUG, message, throwable);
    }

    public void trace(String message) {
        this.log(Level.TRACE, message);
    }

    public void trace(String message, Throwable throwable) {
        this.log(Level.TRACE, message, throwable);
    }


    public void log(Level level, String message) {
        log(level, message, null);
    }

    public void log(Level level, String message, Throwable t) {
        if (level == null) {
            if (shouldLog(Level.ERROR)) { //missing level in the input has severity level of Level.ERROR
                print(Level.ERROR, "Missing log Level for this log message!");
            }
            if (shouldLog(DEFAULT_LOGGING)) { //the logging message itself takes the default logging level
                print(DEFAULT_LOGGING, message, t);
            }
        } else if (shouldLog(level)) {
            print(level, message, t);
        }
    }

    private void print(Level level, String message) {
        print(level, message, null);
    }

    private void print(Level level, String message, Throwable t) {
        LoggerStream output = level.compareTo(Level.INFO) < 0 ? errorStream : infoStream; //most severe level declared first
        output.println(getFormattedMessage(level, message));

        if (t != null) {
            output.printStackTrace(t);
        }
    }

    private static String getFormattedMessage(Level level, String message) {
        String timestamp = DATE_FORMAT.get().format(Calendar.getInstance().getTime());
        String label = timestamp + " " + level.toString() + " " + SOLARWINDSS_TAG + " ";

        return message != null ? label + message : label;
    }


    public boolean shouldLog(Level messageLevel) {
        return messageLevel.compareTo(loggerLevel) <= 0; //most severe level is declared first
    }

    void configure(ConfigContainer config) {
        LogSetting logSetting = (LogSetting) config.get(ConfigProperty.AGENT_LOGGING);
        if (logSetting == null) {
            logSetting = DEFAULT_LOG_SETTING;
        }

        //level
        if (config.containsProperty(ConfigProperty.AGENT_DEBUG) && (Boolean) config.get(ConfigProperty.AGENT_DEBUG)) { //backward compatibility, use loggingLevel=DEBUG if the argument is set to false
            loggerLevel = Level.DEBUG;
        } else {
            loggerLevel = logSetting.getLevel() != null ? logSetting.getLevel() : DEFAULT_LOGGING;
        }

        //log file location
        Path path = null;
        if (config.containsProperty(ConfigProperty.AGENT_LOG_FILE)) { //this takes precedence
            path = (Path) config.get(ConfigProperty.AGENT_LOG_FILE);
        } else if (logSetting.getLogFilePath() != null) {
            path = logSetting.getLogFilePath();
        }

        //other log file parameters
        LoggerStream logFileStream = null;
        if (path != null) {
            String javaVersion = System.getProperty("java.version");

            if (javaVersion.startsWith("1.6")) {

                info("Cannot set up log file at [" + path + "] as it is only supported for JDK 7 or later");
            } else {
                logFileStream = getLogFileStream(path, logSetting.getLogFileMaxSize(), logSetting.getLogFileMaxBackup());
            }
        }

        List<LoggerStream> errorStreams = new ArrayList<LoggerStream>();
        List<LoggerStream> infoStreams = new ArrayList<LoggerStream>();

        if (logSetting.isStderrEnabled()) {
            errorStreams.add(SystemErrStream.INSTANCE);
        }
        if (logSetting.isStdoutEnabled()) {
            infoStreams.add(SystemOutStream.INSTANCE);
        }
        if (logFileStream != null) {
            errorStreams.add(logFileStream);
            infoStreams.add(logFileStream);
        }

        if (errorStreams.size() == 1) {
            this.errorStream = errorStreams.get(0);
        } else {
            this.errorStream = new CompositeStream(errorStreams.toArray(new LoggerStream[errorStreams.size()]));
        }

        if (infoStreams.size() == 1) {
            this.infoStream = infoStreams.get(0);
        } else {
            this.infoStream = new CompositeStream(infoStreams.toArray(new LoggerStream[infoStreams.size()]));
        }
    }


    @Getter
    public enum Level {
        OFF("off"),
        FATAL("fatal"),
        ERROR("error"),
        WARNING("warn"),
        INFO("info"),
        DEBUG("debug"),
        TRACE("trace");


        private static final Map<String, Level> map = new HashMap<String, Level>();

        static {
            for (Level level : Level.values()) {
                map.put(level.label, level);
            }
        }

        private final String label;

        Level(String label) {
            this.label = label;
        }

        public static Level fromLabel(String label) {
            return map.get(label);
        }

        public static Set<String> getAllLabels() {
            return Collections.unmodifiableSet(map.keySet());
        }
    }

    interface LoggerStream {
        void println(String value);

        void printStackTrace(Throwable throwable);
    }

    class CompositeStream implements LoggerStream {
        private final LoggerStream[] streams;

        private CompositeStream(LoggerStream... streams) {
            this.streams = streams;
        }

        @Override
        public void println(String value) {
            for (LoggerStream stream : streams) {
                stream.println(value);
            }
        }

        @Override
        public void printStackTrace(Throwable throwable) {
            for (LoggerStream stream : streams) {
                stream.printStackTrace(throwable);
            }

        }

        LoggerStream[] getStreams() {
            return streams;
        }
    }

    static class SystemOutStream implements LoggerStream {
        static final SystemOutStream INSTANCE = new SystemOutStream();

        private SystemOutStream() {
        }

        @Override
        public void println(String value) {
            System.out.println(value);
        }

        @Override
        public void printStackTrace(Throwable throwable) {
            throwable.printStackTrace(System.out);
        }
    }

    static class SystemErrStream implements LoggerStream {
        static final SystemErrStream INSTANCE = new SystemErrStream();

        private SystemErrStream() {
        }

        @Override
        public void println(String value) {
            System.err.println(value);
        }

        @Override
        public void printStackTrace(Throwable throwable) {
            throwable.printStackTrace(System.err);
        }
    }

    /**
     * Logger stream that writes to a File via a FileChannel
     *
     * This implements a rolling behavior which rolls the log file and create backup files up to `maxBackup` if
     * the current log file size exceeds `maxSize`.
     *
     * Take note that multiple java processes might run this logger and the rolling operation should be blocking across
     * processes so that the log file should only be rolled once.
     *
     * If another process has rolled the file, the current logger might still write to the renamed (rolled) file for a
     * short while but should be switching to the new log file eventually. However, it is guaranteed that no log should
     * be lost if the rolling is done by another process.
     */
    static class FileLoggerStream implements LoggerStream, Closeable {
        private static final int MAX_LOCK_TRY = 5;
        private static final int LOCK_RETRY_SLEEP = 100; //in millseconds
        private static final int CLOSE_TIMEOUT = 5;
        private static final int MESSAGE_QUEUE_SIZE = 100000;
        private static final int RETRY_INTERVAL = 60000; //only retry every 60 seconds
        private final Path logFilePath;
        private final Path logFileLockPath; //need a separate lock file, cannot use the existing log file as it could be moved by other processes
        FileChannel fileChannel;
        private PrintWriter filePrintWriter;
        private Long lastRollFailure; //last time of file rolling failure
        private Long lastChannelReset; //last time the file channel was reset
        private final long maxSize; //in bytes
        private final int maxBackup;
        private final ExecutorService service = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(MESSAGE_QUEUE_SIZE), DaemonThreadFactory.newInstance("file-logger"));



        FileLoggerStream(Path logFilePath, int maxSizeInBytes, int maxBackup) throws IOException {
            resetChannel(logFilePath);
            this.logFilePath = logFilePath;
            String tempDir = System.getProperty("java.io.tmpdir");
            if (tempDir != null && Files.isWritable(Paths.get(tempDir))) {
                this.logFileLockPath = Paths.get(tempDir, logFilePath.getFileName() + ".lock");
            } else {
                this.logFileLockPath = Paths.get(logFilePath.toString() + ".lock");
            }
            Logger.INSTANCE.debug("Using " + logFileLockPath + " for log file lock for file rolling");

            this.maxSize = maxSizeInBytes;
            this.maxBackup = maxBackup;
        }

        Path getLogFilePath() {
            return logFilePath;
        }

        private void resetChannel(Path logFilePath) throws IOException {
            resetChannel(logFilePath, true);
        }

        /**
         * Resets the file channel and print writer
         *
         * If forced, then it always resets it, otherwise reset would only happen if it has been less than RETRY_INTERVAL since last attempt
         *
         * @param logFilePath
         * @param forced
         * @throws IOException
         */
        private void resetChannel(Path logFilePath, boolean forced) throws IOException {
            boolean resetChannel;
            long time = System.currentTimeMillis();
            if (forced) {
                resetChannel = true;
            } else {
                resetChannel = lastChannelReset == null || time - lastChannelReset >= RETRY_INTERVAL;
            }

            if (resetChannel) {
                closeIo();

                this.fileChannel = FileChannel.open(logFilePath, StandardOpenOption.APPEND, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                this.filePrintWriter = new PrintWriter(new BufferedWriter(Channels.newWriter(fileChannel, Charset.defaultCharset().newEncoder(), -1)));

                lastChannelReset = time;
            }
        }

        @Override
        public void println(final String value) {
            try {
                service.submit(new LogTask() {
                    @Override
                    protected void log() {
                        filePrintWriter.println(value);
                        filePrintWriter.flush();
                    }
                });
            } catch (RejectedExecutionException e) {
                //use STD_STREAM_LOGGER as we do not want to submit more messages to this file logger
                STD_STREAM_LOGGER.debug("Failed to log message to file as queue is full " + e.getMessage(), e);
            }
        }

        @Override
        public void printStackTrace(final Throwable throwable) {
            try {
                service.submit(new LogTask() {
                    @Override
                    protected void log() {
                        throwable.printStackTrace(filePrintWriter);
                        filePrintWriter.flush();
                    }
                });
            } catch (RejectedExecutionException e) {
                //use STD_STREAM_LOGGER as we do not want to submit more messages to this file logger
                STD_STREAM_LOGGER.debug("Failed to log message to file as queue is full " + e.getMessage(), e);
            }
        }

        @Override
        public void close() {
            close(CLOSE_TIMEOUT);
        }

        public void close(long closeTimeoutInSec) {
            service.shutdown();
            try {
                service.awaitTermination(closeTimeoutInSec, TimeUnit.SECONDS);
                closeIo();
//                Files.deleteIfExists(logFileLockPath); //Do not attempt to delete file, it's problematic on some OS
            } catch (Exception e) {
                //ok, it's closing anyway
            }
        }

        private void closeIo() throws IOException {
            if (filePrintWriter != null) {
                filePrintWriter.flush();
                filePrintWriter.close();
            }
            if (fileChannel != null) {
                fileChannel.close();
            }
        }


        private abstract class LogTask implements Callable<Boolean> {
            @Override
            public Boolean call() {
                try {
                    checkFile();
                } catch (Throwable e) {
                    //use STD_STREAM_LOGGER as we do not want to submit more messages to this file logger which might trigger more error
                    STD_STREAM_LOGGER.warn("Failed to check the log file for size limit : " + e.getMessage(), e);
                }
                log();
                return true;
            }

            protected abstract void log();
        }


        /**
         * Checks and resets if the current log file channel exceeds the max size and perform file rolling if necessary
         *
         * Take note that this needs to take into consideration of:
         * 1. 2 threads within the same JVM can possibly call this concurrently, even though current implementation runs
         * on a single threaded pool, it could change in the future.
         * 2. 2 JVM processes can possibly call this concurrently (for example the shutdown process of tomcat is a separate
         * process from the running tomcat), and it should only roll once.
         */
        private boolean checkFile() throws IOException {
            if (isChannelExceedingSizeLimit()) {
                boolean forcedReset = false;
                try {
                    if (shouldRollFile()) { //then see if files should be rolled
                        Logger.INSTANCE.debug("Log file [" + logFilePath + "] exceeds " + (maxSize / 1024 / 1024) + " MB, attempt to roll the log files.");
                        boolean rolled = lockAndRollFile();
                        forcedReset = rolled; //always reset the channel if the rolling was successful
                    }
                } finally {
                    //if the channel exceeds size limit and the file rolling was successful, then it's required to
                    //reset the channel. Otherwise, we will try to reset the channel but w/o a forced reset, this is
                    //to avoid resetting the channel too rapidly if file rolling fails repeatedly due to persistent
                    //problem such as external file locking. Take note that it still makes sense to try resetting even
                    //if file rolling failed, for example another process might have rolled the file so this process
                    //should attempt to reset file channel to point to the new log file
                    resetChannel(logFilePath, forcedReset);
                }
            }
            return true;
        }

        private boolean isChannelExceedingSizeLimit() throws IOException {
            return fileChannel.size() > maxSize;
        }

        private boolean shouldRollFile() throws IOException {
            return Files.size(logFilePath) > maxSize && (lastRollFailure == null || System.currentTimeMillis() - lastRollFailure >= RETRY_INTERVAL);
        }


        /**
         * Attempts to acquire the lock for rolling the file.
         *
         * If the lock is acquired, then perform file rolling
         *
         * @return whether the file was rolled successfully
         * @throws IOException
         */
        boolean lockAndRollFile() throws IOException {
            FileLockAndChannel fileLockAndChannel = null;

            try {
                fileLockAndChannel = tryLock();
                boolean rolled = false;
                if (fileLockAndChannel == null) {
                    //ok to log to the file logger as we know this would not trigger rapid file locking error as it's guarded by RETRY_INTERVAL
                    Logger.INSTANCE.warn("Failed to acquire lock on log file after " + MAX_LOCK_TRY + " tries during the attempt to roll.");
                } else { //locked acquired, check again to ensure the files are not rolled while waiting for the lock
                    if (shouldRollFile())  {
                        try {
                            doRollFile();
                            rolled = true;
                            Logger.INSTANCE.debug("Successfully rolled the log files");
                            lastRollFailure = null; //reset failure timestamp
                        } catch (IOException e) {
                            //ok to log to the file logger as we know this would not trigger rapid file locking error as it's guarded by RETRY_INTERVAL
                            Logger.INSTANCE.warn("Failed to roll the log files, exception message: " + e.getMessage(), e);
                            lastRollFailure = System.currentTimeMillis();
                            throw e;
                        }
                    }
                }
                return rolled;
            } finally {
                if (fileLockAndChannel != null) {
                    fileLockAndChannel.fileLock.release(); //release the lock first before closing the file channel
                    fileLockAndChannel.lockChannel.close();
                    //Files.deleteIfExists(logFileLockPath); //do NOT delete the file as locking and deleting do not work well in some OS
                }
            }
        }

        private void doRollFile() throws IOException {
            //delete the one with highest index if exist
            Path lastBackupFilePath = Paths.get(logFilePath.toString() + "." + maxBackup);
            Files.deleteIfExists(lastBackupFilePath);

            //traverse the backup file and bump the index up by one, from the highest index - 1 first
            for (int i = maxBackup - 1; i > 0; i--) {
                Path backupFilePath = Paths.get(logFilePath + "." + i);
                Path newBackupFilePath = Paths.get(logFilePath + "." + (i + 1));
                if (Files.exists(backupFilePath)) {
                    Files.move(backupFilePath, newBackupFilePath, StandardCopyOption.ATOMIC_MOVE);
                }
            }
            //move the current log file
            Files.move(logFilePath, Paths.get(logFilePath + ".1"), StandardCopyOption.ATOMIC_MOVE);
        }

        FileLockAndChannel tryLock() {
            int tryCount = 0;
            FileLock lock;
            while (++ tryCount <= MAX_LOCK_TRY) {
                try {
                    FileChannel lockChannel = FileChannel.open(logFileLockPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE); //do not use DELETE_ON_EXIT here. Not working sometimes
                    lock = lockChannel.tryLock();
                    if (lock != null) {
                        return new FileLockAndChannel(lock, lockChannel);
                    }
                } catch (IOException e) {
                    Logger.INSTANCE.debug("Failed to obtain lockChannel : " + e.getMessage(), e);
                }
                try {
                    TimeUnit.MILLISECONDS.sleep(LOCK_RETRY_SLEEP);
                } catch (InterruptedException e) {
                    Logger.INSTANCE.debug("Retry lock sleep interrupted", e);
                }
            }
            return null;
        }

        private class FileLockAndChannel {
            private final FileLock fileLock;
            private final FileChannel lockChannel;

            private FileLockAndChannel(FileLock fileLock, FileChannel lockChannel) {
                this.fileLock = fileLock;
                this.lockChannel = lockChannel;
            }
        }
    }
}
