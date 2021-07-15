/**
 * Java Instrumentation Agent
 */

package com.tracelytics.agent;

import com.tracelytics.agent.config.*;
import com.tracelytics.ext.javassist.bytecode.ClassFile;
import com.tracelytics.joboe.config.*;
import com.tracelytics.joboe.rpc.*;
import com.tracelytics.joboe.rpc.RpcClientManager.OperationType;
import com.tracelytics.logging.Logger;
import com.tracelytics.logging.LoggerFactory;
import com.tracelytics.monitor.SystemMonitorController;
import com.tracelytics.util.JavaProcessUtils;
import com.tracelytics.util.ServiceKeyUtils;
import com.tracelytics.util.TimeUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.tracelytics.agent.config.ConfigConstants.MAX_SQL_QUERY_LENGTH_LOWER_LIMIT;
import static com.tracelytics.agent.config.ConfigConstants.MAX_SQL_QUERY_LENGTH_UPPER_LIMIT;

public class Agent implements ClassFileTransformer {

    public static final int SAMPLE_RESOLUTION = 1000000;
    private static final int DEFAULT_INIT_MESSAGE_TIMEOUT = 10; //wait for up to 10 seconds to send init message
    public static final int MIN_SUPPORTED_JAVA_MAJOR_VERSION = ClassFile.JAVA_6;
    
    // Parameters updated through config options:
    public static String version = null;

    protected static AgentClassFileTransformer agentClassFileTransformer = null;
    protected static AgentStatus status = AgentStatus.UNINITIALIZED;
    private static final Logger logger = LoggerFactory.getLogger();

    private static final String VERSIONS_PROPERTIES_FILE = "/versions.properties";
    
    private static String initErrorMessage;
    
    private static long startTime;

    private static AtomicBoolean reportedAgentInit = new AtomicBoolean(false);

    private static String configLocation;
    
    public static void premain(String agentArgs, Instrumentation inst) {
        if (status == AgentStatus.INITIALIZED_FAILED || status == AgentStatus.INITIALIZED_SUCCESSFUL) {
            logger.warn("WARNING: Agent has already been initialized: Check command line parameters to make sure -javaagent is only specified once.");
            return;
        }
        
        try {
            //invoke getPid() here to avoid triggering instrumentation that causes deadlocking, see https://github.com/librato/joboe/pull/768
            JavaProcessUtils.getPid(); 
            
            Properties versionsProperties = new Properties();
            versionsProperties.load(Agent.class.getResourceAsStream(VERSIONS_PROPERTIES_FILE));
            version = versionsProperties.getProperty("agent.version");
            if (version == null) {
                logger.warn("Could not locate agent.version in " + VERSIONS_PROPERTIES_FILE + " for version. Agent not starting...");
                return;
            }
            final String SNAPSHOT_SUFFIX = "-SNAPSHOT"; 
            if (version.endsWith(SNAPSHOT_SUFFIX)) {
                version = version.substring(0, version.length() - SNAPSHOT_SUFFIX.length());
            }
            
            initConfig(agentArgs);
    
            Boolean enabled = (Boolean)ConfigManager.getConfig(ConfigProperty.AGENT_ENABLED);
            enabled = (enabled == null?true:enabled);
            
            if(!enabled) {
                status = AgentStatus.DISABLED;
                logger.info("Agent is disabled as agent.enabled is configured to false.");
                return;
            }
            
            if (!supportJavaVersion()) {
                status = AgentStatus.DISABLED;
                logger.info("Agent is disabled as it only supports Java Major version " + MIN_SUPPORTED_JAVA_MAJOR_VERSION + " or later, but found " + ClassFile.MAJOR_VERSION);
                return;
            }

            if (inst == null) {
                logger.info("premain() Instrumentation parameter is null, not setting agent");
            } else {
                agentClassFileTransformer = ClassFile.MAJOR_VERSION >= ClassFile.JAVA_9 ? new AgentModuleClassFileTransformer(inst) : new AgentClassFileTransformer(inst);
                try {
                    inst.addTransformer(agentClassFileTransformer, true);
                } catch (NoSuchMethodError e) {
                    logger.info("Running on jdk 1.5 or earlier, would not attempt to retransform classes");
                    inst.addTransformer(agentClassFileTransformer);
                }
            }
            printStartupMessage(AgentStatus.INITIALIZED_SUCCESSFUL);
            status = AgentStatus.INITIALIZED_SUCCESSFUL;
        } catch(Throwable ex) {
            logger.error("Exception during initialization : " + ex.getMessage(), ex);
            
            if (ex.getMessage() != null) {
                initErrorMessage = ex.getMessage();
            } else {
                initErrorMessage = "Exception thrown [" + ex.getClass().getName() + "]";
            }
            printStartupMessage(AgentStatus.INITIALIZED_FAILED);
            status = AgentStatus.INITIALIZED_FAILED;
        } finally {
            try {
                if (getStatus() != AgentStatus.DISABLED) {
                    startTime = currentTimeStamp();

                    //delay layer init reporting as doing it in premain has conflicts
                    // with Jboss (LogManager) : please see https://github.com/librato/joboe/issues/542 and https://github.com/librato/joboe/issues/692
                    // with JDK 6- (Class loading deadlock): please see https://github.com/librato/joboe/pull/702 and https://github.com/librato/joboe/issues/704

                    if (!"jboss".equals(AppEnvironment.getAppServerName()) && ClassFile.MAJOR_VERSION >= ClassFile.JAVA_7) {
                        reportInit(true);
                    }

                    if (getStatus() == AgentStatus.INITIALIZED_SUCCESSFUL) {
                        // try to start up the system monitor here, if possible
                        SystemMonitorController.conditionalStart();
                    }
                }
            } catch (Throwable t) {
                logger.warn("Failed to execute finally block in premain: " + t.getMessage(), t);
                status = AgentStatus.INITIALIZED_FAILED;
            }
        }
    }
    
