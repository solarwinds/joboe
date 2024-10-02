package com.tracelytics.agent;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import com.tracelytics.instrumentation.Module;
import com.tracelytics.instrumentation.config.HideParamsConfig;
import com.tracelytics.joboe.StartupManager;
import com.tracelytics.joboe.TracingMode;
import com.tracelytics.joboe.config.ConfigContainer;
import com.tracelytics.joboe.config.ConfigManager;
import com.tracelytics.joboe.config.ConfigProperty;
import com.tracelytics.joboe.config.InvalidConfigException;
import com.tracelytics.joboe.rpc.ClientException;

import com.tracelytics.logging.Logger;
import com.tracelytics.logging.setting.LogSetting;
import junit.framework.TestCase;

public class AgentTest extends TestCase {

    private static final String TEST_CONFIG_FOLDER = "src/test/java/com/tracelytics/agent/";
    

    private ClassLoader testClassLoader;

    static {
        Agent.initConfigPropertyParser();
    }
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }
    

    /**
     * Tests on no -javaagent arguments
     * @throws Exception
     */
    //TODO disabled test case as it relies heavily on the local setup on the machine, which is not ideal
//    public void testReadConfigsNoArguments() throws Exception {
//        boolean hasDefaultConfigFile = new File(ResourceDirectory.getJavaAgentConfigLocation()).exists();
//        
//         try {
//            ConfigContainer configs = Agent.readConfigs(null); 
//            
//            if (!hasDefaultConfigFile) {
//                //should throws exception! as the default config file is not found
//                fail("Expected InvalidConfigException to be thrown!");
//            }
//        } catch (InvalidConfigException e) {
//            if (hasDefaultConfigFile) { //then it shouldnt fail if default file is found
//                fail("config file [" + ResourceDirectory.getJavaAgentConfigLocation() + "] exists but readConfigs fails to process it! Message: " + e.getMessage());
//            }
//        }
//    }
    
    
    /**
     * Tests on setting all variables to non-defaults. Take note that the agent argument values should not get overridden by config file "default.json"
     * @throws Exception
     */
    public void testReadConfigsValidAgentArguments() throws Exception {
        ConfigContainer configs = Agent.readConfigs("sample_rate=500000,debug=true,tracing_mode=never,layer=myLayer,jdbc_inst_all=true,config=" + TEST_CONFIG_FOLDER + "default.json", System.getenv()); 
        
        assertEquals(true, configs.get(ConfigProperty.AGENT_DEBUG));
        assertEquals(TracingMode.NEVER, configs.get(ConfigProperty.AGENT_TRACING_MODE));
        assertEquals(500000, configs.get(ConfigProperty.AGENT_SAMPLE_RATE));
        assertEquals("myLayer", configs.get(ConfigProperty.AGENT_LAYER));
        assertEquals(true, configs.get(ConfigProperty.AGENT_JDBC_INST_ALL));
    }
    
    /**
     * Tests on reading config with the highest priority environment variable
     * @throws Exception
     */
    public void testReadConfigsEnvironmentVariable() throws Exception {
        ConfigContainer configs = Agent.readConfigs("config=" + TEST_CONFIG_FOLDER + "default.json", Collections.singletonMap(ConfigProperty.AGENT_SERVICE_KEY.getEnviromentVariableKey(), "ddd"));
        
        assertEquals("ddd", configs.get(ConfigProperty.AGENT_SERVICE_KEY));
        assertEquals(new LogSetting(Logger.Level.INFO, true, true, null, null, null), configs.get(ConfigProperty.AGENT_LOGGING));
        assertEquals(null, configs.get(ConfigProperty.AGENT_TRACING_MODE));
        assertNull(configs.get(ConfigProperty.AGENT_LAYER));
        assertEquals(false, configs.get(ConfigProperty.AGENT_JDBC_INST_ALL));
        assertEquals(1, configs.get(ConfigProperty.AGENT_SQL_SANITIZE));
        
        String scopeString = (String) configs.get(ConfigProperty.MONITOR_JMX_SCOPES);
        assertTrue(scopeString.contains("\"java.lang:type=MemoryPool,*\":[\"Usage\"]"));
        assertTrue(scopeString.contains("\"java.lang:type=Memory\":[\"HeapMemoryUsage\",\"NonHeapMemoryUsage\"]"));
        assertTrue(scopeString.contains("\"java.lang:type=GarbageCollector,*\":[\"CollectionTime\"]"));
        assertTrue(scopeString.contains("\"java.lang:type=Threading\":[\"ThreadCount\"]"));
        assertTrue(scopeString.contains("\"java.lang:type=OperatingSystem\":[\"ProcessCpuTime\",\"AvailableProcessors\",\"ProcessCpuLoad\"]"));
        assertTrue(scopeString.contains("\"java.lang:type=Runtime,*\":[\"Uptime\"]"));
        
        assertEquals(true, configs.get(ConfigProperty.MONITOR_JMX_ENABLE));    }
    
    
    /**
     * Tests on setting all variables using invalid values
     * @throws Exception
     */
    public void testReadConfigsInvalidAgentArguments() throws Exception {
        try {
            ConfigContainer configs = Agent.readConfigs("sample_rate=invalid,debug=invalid,tracing_mode=invalid,trace_ajax=invalid,jdbc_inst_all=invalid,config=" + TEST_CONFIG_FOLDER + "default.json", System.getenv());
            
            //should throws exception! Fails if it runs to here
            fail("Expected InvalidConfigException to be thrown!");
        } catch (InvalidConfigException e) {
            //expected
        }
    }
    
    /**
     * Tests on empty config file
     * @throws Exception
     */
    public void testReadConfigsEmptyConfig() throws Exception {
        try {
            ConfigContainer configs = Agent.readConfigs("config=" + TEST_CONFIG_FOLDER + "empty.json", System.getenv());
          //should throws exception! Fails if it runs to here
            fail("Expected InvalidConfigException to be thrown!");
        } catch (InvalidConfigException e) {
            //expected
        }
    }
    
    /**
     * Tests on config file that is not found
     * @throws Exception
     */
    public void testReadConfigsNotFoundConfig() throws Exception {
        try {
            ConfigContainer configs = Agent.readConfigs("config=" + TEST_CONFIG_FOLDER + "notFound.json", System.getenv());
          //should throws exception! Fails if it runs to here
            fail("Expected InvalidConfigException to be thrown!");
        } catch (InvalidConfigException e) {
            //expected
        }
    }
    
    /**
     * Tests on config with default values
     * @throws Exception
     */
    public void testReadConfigsDefaultConfig() throws Exception {
        ConfigContainer configs = Agent.readConfigs("config=" + TEST_CONFIG_FOLDER + "default.json", System.getenv());
        
        assertEquals(new LogSetting(Logger.Level.INFO, true, true, null, null, null), configs.get(ConfigProperty.AGENT_LOGGING));
        assertEquals(null, configs.get(ConfigProperty.AGENT_TRACING_MODE));
        assertEquals("some key", configs.get(ConfigProperty.AGENT_SERVICE_KEY));
        assertNull(configs.get(ConfigProperty.AGENT_LAYER));
        assertEquals(false, configs.get(ConfigProperty.AGENT_JDBC_INST_ALL));
        assertEquals(1, configs.get(ConfigProperty.AGENT_SQL_SANITIZE));
        
        String scopeString = (String) configs.get(ConfigProperty.MONITOR_JMX_SCOPES);
        assertTrue(scopeString.contains("\"java.lang:type=MemoryPool,*\":[\"Usage\"]"));
        assertTrue(scopeString.contains("\"java.lang:type=Memory\":[\"HeapMemoryUsage\",\"NonHeapMemoryUsage\"]"));
        assertTrue(scopeString.contains("\"java.lang:type=GarbageCollector,*\":[\"CollectionTime\"]"));
        assertTrue(scopeString.contains("\"java.lang:type=Threading\":[\"ThreadCount\"]"));
        assertTrue(scopeString.contains("\"java.lang:type=OperatingSystem\":[\"ProcessCpuTime\",\"AvailableProcessors\",\"ProcessCpuLoad\"]"));
        assertTrue(scopeString.contains("\"java.lang:type=Runtime,*\":[\"Uptime\"]"));
        
        assertEquals(true, configs.get(ConfigProperty.MONITOR_JMX_ENABLE));
    }
    
       
    /**
     * Tests on config with valid values
     * @throws Exception
     */
    public void testReadConfigsValidConfig() throws Exception {
        ConfigContainer configs = Agent.readConfigs("config=" + TEST_CONFIG_FOLDER + "test.json", System.getenv());
        
        assertEquals("abc1234", configs.get(ConfigProperty.AGENT_SERVICE_KEY));
        assertEquals(new LogSetting(Logger.Level.TRACE, true, true, null, null, null), configs.get(ConfigProperty.AGENT_LOGGING));
        assertEquals(TracingMode.NEVER, configs.get(ConfigProperty.AGENT_TRACING_MODE));
        assertEquals(200000, configs.get(ConfigProperty.AGENT_SAMPLE_RATE));
        assertEquals("MyLayer(File)", configs.get(ConfigProperty.AGENT_LAYER));
        assertEquals(true, configs.get(ConfigProperty.AGENT_JDBC_INST_ALL));
        assertEquals(1, configs.get(ConfigProperty.AGENT_SQL_SANITIZE));
        assertEquals(true, configs.get(ConfigProperty.AGENT_MONGO_SANITIZE));
        assertEquals("[\"org.jboss\"]", configs.get(ConfigProperty.AGENT_EXCLUDE_CLASSES));
        assertEquals("[\"HBASE\",\"MONGODB\",\"WEBFLOW\"]", configs.get(ConfigProperty.AGENT_EXCLUDE_MODULES));
        assertNotNull(configs.get(ConfigProperty.AGENT_URL_SAMPLE_RATE));
        assertEquals("p1,p2", configs.get(ConfigProperty.AGENT_TRANSACTION_NAME_PATTERN));
        assertEquals(true, configs.get(ConfigProperty.AGENT_EXTENDED_BACK_TRACES));
        assertEquals(Arrays.asList(Module.PLAY, Module.SLING), configs.get(ConfigProperty.AGENT_EXTENDED_BACK_TRACES_BY_MODULE));
        assertEquals(true, configs.get(ConfigProperty.AGENT_HBASE_SCANNER_NEXT));
        HideParamsConfig hideParamsConfig = (HideParamsConfig) configs.get(ConfigProperty.AGENT_HIDE_PARAMS);
        assertEquals(true, hideParamsConfig.shouldHideParams(Module.SERVLET));
        assertEquals(true, hideParamsConfig.shouldHideParams(Module.NETTY));
        assertEquals(false, hideParamsConfig.shouldHideParams(Module.WEB_SERVICE));
        assertEquals(2, ((String[])configs.get(ConfigProperty.AGENT_AKKA_ACTORS)).length);
        assertEquals(100, configs.get(ConfigProperty.AGENT_TIME_ADJUST_INTERVAL));
        assertEquals(200, configs.get(ConfigProperty.AGENT_CONTEXT_TTL));
        assertEquals(Arrays.asList(Module.SERVLET, Module.JDBC), configs.get(ConfigProperty.AGENT_BACKTRACE_MODULES));
        assertEquals(false, configs.get(ConfigProperty.AGENT_TRIGGER_TRACE_ENABLED));
        
        String scopeString = (String) configs.get(ConfigProperty.MONITOR_JMX_SCOPES);
        assertTrue(scopeString.contains("\"java.lang:type=MemoryPool,*\":[\"Usage\"]"));
        assertTrue(scopeString.contains("\"java.lang:type=Memory\":[\"HeapMemoryUsage\",\"NonHeapMemoryUsage\"]"));
        
        assertFalse((Boolean) configs.get(ConfigProperty.MONITOR_JMX_ENABLE));
        assertEquals(500, configs.get(ConfigProperty.MONITOR_JMX_MAX_ENTRY));
        
        assertFalse((Boolean) configs.get(ConfigProperty.MONITOR_SPAN_METRICS_ENABLE));
    }


    /**
     * Tests on config with invalid values
     * @throws Exception
     */
    public void testReadConfigsInvalidValuesConfig() throws Exception {
        try {
            ConfigContainer configs = Agent.readConfigs("config=" + TEST_CONFIG_FOLDER + "invalidValues.json", System.getenv());
            
          //should throws exception! Fails if it runs to here
            fail("Expected InvalidConfigException to be thrown!");
        } catch (InvalidConfigException e) {
            //expected
        }
    }
    
    /**
     * Tests on config with invalid keys
     * @throws Exception
     */
    public void testReadConfigsInvalidKeysConfig() throws Exception {
        try {
            ConfigContainer configs = Agent.readConfigs("config=" + TEST_CONFIG_FOLDER + "invalidKeys.json", System.getenv());
        
          //should throws exception! Fails if it runs to here
            fail("Expected InvalidConfigException to be thrown!");
        } catch (InvalidConfigException e) {
            //expected
        }
    }
    
    /**
     * Tests on config with invalid Json format
     * @throws Exception
     */
    public void testReadConfigsInvalidFormatConfig() throws Exception {
        try {
            ConfigContainer configs = Agent.readConfigs("config=" + TEST_CONFIG_FOLDER + "invalidFormat.json", System.getenv());
          //should throws exception! Fails if it runs to here
            fail("Expected InvalidConfigException to be thrown!");
        } catch (InvalidConfigException e) {
            //expected
        }
    }
    
    /**
     * Tests on config with default values
     * @throws Exception
     */
    public void testReadConfigsInvalidParamsValuesConfig() throws Exception {
        try {
            ConfigContainer configs = Agent.readConfigs("config=" + TEST_CONFIG_FOLDER + "invalidHideParamsValues.json", System.getenv());
            
          //should throws exception! Fails if it runs to here
            fail("Expected InvalidConfigException to be thrown!");
        } catch (InvalidConfigException e) {
            //expected
        }
    }
    
    
    /**
     * Tests on config with missing keys
     * @throws Exception
     */
    public void testReadConfigsMissingKeysConfig() throws Exception {
        try {
            ConfigContainer configs = Agent.readConfigs("config=" + TEST_CONFIG_FOLDER + "missingKeys.json", System.getenv());
          //should throws exception! Fails if it runs to here
            fail("Expected InvalidConfigException to be thrown!");
        } catch (InvalidConfigException e) {
            //expected
        }
    }

    
    
    /**
    * Tests on initializeFields with default values except trace mode, which is set to NEVER. 
    * 
    * @throws Exception
    */
   public void testProcessConfigsTraceModeNever() throws Exception {
       Class<?> agentClass = Agent.class;

       //should run premain too as the referencing class has run it
       agentClass.getDeclaredMethod("premain", String.class, Instrumentation.class).invoke(null, "config=" + TEST_CONFIG_FOLDER + "default.json", null);
       
       Method processConfigsMethod = agentClass.getDeclaredMethod("processConfigs", ConfigContainer.class);

       ConfigContainer configContainer = new ConfigContainer();
       configContainer.put(ConfigProperty.AGENT_TRACING_MODE, TracingMode.NEVER);
       configContainer.put(ConfigProperty.AGENT_SERVICE_KEY, "some key");

     //initialize field. Use reflection as we do not want to modify the static fields in Agent
       processConfigsMethod.setAccessible(true);
       processConfigsMethod.invoke(null, configContainer);
       
       assertEquals(TracingMode.NEVER, ConfigManager.getConfig(ConfigProperty.AGENT_TRACING_MODE));
       assertEquals(null, ConfigManager.getConfig(ConfigProperty.AGENT_SAMPLE_RATE));
   }
   

    /**
     * Tests on processConfigs with valid but non default values in container
     * @throws Exception
     */
    public void testProcessConfigs() throws Exception {
        Class<?> agentClass = Agent.class;

        //should run premain too as the referencing class has run it
        agentClass.getDeclaredMethod("premain", String.class, Instrumentation.class).invoke(null, "config=" + TEST_CONFIG_FOLDER + "default.json", null);

        Method processConfigsMethod = agentClass.getDeclaredMethod("processConfigs", ConfigContainer.class);

        //"sample_rate=500000,debug=true,tracing_mode=through,application=myApp,trace_ajax=true,access_key=123,layer=myLayer,jdbc_inst_all=true"
        ConfigContainer configContainer = new ConfigContainer();
        configContainer.put(ConfigProperty.AGENT_SAMPLE_RATE, 500000);
        configContainer.put(ConfigProperty.AGENT_DEBUG, true);
        configContainer.put(ConfigProperty.AGENT_SERVICE_KEY, "123");
        configContainer.put(ConfigProperty.AGENT_LAYER, "myLayer");
        configContainer.put(ConfigProperty.AGENT_JDBC_INST_ALL, true);

        processConfigsMethod.setAccessible(true);
        processConfigsMethod.invoke(null, configContainer);

        assertEquals(500000, ConfigManager.getConfig(ConfigProperty.AGENT_SAMPLE_RATE));
        assertEquals(true, ConfigManager.getConfig(ConfigProperty.AGENT_DEBUG));
        assertEquals("123", ConfigManager.getConfig(ConfigProperty.AGENT_SERVICE_KEY));
        assertEquals("myLayer", ConfigManager.getConfig(ConfigProperty.AGENT_LAYER));
        assertEquals(true, ConfigManager.getConfig(ConfigProperty.AGENT_JDBC_INST_ALL));
    }

    public void testLayerInit() throws IOException, ClientException {
        final String LAYER_NAME = "";
        final String JAVA_VERSION = "6.0";
        final long START_TIME = Agent.currentTimeStamp();
        final String ERROR_MESSAGE = "test error";
        Map<String, Object> initMessage = Agent.buildInitMessage(LAYER_NAME, JAVA_VERSION, ERROR_MESSAGE, START_TIME);
        
        assertEquals(LAYER_NAME, initMessage.get("Layer"));
        assertEquals("single", initMessage.get("Label"));
        assertEquals(true, initMessage.get("__Init"));
        assertEquals(JAVA_VERSION, initMessage.get("Java.AppOptics.Version"));
        File agentJarPath = ResourceDirectory.getAgentJarPath();
        if (agentJarPath != null) {
            assertEquals(agentJarPath.getAbsolutePath(), initMessage.get("Java.InstallDirectory"));
            assertEquals(agentJarPath.lastModified() / 1000, initMessage.get("Java.InstallTimestamp"));
        }
        assertTrue(initMessage.get("Java.LastRestart") != null);
        assertEquals(ERROR_MESSAGE, initMessage.get("Error"));
    }
}
