package com.tracelytics.agent.config;

import com.tracelytics.agent.config.LogTraceIdSettingParser;
import com.tracelytics.joboe.JoboeTest;
import com.tracelytics.joboe.config.InvalidConfigException;
import com.tracelytics.joboe.config.LogTraceIdScope;
import com.tracelytics.joboe.config.LogTraceIdSetting;


public class LogTraceIdSettingParserTest extends JoboeTest {
    public void testInvalidJson() {
        //test empty config entry
        try {
            LogTraceIdSettingParser.INSTANCE.convert("{}");
            fail("json is invalid, it should throw exceptions");
        } catch (InvalidConfigException e) {
            System.out.println("Found expected exception. Message: " + e.getMessage());
        }
        
        //test empty config key
        try {
            LogTraceIdSettingParser.INSTANCE.convert("{ \"xyz\" : \"sampledOnly\" }");
            fail("json is invalid, it should throw exceptions");
        } catch (InvalidConfigException e) {
            System.out.println("Found expected exception. Message: " + e.getMessage());
        }

        //test invalid value type
        try {
            LogTraceIdSettingParser.INSTANCE.convert("{ \"mdc\":true }");
            fail("json is invalid, it should throw exceptions");
        } catch (InvalidConfigException e) {
            System.out.println("Found expected exception. Message: " + e.getMessage());
        }
        
        //test invalid value enum
        try {
            LogTraceIdSettingParser.INSTANCE.convert("{ \"mdc\":\"yes\" }");
            fail("json is invalid, it should throw exceptions");
        } catch (InvalidConfigException e) {
            System.out.println("Found expected exception. Message: " + e.getMessage());
        }
    }

    public void testValidJson() throws Exception {
        LogTraceIdSetting config = LogTraceIdSettingParser.INSTANCE.convert("{ \"autoInsert\" : \"sampledOnly\" }");
        assertNotNull(config);
        assertEquals(LogTraceIdScope.SAMPLED_ONLY, config.getAutoInsertScope());
        assertEquals(LogTraceIdScope.DISABLED, config.getMdcScope()); 
        
        config = LogTraceIdSettingParser.INSTANCE.convert("{ \"mdc\" : \"enabled\" }");
        assertNotNull(config);
        assertEquals(LogTraceIdScope.DISABLED, config.getAutoInsertScope()); //default as disabled
        assertEquals(LogTraceIdScope.ENABLED, config.getMdcScope()); 
        
        config = LogTraceIdSettingParser.INSTANCE.convert("{ \"mdc\" : \"enabled\", \"autoInsert\" : \"disabled\" }");
        assertNotNull(config);
        assertEquals(LogTraceIdScope.DISABLED, config.getAutoInsertScope()); //default as disabled
        assertEquals(LogTraceIdScope.ENABLED, config.getMdcScope());
        
        config = LogTraceIdSettingParser.INSTANCE.convert("{ \"mdc\" : \"enabled\", \"autoInsert\" : \"enabled\" }");
        assertNotNull(config);
        assertEquals(LogTraceIdScope.ENABLED, config.getAutoInsertScope()); //default as disabled
        assertEquals(LogTraceIdScope.ENABLED, config.getMdcScope());
        
        config = LogTraceIdSettingParser.INSTANCE.convert("{ \"mdc\":\"disabled\", \"autoInsert\":\"enabled\" }");
        assertNotNull(config);
        assertEquals(LogTraceIdScope.ENABLED, config.getAutoInsertScope()); 
        assertEquals(LogTraceIdScope.DISABLED, config.getMdcScope());
        
        
    }
    
    
   
}