    private static boolean supportJavaVersion() {
        //this might throws UnsupportedClassVersionError on pre-proguard agent, as ClassFile.MAJOR_VERSION has major.minor version 51.0 (java 1.7)
        //however the post-proguard agent should work fine as proguard change the target version to 1.5
        return ClassFile.MAJOR_VERSION >= MIN_SUPPORTED_JAVA_MAJOR_VERSION; 
    }

    /**
     * Initializes the agent config with agentArgs
     */
    public static void initConfig(String agentArgs) throws InvalidConfigException {
        initConfig(agentArgs, null);
    }
    
    /**
     * Initializes the agent config with agentArgs with an explicit service key (used by diagnostic tools to overwrite service key)
     */
    public static void initConfig(String agentArgs, String explicitServiceKey) throws InvalidConfigException {
        ConfigContainer configs = null;
        boolean hasReadConfigException = false;
        try {
            initConfigPropertyParser();
            configs = readConfigs(agentArgs, System.getenv(), explicitServiceKey);
        } catch (InvalidConfigException e) {
            hasReadConfigException = true;
            //attempt to initialize the logger factory, as it could contain valid logging config and it's valuable to log message to it if possible
            if (e instanceof InvalidConfigReadSourceException) {
                configs = ((InvalidConfigReadSourceException) e).getConfigContainerBeforeException(); //try with partial config, even though we will fail the agent (throwing exception), config such as service key is still useful for reporting failure
            }
            throw e; //rethrow the exception
        } finally {
            if (configs != null) {
                LoggerFactory.init(configs.subset(ConfigGroup.AGENT)); //initialize the logger factory as soon as the config is available
                try {
                    processConfigs(configs);
                } catch (InvalidConfigException e) {
                    //if there was a config read exception then processConfigs might throw exception due to incomplete config container.
                    //Do NOT override the original exception by not rethrowing the exception
                    if (!hasReadConfigException) {
                        throw e;
                    }
                }
            }
        }
    }

    /**
     * Registers special parsers to use for certain ConfigProperty.
     *
     * As config reading and parsing are more dependent on the actual agent implementation,
     * therefore we register them in the agent project instead of the core
     */
    static void initConfigPropertyParser() {
        ConfigProperty.AGENT_LOGGING.setParser(LogSettingParser.INSTANCE);
        ConfigProperty.AGENT_LOG_FILE.setParser(new LogFileStringParser());
        ConfigProperty.AGENT_LOGGING_TRACE_ID.setParser(LogTraceIdSettingParser.INSTANCE);
        ConfigProperty.AGENT_TRACING_MODE.setParser(new TracingModeParser());
        ConfigProperty.AGENT_SQL_QUERY_MAX_LENGTH.setParser(new RangeValidationParser<Integer>(MAX_SQL_QUERY_LENGTH_LOWER_LIMIT, MAX_SQL_QUERY_LENGTH_UPPER_LIMIT));
        ConfigProperty.AGENT_URL_SAMPLE_RATE.setParser(UrlSampleRateConfigParser.INSTANCE);
        ConfigProperty.AGENT_BACKTRACE_MODULES.setParser(ModulesParser.INSTANCE);
        ConfigProperty.AGENT_EXTENDED_BACK_TRACES_BY_MODULE.setParser(ModulesParser.INSTANCE);
        ConfigProperty.AGENT_HIDE_PARAMS.setParser(new HideParamsConfigParser());
        ConfigProperty.AGENT_TRANSACTION_SETTINGS.setParser(TransactionSettingsConfigParser.INSTANCE);
        ConfigProperty.AGENT_TRIGGER_TRACE_ENABLED.setParser(ModeStringToBooleanParser.INSTANCE);
        ConfigProperty.AGENT_PROXY.setParser(ProxyConfigParser.INSTANCE);
        ConfigProperty.PROFILER.setParser(ProfilerSettingParser.INSTANCE);
    }

