package com.solarwinds.logging;

import com.solarwinds.joboe.config.ConfigContainer;
import com.solarwinds.joboe.config.ConfigProperty;
import com.solarwinds.logging.Logger.Level;
import com.solarwinds.logging.Logger.LoggerStream;
import com.solarwinds.logging.setting.LogSetting;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LoggerTest {
    


    private class ProxyLoggerStream implements LoggerStream {
        private ByteArrayOutputStream proxyOutput = new ByteArrayOutputStream();
        private PrintStream proxyStream = new PrintStream(proxyOutput);

        public void println(String value) {
            proxyStream.println(value);
        }

        public void printStackTrace(Throwable throwable) {
            throwable.printStackTrace(proxyStream);
        }

        public ByteArrayOutputStream getProxyOutput() {
            return proxyOutput;
        }
    }
    
    private void setProxyStreams(Logger logger, ProxyLoggerStream out, ProxyLoggerStream err) throws Exception {
        Field outStreamField = Logger.class.getDeclaredField("infoStream");
        outStreamField.setAccessible(true);
        outStreamField.set(logger, out);
        
        Field errorStreamField = Logger.class.getDeclaredField("errorStream");
        errorStreamField.setAccessible(true);
        errorStreamField.set(logger, err);
        
    }
    
    @Test
    public void testTrace() throws Exception {
        Logger logger = new Logger();
        
        ConfigContainer config = new ConfigContainer();
        config.put(ConfigProperty.AGENT_LOGGING, getLogSetting(Level.TRACE));
        
        logger.configure(config);
        
        ProxyLoggerStream proxyOutStream = new ProxyLoggerStream();
        ProxyLoggerStream proxyErrStream = new ProxyLoggerStream();
        ByteArrayOutputStream proxyOut = proxyOutStream.getProxyOutput();
        ByteArrayOutputStream proxyErr = proxyErrStream.getProxyOutput();
        setProxyStreams(logger, proxyOutStream, proxyErrStream);
        
        sendTestMessages(logger);
        
        String outMessage = proxyOut.toString();
        String errMessage = proxyErr.toString();
        
        assertEquals(2, countOccurence(outMessage, "trace-message"));
        assertEquals(1, countOccurence(outMessage, "trace-exception"));
        assertEquals(2, countOccurence(outMessage, "debug-message"));
        assertEquals(1, countOccurence(outMessage, "debug-exception"));
        assertEquals(2, countOccurence(outMessage, "info-message"));
        assertEquals(1, countOccurence(outMessage, "info-exception"));
        assertEquals(2, countOccurence(errMessage, "warn-message"));
        assertEquals(1, countOccurence(errMessage, "warn-exception"));
        assertEquals(2, countOccurence(errMessage, "error-message"));
        assertEquals(1, countOccurence(errMessage, "error-exception"));
        assertEquals(2, countOccurence(errMessage, "fatal-message"));
        assertEquals(1, countOccurence(errMessage, "fatal-exception"));
    }

    @Test
    public void testDebug() throws Exception {
        Logger logger = new Logger();
        
        ConfigContainer config = new ConfigContainer();
        config.put(ConfigProperty.AGENT_LOGGING, getLogSetting(Level.DEBUG));
        
        logger.configure(config);
        
        ProxyLoggerStream proxyOutStream = new ProxyLoggerStream();
        ProxyLoggerStream proxyErrStream = new ProxyLoggerStream();
        ByteArrayOutputStream proxyOut = proxyOutStream.getProxyOutput();
        ByteArrayOutputStream proxyErr = proxyErrStream.getProxyOutput();
        setProxyStreams(logger, proxyOutStream, proxyErrStream);
        
        sendTestMessages(logger);
        
        String outMessage = proxyOut.toString();
        String errMessage = proxyErr.toString();
        
        assertEquals(0, countOccurence(outMessage, "trace-message"));
        assertEquals(0, countOccurence(outMessage, "trace-exception"));
        assertEquals(2, countOccurence(outMessage, "debug-message"));
        assertEquals(1, countOccurence(outMessage, "debug-exception"));
        assertEquals(2, countOccurence(outMessage, "info-message"));
        assertEquals(1, countOccurence(outMessage, "info-exception"));
        assertEquals(2, countOccurence(errMessage, "warn-message"));
        assertEquals(1, countOccurence(errMessage, "warn-exception"));
        assertEquals(2, countOccurence(errMessage, "error-message"));
        assertEquals(1, countOccurence(errMessage, "error-exception"));
        assertEquals(2, countOccurence(errMessage, "fatal-message"));
        assertEquals(1, countOccurence(errMessage, "fatal-exception"));
    }

    @Test
    public void testInfo() throws Exception {
        Logger logger = new Logger();
        
        ConfigContainer config = new ConfigContainer();
        config.put(ConfigProperty.AGENT_LOGGING, getLogSetting(Level.INFO));
        
        logger.configure(config);
        
        ProxyLoggerStream proxyOutStream = new ProxyLoggerStream();
        ProxyLoggerStream proxyErrStream = new ProxyLoggerStream();
        ByteArrayOutputStream proxyOut = proxyOutStream.getProxyOutput();
        ByteArrayOutputStream proxyErr = proxyErrStream.getProxyOutput();
        setProxyStreams(logger, proxyOutStream, proxyErrStream);
        
        sendTestMessages(logger);
        
        String outMessage = proxyOut.toString();
        String errMessage = proxyErr.toString();
        
        assertEquals(0, countOccurence(outMessage, "trace-message"));
        assertEquals(0, countOccurence(outMessage, "trace-exception"));
        assertEquals(0, countOccurence(outMessage, "debug-message"));
        assertEquals(0, countOccurence(outMessage, "debug-exception"));
        assertEquals(2, countOccurence(outMessage, "info-message"));
        assertEquals(1, countOccurence(outMessage, "info-exception"));
        assertEquals(2, countOccurence(errMessage, "warn-message"));
        assertEquals(1, countOccurence(errMessage, "warn-exception"));
        assertEquals(2, countOccurence(errMessage, "error-message"));
        assertEquals(1, countOccurence(errMessage, "error-exception"));
        assertEquals(2, countOccurence(errMessage, "fatal-message"));
        assertEquals(1, countOccurence(errMessage, "fatal-exception"));
    }

    @Test
    public void testWarn() throws Exception {
        Logger logger = new Logger();
        
        ConfigContainer config = new ConfigContainer();
        config.put(ConfigProperty.AGENT_LOGGING, getLogSetting(Level.WARNING));
        
        logger.configure(config);
        
        ProxyLoggerStream proxyOutStream = new ProxyLoggerStream();
        ProxyLoggerStream proxyErrStream = new ProxyLoggerStream();
        ByteArrayOutputStream proxyOut = proxyOutStream.getProxyOutput();
        ByteArrayOutputStream proxyErr = proxyErrStream.getProxyOutput();
        setProxyStreams(logger, proxyOutStream, proxyErrStream);
        
        sendTestMessages(logger);
        
        String outMessage = proxyOut.toString();
        String errMessage = proxyErr.toString();
        
        assertEquals(0, countOccurence(outMessage, "trace-message"));
        assertEquals(0, countOccurence(outMessage, "trace-exception"));
        assertEquals(0, countOccurence(outMessage, "debug-message"));
        assertEquals(0, countOccurence(outMessage, "debug-exception"));
        assertEquals(0, countOccurence(outMessage, "info-message"));
        assertEquals(0, countOccurence(outMessage, "info-exception"));
        assertEquals(2, countOccurence(errMessage, "warn-message"));
        assertEquals(1, countOccurence(errMessage, "warn-exception"));
        assertEquals(2, countOccurence(errMessage, "error-message"));
        assertEquals(1, countOccurence(errMessage, "error-exception"));
        assertEquals(2, countOccurence(errMessage, "fatal-message"));
        assertEquals(1, countOccurence(errMessage, "fatal-exception"));
    }

    @Test
    public void testError() throws Exception {
        Logger logger = new Logger();
        
        ConfigContainer config = new ConfigContainer();
        config.put(ConfigProperty.AGENT_LOGGING, getLogSetting(Level.ERROR));
        
        logger.configure(config);
        
        ProxyLoggerStream proxyOutStream = new ProxyLoggerStream();
        ProxyLoggerStream proxyErrStream = new ProxyLoggerStream();
        ByteArrayOutputStream proxyOut = proxyOutStream.getProxyOutput();
        ByteArrayOutputStream proxyErr = proxyErrStream.getProxyOutput();
        setProxyStreams(logger, proxyOutStream, proxyErrStream);
        
        sendTestMessages(logger);
        
        String outMessage = proxyOut.toString();
        String errMessage = proxyErr.toString();
        
        assertEquals(0, countOccurence(outMessage, "trace-message"));
        assertEquals(0, countOccurence(outMessage, "trace-exception"));
        assertEquals(0, countOccurence(outMessage, "debug-message"));
        assertEquals(0, countOccurence(outMessage, "debug-exception"));
        assertEquals(0, countOccurence(outMessage, "info-message"));
        assertEquals(0, countOccurence(outMessage, "info-exception"));
        assertEquals(0, countOccurence(errMessage, "warn-message"));
        assertEquals(0, countOccurence(errMessage, "warn-exception"));
        assertEquals(2, countOccurence(errMessage, "error-message"));
        assertEquals(1, countOccurence(errMessage, "error-exception"));
        assertEquals(2, countOccurence(errMessage, "fatal-message"));
        assertEquals(1, countOccurence(errMessage, "fatal-exception"));
    }

    @Test
    public void testFatal() throws Exception {
        Logger logger = new Logger();
        
        ConfigContainer config = new ConfigContainer();
        config.put(ConfigProperty.AGENT_LOGGING, getLogSetting(Level.FATAL));
        
        logger.configure(config);
        
        ProxyLoggerStream proxyOutStream = new ProxyLoggerStream();
        ProxyLoggerStream proxyErrStream = new ProxyLoggerStream();
        ByteArrayOutputStream proxyOut = proxyOutStream.getProxyOutput();
        ByteArrayOutputStream proxyErr = proxyErrStream.getProxyOutput();
        setProxyStreams(logger, proxyOutStream, proxyErrStream);
        
        sendTestMessages(logger);
        
        String outMessage = proxyOut.toString();
        String errMessage = proxyErr.toString();
        
        assertEquals(0, countOccurence(outMessage, "trace-message"));
        assertEquals(0, countOccurence(outMessage, "trace-exception"));
        assertEquals(0, countOccurence(outMessage, "debug-message"));
        assertEquals(0, countOccurence(outMessage, "debug-exception"));
        assertEquals(0, countOccurence(outMessage, "info-message"));
        assertEquals(0, countOccurence(outMessage, "info-exception"));
        assertEquals(0, countOccurence(errMessage, "warn-message"));
        assertEquals(0, countOccurence(errMessage, "warn-exception"));
        assertEquals(0, countOccurence(errMessage, "error-message"));
        assertEquals(0, countOccurence(errMessage, "error-exception"));
        assertEquals(2, countOccurence(errMessage, "fatal-message"));
        assertEquals(1, countOccurence(errMessage, "fatal-exception"));
    }

    @Test
    public void testOff() throws Exception {
        Logger logger = new Logger();
        
        ConfigContainer config = new ConfigContainer();
        config.put(ConfigProperty.AGENT_LOGGING, getLogSetting(Level.OFF));
        
        logger.configure(config);
        
        ProxyLoggerStream proxyOutStream = new ProxyLoggerStream();
        ProxyLoggerStream proxyErrStream = new ProxyLoggerStream();
        ByteArrayOutputStream proxyOut = proxyOutStream.getProxyOutput();
        ByteArrayOutputStream proxyErr = proxyErrStream.getProxyOutput();
        setProxyStreams(logger, proxyOutStream, proxyErrStream);
        
        sendTestMessages(logger);
        
        String outMessage = proxyOut.toString();
        String errMessage = proxyErr.toString();
        
        assertEquals(0, countOccurence(outMessage, "trace-message"));
        assertEquals(0, countOccurence(outMessage, "trace-exception"));
        assertEquals(0, countOccurence(outMessage, "debug-message"));
        assertEquals(0, countOccurence(outMessage, "debug-exception"));
        assertEquals(0, countOccurence(outMessage, "info-message"));
        assertEquals(0, countOccurence(outMessage, "info-exception"));
        assertEquals(0, countOccurence(errMessage, "warn-message"));
        assertEquals(0, countOccurence(errMessage, "warn-exception"));
        assertEquals(0, countOccurence(errMessage, "error-message"));
        assertEquals(0, countOccurence(errMessage, "error-exception"));
        assertEquals(0, countOccurence(errMessage, "fatal-message"));
        assertEquals(0, countOccurence(errMessage, "fatal-exception"));
    }
    
    private void sendTestMessages(Logger logger) {
        logger.trace("trace-message");
        logger.trace("trace-message", new Exception("trace-exception"));
        
        logger.debug("debug-message");
        logger.debug("debug-message", new Exception("debug-exception"));
        
        logger.info("info-message");
        logger.info("info-message", new Exception("info-exception"));
        
        logger.warn("warn-message");
        logger.warn("warn-message", new Exception("warn-exception"));
        
        logger.error("error-message");
        logger.error("error-message", new Exception("error-exception"));
        
        logger.fatal("fatal-message");
        logger.fatal("fatal-message", new Exception("fatal-exception"));
    }
    
    
    
    
    
    private class ProxyFormatter extends Formatter {
        private Set<LogRecord> receievedRecords = new HashSet<LogRecord>();
        @Override
        public String format(LogRecord record) {
            receievedRecords.add(record);
            return "";
        }
        
    }
    
    private static int countOccurence(String input, String phase) {
        int occurence = 0;
        
        int index;
        
        while ((index = input.indexOf(phase)) != -1) {
            occurence ++;
            input = input.substring(index + phase.length());
        }
        
        return occurence;
    }

    private static LogSetting getLogSetting(Level logLevel) {
        return new LogSetting(logLevel, true, true, null, null, null);
    }
    
}
