package com.tracelytics.agent.config;

import com.tracelytics.agent.ResourceDirectory;
import com.tracelytics.joboe.config.InvalidConfigException;
import com.tracelytics.logging.Logger;
import com.tracelytics.logging.setting.LogSetting;
import junit.framework.TestCase;

import java.nio.file.Path;
import java.nio.file.Paths;

public class LogSettingParserTest extends TestCase {
    
    public void testConfigureValidConfig() throws Exception {
        LogSetting setting;

        setting = LogSettingParser.INSTANCE.convert("fatal"); //simple string form
        assertEquals(Logger.Level.FATAL, setting.getLevel());
        assertEquals(null, setting.getLogFilePath());
        assertEquals(LogSettingParser.DEFAULT_STDERR_ENABLED, setting.isStderrEnabled());
        assertEquals(LogSettingParser.DEFAULT_STDOUT_ENABLED, setting.isStdoutEnabled());
        assertEquals(LogSetting.DEFAULT_FILE_MAX_BACKUP, setting.getLogFileMaxBackup());
        assertEquals(LogSetting.DEFAULT_FILE_MAX_SIZE, setting.getLogFileMaxSize());

        setting = LogSettingParser.INSTANCE.convert("{\"level\" : \"info\", \"file\" : { \"location\" : \"appoptics.log\"}}");
        assertEquals(Logger.Level.INFO, setting.getLevel());
        assertEquals(true, setting.getLogFilePath().toString().endsWith("appoptics.log"));
        assertEquals(LogSettingParser.DEFAULT_STDERR_ENABLED, setting.isStderrEnabled());
        assertEquals(LogSettingParser.DEFAULT_STDOUT_ENABLED, setting.isStdoutEnabled());
        assertEquals(LogSetting.DEFAULT_FILE_MAX_BACKUP, setting.getLogFileMaxBackup());
        assertEquals(LogSetting.DEFAULT_FILE_MAX_SIZE, setting.getLogFileMaxSize());

        setting = LogSettingParser.INSTANCE.convert("{\"level\" : \"warn\", \"stderr\" : \"disabled\", \"stdout\" : \"disabled\", \"file\" : { \"location\" : \"appoptics.log\", maxSize : 1024, maxBackup : 50}}");
        assertEquals(Logger.Level.WARNING, setting.getLevel());
        assertEquals(true, setting.getLogFilePath().toString().endsWith("appoptics.log"));
        assertEquals(false, setting.isStderrEnabled());
        assertEquals(false, setting.isStdoutEnabled());
        assertEquals(50, setting.getLogFileMaxBackup());
        assertEquals(1024, setting.getLogFileMaxSize());
    }
    
    public void testConfigureInvalidConfig() throws Exception {
        try {
            LogSettingParser.INSTANCE.convert("{}"); //should at least have level
            fail("Expect " + InvalidConfigException.class.getName() + " but found none");
        } catch (InvalidConfigException e) {
        }

        //no unknown key check currently
//        try {
//            LogSettingParser.INSTANCE.convert("{\"level\" : \"info\", \"file\" : { \"location\" : \"appoptics.log\"}, \"unknown\" : false}"); //unknown key
//            fail("Expect " + InvalidConfigException.class.getName() + " but found none");
//        } catch (InvalidConfigException e) {
//        }

        //TODO should fail the case below: see https://swicloud.atlassian.net/browse/AO-15729
//        try {
//            LogSettingParser.INSTANCE.convert("{\"level\" : \"info\", \"file\" : \"appoptics.log\"}"); //file should be file : { location : value}
//            fail("Expect " + InvalidConfigException.class.getName() + " but found none");
//        } catch (InvalidConfigException e) {
//        }

        try {
            LogSettingParser.INSTANCE.convert("{\"level\" : \"warning\"}"); //unknown level
            fail("Expect " + InvalidConfigException.class.getName() + " but found none");
        } catch (InvalidConfigException e) {
        }

        try {
            LogSettingParser.INSTANCE.convert("{\"level\" : \"warn\", \"stderr\" : \"disabled\", \"stdout\" : \"disabled\", \"file\" : { \"location\" : \"appoptics.log\", maxSize : true, maxBackup : 5}}"); //maxSize expect number
            fail("Expect " + InvalidConfigException.class.getName() + " but found none");
        } catch (InvalidConfigException e) {
        }

        try {
            LogSettingParser.INSTANCE.convert("{\"level\" : \"warn\", \"stderr\" : \"disabled\", \"stdout\" : \"disabled\", \"file\" : { \"location\" : \"appoptics.log\", maxSize : 2048, maxBackup : 5}}"); //maxSize out of range
            fail("Expect " + InvalidConfigException.class.getName() + " but found none");
        } catch (InvalidConfigException e) {
        }
    }


    
    
}