    private static void printStartupMessage(AgentStatus startupStatus) {
        if (startupStatus == AgentStatus.INITIALIZED_SUCCESSFUL) {
            logger.info("Java agent version " + getVersion() + " started successfully");
        } else {
            logger.info("Java agent version " + getVersion() + " failed to start");
        }

        File agentJarPath = ResourceDirectory.getAgentJarPath();
        logger.info("Java agent jar location: " + (agentJarPath != null ? agentJarPath.toString() : "unresolved"));
        logger.info("Java agent config location: " + (configLocation != null ? configLocation : "unresolved"));
        String serviceKey = (String) ConfigManager.getConfig(ConfigProperty.AGENT_SERVICE_KEY);
        if (serviceKey != null) {
            String serviceName = ServiceKeyUtils.getServiceName(serviceKey);
            if (serviceName != null) {
                logger.info("Java agent service name: " + serviceName);
            }
        }
    }

    /**
     * Gets current timestamp in microseconds
     * @return
     */
    public static long currentTimeStamp() {
     //   return agentInfo.getCurrentTimestamp();
        return TimeUtils.getTimestampMicroSeconds();
    }

    public static AgentStatus getStatus() {
        return status;
    }
    
    /**
     * Retrieves the agent current version. Could return null if agent was not initialized properly
     * @return
     */
    public static String getVersion() {
        return version;
    }
    
    

    static ConfigContainer readConfigs(String agentArgs, Map<String, String> env) throws InvalidConfigException {
        return readConfigs(agentArgs, env, null);
    }
    
    /**
     * Collect configuration properties from both the -javaagent arguments and the configuration property file
     * 
     * 
     * @param agentArgs                 the arguments string passed in via -javaagent:(jar location)=(agentArgs)
     * @param env						the environment variables
     * @param explicitServiceKey        an explicit service key provided by the caller, this will have higher precedence
     * @return                          ConfigContainer filled with the properties parsed from the -javaagent arguments and configuration property file
     * @throws InvalidConfigException   failed to read the configs
     */
    static ConfigContainer readConfigs(String agentArgs, Map<String, String> env, String explicitServiceKey) throws InvalidConfigException {
        ConfigContainer container = new ConfigContainer();
        
        if (explicitServiceKey != null) {
            container.putByStringValue(ConfigProperty.AGENT_SERVICE_KEY, explicitServiceKey);
        }

        List<InvalidConfigException> exceptions = new ArrayList<InvalidConfigException>();

        try {
            //Firstly, read from ENV
            logger.debug("Start reading configs from ENV");
            new EnvConfigReader(env).read(container);
            logger.debug("Finished reading configs from ENV");
        } catch (InvalidConfigException e) {
            exceptions.add(new InvalidConfigReadSourceException(e.getConfigProperty(), ConfigSourceType.ENV_VAR, null, container, e));
        }

        try {
            //Secondly, read from Java Agent Arguments
            logger.debug("Start reading configs from -javaagent arguments");
            new JavaAgentArgumentConfigReader(agentArgs).read(container);
            logger.debug("Finished reading configs from -javaagent arguments");
        } catch (InvalidConfigException e) {
            exceptions.add(new InvalidConfigReadSourceException(e.getConfigProperty(), ConfigSourceType.JVM_ARG, null, container, e));
        }


        String location = null;
        try {
            //Thirdly, read from Config Property File
            InputStream config;
            if (container.containsProperty(ConfigProperty.AGENT_CONFIG)) {
                location = (String) container.get(ConfigProperty.AGENT_CONFIG);
                try {
                    config = new FileInputStream(location);
                } catch (FileNotFoundException e) {
                    throw new InvalidConfigException(e);
                }
            } else {
                location = ResourceDirectory.getJavaAgentConfigLocation();
                try {
                    config = new FileInputStream(location);
                } catch (FileNotFoundException e) {
                    //try to look up the default file then
                    logger.info("No config file found in " + ResourceDirectory.getJavaAgentConfigLocation() + " . Going to use default values");
                    config = Agent.class.getResourceAsStream("/javaagent.json"); //the file included within the agent jar
                    location = "default";
                }
            }
            configLocation = location;
            logger.debug("Start reading configs from config file: " + configLocation);
            new JsonConfigReader(config).read(container);
            logger.debug("Finished reading configs from config file: " + configLocation);
        } catch (InvalidConfigException e) {
            exceptions.add(new InvalidConfigReadSourceException(e.getConfigProperty(), ConfigSourceType.JSON_FILE, location, container, e));
        }

        if (!exceptions.isEmpty()) {
            //rethrow 1st exceptions encountered
            throw exceptions.get(0);
        }

        //check all the required keys are present
        checkRequiredConfigKeys(container);

        return container;
    }


    /**
     * Puts all the entries from the sourceMap to the targetMap only if the key is absent in the targetMap
     * @param targetMap
     * @param sourceMap
     */
    private static <T, R> void putAllIfAbsent(Map<T, R> targetMap, Map<T, R> sourceMap) {
    	Map<T, R> newKeysMap = new HashMap<T, R>(sourceMap); //to avoid modifying the sourceMap
    	newKeysMap.keySet().removeAll(targetMap.keySet()); //remove all the overlapping keys from sourceMap
    	targetMap.putAll(newKeysMap);
    }

    /**
     * Checks whether all the required config keys by this Agent is present.
     * 
     * @param configs
     * @throws InvalidConfigException
     */
    private static void checkRequiredConfigKeys(ConfigContainer configs) throws InvalidConfigException {
        Set<ConfigProperty> requiredKeys = new HashSet<ConfigProperty>();

        requiredKeys.add(ConfigProperty.AGENT_SERVICE_KEY);
        requiredKeys.add(ConfigProperty.AGENT_LOGGING);
        requiredKeys.add(ConfigProperty.AGENT_JDBC_INST_ALL);

        requiredKeys.add(ConfigProperty.MONITOR_JMX_ENABLE);
        requiredKeys.add(ConfigProperty.MONITOR_JMX_SCOPES);

        Set<ConfigProperty> missingKeys = new HashSet<ConfigProperty>();

        for (ConfigProperty requiredKey : requiredKeys) {
            if (!configs.containsProperty(requiredKey)) {
                missingKeys.add(requiredKey);
            }
        }

        if (!missingKeys.isEmpty()) {
            StringBuffer errorMessage = new StringBuffer("Missing Configs ");
            errorMessage.append(missingKeys.toString());
            throw new InvalidConfigException(errorMessage.toString());
        }
    }

    
    /**
     * Validate and populate the ConfigContainer with defaults if not found from the config loading
     *
     * Then initializes {@link ConfigManager} with the processed values
     *
     * @param configs
     */
    private static void processConfigs(ConfigContainer configs) throws InvalidConfigException {
        if (configs.containsProperty(ConfigProperty.AGENT_DEBUG)) { //legacy flag
            configs.put(ConfigProperty.AGENT_LOGGING, false);
        } 
        
        if (configs.containsProperty(ConfigProperty.AGENT_SAMPLE_RATE)) {
            Integer sampleRateFromConfig = (Integer) configs.get(ConfigProperty.AGENT_SAMPLE_RATE);
            if (sampleRateFromConfig < 0 ||  sampleRateFromConfig > SAMPLE_RESOLUTION) { 
                logger.warn(ConfigProperty.AGENT_SAMPLE_RATE + ": Invalid argument value: " + sampleRateFromConfig + ": must be between 0 and " + SAMPLE_RESOLUTION); 
                throw new InvalidConfigException("Invalid " + ConfigProperty.AGENT_SAMPLE_RATE.getConfigFileKey() + " : " + sampleRateFromConfig);
            }
        }
        
        if (configs.containsProperty(ConfigProperty.AGENT_SERVICE_KEY) && !"".equals(configs.get(ConfigProperty.AGENT_SERVICE_KEY))) {
            // Customer access key (UUID)
            String rawServiceKey = (String) configs.get(ConfigProperty.AGENT_SERVICE_KEY);
            String serviceKey = ServiceKeyUtils.transformServiceKey(rawServiceKey);
            
            if (!serviceKey.equalsIgnoreCase(rawServiceKey)) {
                logger.warn("Invalid service name detected in service key, the service key is transformed to " + ServiceKeyUtils.maskServiceKey(serviceKey));
                configs.put(ConfigProperty.AGENT_SERVICE_KEY, serviceKey, true);
            }
            logger.debug("Service key (masked) is [" + ServiceKeyUtils.maskServiceKey(serviceKey) + "]");

        } else {
        	if (!configs.containsProperty(ConfigProperty.AGENT_SERVICE_KEY)) {
        		logger.warn("Could not find the service key! Please specify " + ConfigProperty.AGENT_SERVICE_KEY.getConfigFileKey() + " in javaagent.json");
        		throw new InvalidConfigServiceKeyException("Service key not found");
        	} else {
        		logger.warn("Service key is empty! Please specify " + ConfigProperty.AGENT_SERVICE_KEY.getConfigFileKey() + " in javaagent.json");
        		throw new InvalidConfigServiceKeyException("Service key is empty");
        	}
        }
        

        if (!configs.containsProperty(ConfigProperty.AGENT_JDBC_INST_ALL)) {
            configs.put(ConfigProperty.AGENT_JDBC_INST_ALL, false);
        }

        TraceConfigs traceConfigs = null;
        if (configs.containsProperty(ConfigProperty.AGENT_TRANSACTION_SETTINGS)) {
            if (configs.containsProperty(ConfigProperty.AGENT_URL_SAMPLE_RATE)) {
                logger.warn(ConfigProperty.AGENT_URL_SAMPLE_RATE.getConfigFileKey() + " is ignored as " + ConfigProperty.AGENT_TRANSACTION_SETTINGS.getConfigFileKey() + " is also defined");
            }
            traceConfigs = (TraceConfigs) configs.get(ConfigProperty.AGENT_TRANSACTION_SETTINGS);
        } else if (configs.containsProperty(ConfigProperty.AGENT_URL_SAMPLE_RATE)) {
            traceConfigs = (TraceConfigs) configs.get(ConfigProperty.AGENT_URL_SAMPLE_RATE);
        }

        if (traceConfigs != null) {
            configs.put(ConfigProperty.AGENT_INTERNAL_TRANSACTION_SETTINGS, traceConfigs);
        }

        if (!configs.containsProperty(ConfigProperty.AGENT_LAYER)) {
            configs.put(ConfigProperty.AGENT_LAYER, AppEnvironment.getAppServerName());
        }
        
        Boolean profilerEnabledFromEnvVar = null;
        if (configs.containsProperty(ConfigProperty.PROFILER_ENABLED_ENV_VAR)) {
            profilerEnabledFromEnvVar = (Boolean) configs.get(ConfigProperty.PROFILER_ENABLED_ENV_VAR);
        }

        Integer profilerIntervalFromEnvVar = null;
        if (configs.containsProperty(ConfigProperty.PROFILER_INTERVAL_ENV_VAR)) {
            profilerIntervalFromEnvVar = (Integer) configs.get(ConfigProperty.PROFILER_INTERVAL_ENV_VAR);
        }

        ProfilerSetting finalProfilerSetting = null;
        if (configs.containsProperty(ConfigProperty.PROFILER)) {
            ProfilerSetting profilerSettingsFromConfigFile = (ProfilerSetting) configs.get(ConfigProperty.PROFILER);
            boolean finalEnabled = profilerEnabledFromEnvVar != null ? profilerEnabledFromEnvVar : profilerSettingsFromConfigFile.isEnabled();
            int finalInterval = profilerIntervalFromEnvVar != null ? profilerIntervalFromEnvVar : profilerSettingsFromConfigFile.getInterval();
            finalProfilerSetting = new ProfilerSetting(finalEnabled, profilerSettingsFromConfigFile.getExcludePackages(), finalInterval, profilerSettingsFromConfigFile.getCircuitBreakerDurationThreshold(), profilerSettingsFromConfigFile.getCircuitBreakerCountThreshold());
        } else if (profilerEnabledFromEnvVar != null || profilerIntervalFromEnvVar != null) {
            finalProfilerSetting = new ProfilerSetting(profilerEnabledFromEnvVar != null ? profilerEnabledFromEnvVar : false, profilerIntervalFromEnvVar != null ? profilerIntervalFromEnvVar : ProfilerSetting.DEFAULT_INTERVAL);
        }

        if (finalProfilerSetting != null) {
            configs.put(ConfigProperty.PROFILER, finalProfilerSetting, true);
        }

        ConfigManager.initialize(configs);
    }
    
    /**
     * Checks for any classes loaded so far that are eligible for "re-transformation". This should be applied after the premain is completed in order to avoid
     * crashing VM as reported in https://bugs.openjdk.java.net/browse/JDK-8074299
     */
    public static void checkRetransformation() {
        if (status == AgentStatus.INITIALIZED_SUCCESSFUL && agentClassFileTransformer != null) {
            agentClassFileTransformer.checkRetransformation();
        }
    }
    
    /**
     * Reports the agent init message without blocking.   
     * 
     * Only the first call to this method will have effect, all other subsequent invocations will be ignored.
     * 
     * This might be called in various places as it's hard to find a single safe place to call this for all java processes.
     * 
     * For example https://github.com/librato/joboe/issues/542 and https://github.com/librato/joboe/pull/692
     */
    public static void reportInit() {
        reportInit(false);
    }
    
    /**
     * Reports the agent init message. 
     * 
     * Only the first call to this method will have effect, all other subsequent invocations will be ignored.
     * 
     * If timeout (default as 10 seconds, configurable) is non-zero, block until either the init message is sent or timeout elapsed. Otherwise submit the message to the client and return without blocking.
     *  
     * @param  blockUntilFinish whether
     */
    private static void reportInit(boolean blockUntilFinish) {
        if (!reportedAgentInit.getAndSet(true)) {
            Future<Result> future;
            try {
                String layerName = (String) ConfigManager.getConfig(ConfigProperty.AGENT_LAYER);
                future = reportLayerInit(layerName, version, initErrorMessage, startTime);
                if (blockUntilFinish) {
                    int timeout = ConfigManager.getConfig(ConfigProperty.AGENT_INIT_TIMEOUT) != null ? (Integer) ConfigManager.getConfig(ConfigProperty.AGENT_INIT_TIMEOUT) : DEFAULT_INIT_MESSAGE_TIMEOUT;
                    if (timeout != 0) {
                        try {
                            future.get(timeout, TimeUnit.SECONDS);
                        } catch (TimeoutException e) {
                            logger.info("Failed to post init message after waiting for " + timeout + " seconds");
                        }
                    }
                }
            } catch (Exception e) {
                logger.warn("Failed to post init message: " + (e.getMessage() != null ? e.getMessage() : e.toString()));
                if (initErrorMessage != null) {
                    logger.warn("Failed to report init error [" + initErrorMessage + "]");
                }
            }

        }
        
    }
    
    
    
    /**
     * Report layer initialization
     * @param layer
     * @param version
     * @param errorMessage
     * @param startTime 
     * @throws ClientException
     * @return a future object of the post status (init message) operation  
     */
    private static Future<Result> reportLayerInit(final String layer, final String version, final String errorMessage, final long startTime) throws ClientException {
        //must call buildInitMessage before initializing RPC client, otherwise it might deadlock as discussed in https://github.com/librato/joboe/pull/767
        Map<String, Object> initMessage = buildInitMessage(layer, version, errorMessage, startTime);
        
        Client rpcClient = RpcClientManager.getClient(OperationType.STATUS);
        return rpcClient.postStatus(Collections.singletonList(initMessage), new ClientLoggingCallback<Result>("post init message"));
    }
    
    static Map<String, Object> buildInitMessage(String layer, String version, String errorMessage, long startTime) {
        Map<String, Object> initMessage = new HashMap<String, Object>();
        
        initMessage.put("Layer", layer);
        initMessage.put("Label", "single");
        initMessage.put("__Init", true);
        
        if (version != null) {
            initMessage.put("Java.AppOptics.Version", version);
        }

        String javaVersion = System.getProperty("java.version");
        if (javaVersion != null) {
            initMessage.put("Java.Version", javaVersion);
        }

        if (errorMessage != null) {
            initMessage.put("Error", errorMessage);
        }
        
        File agentJarPath = ResourceDirectory.getAgentJarPath();
        if (agentJarPath != null) {
            initMessage.put("Java.InstallDirectory", agentJarPath.getAbsolutePath());
            initMessage.put("Java.InstallTimestamp", agentJarPath.lastModified() / 1000); //make it second since epoch
        }
        initMessage.put("Java.LastRestart", startTime);
        
        return initMessage;
    }
    

    public enum AgentStatus {
        INITIALIZED_SUCCESSFUL,
        INITIALIZED_FAILED,
        UNINITIALIZED,
        DISABLED
    }
}
