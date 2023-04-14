package com.tracelytics.joboe.config;

import com.tracelytics.logging.Logger;
import com.tracelytics.logging.LoggerFactory;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Lists and describes the properties used in configurations with its value type (Java Class Type).
 * <p>
 * It also defines the corresponding keys of the properties when used as Agent Arguments (via -javaagent) or in configuration file.
 * <p>
 * Lookups are provided in order to retrieve the <code>ConfigProperty</code> from Agent Argument and from Key (property file key)
 *
 * @author Patson Luk
 *
 */
public enum ConfigProperty {
    AGENT_CONFIG (new ConfigKey(null, EnvPrefix.PRODUCT + "CONFIG_FILE", "config"), ConfigGroup.AGENT, String.class),
    AGENT_DEBUG (new ConfigKey(null, null, "debug"), ConfigGroup.AGENT, Boolean.class),
    AGENT_LOGGING (new ConfigKey("agent.logging", EnvPrefix.PRODUCT + "DEBUG_LEVEL", "logging"), ConfigGroup.AGENT, String.class),
    AGENT_LOGGING_TRACE_ID (new ConfigKey("agent.logging.traceId"), ConfigGroup.AGENT, String.class),
    AGENT_TRACING_MODE (new ConfigKey("agent.tracingMode", null, "tracing_mode"), ConfigGroup.AGENT, String.class),
    AGENT_SAMPLE_RATE (new ConfigKey("agent.sampleRate", null, "sampling_rate", "sample_rate"), ConfigGroup.AGENT, Integer.class),
    AGENT_SERVICE_KEY (new ConfigKey("agent.serviceKey", EnvPrefix.PRODUCT + "SERVICE_KEY", "service_key"), ConfigGroup.AGENT, String.class),
    AGENT_LAYER (new ConfigKey("agent.layer", null, "layer"), ConfigGroup.AGENT, String.class),
    AGENT_JDBC_INST_ALL (new ConfigKey("agent.jdbcInstAll", null, "jdbc_inst_all"), ConfigGroup.AGENT, Boolean.class),
    AGENT_SQL_SANITIZE (new ConfigKey("agent.sqlSanitize", EnvPrefix.PRODUCT + "SQL_SANITIZE", "sql_sanitize") , ConfigGroup.AGENT, Integer.class),
    AGENT_SQL_QUERY_MAX_LENGTH(new ConfigKey("agent.sqlQueryMaxLength", EnvPrefix.PRODUCT + "MAX_SQL_QUERY_LENGTH"), ConfigGroup.AGENT, Integer.class),
    AGENT_MONGO_SANITIZE (new ConfigKey("agent.mongoSanitize", EnvPrefix.PRODUCT + "MONGO_SANITIZE", "mongo_sanitize") , ConfigGroup.AGENT, Boolean.class),
    AGENT_KAFKA_PROPAGATION(new ConfigKey("agent.kafkaPropagation", EnvPrefix.PRODUCT + "KAFKA_PROPAGATION", "kafka_propagation"), ConfigGroup.AGENT, Boolean.class),
    AGENT_EXCLUDE_CLASSES (new ConfigKey("agent.excludeClasses"), ConfigGroup.AGENT, String.class),
    AGENT_EXCLUDE_MODULES (new ConfigKey("agent.excludeModules"), ConfigGroup.AGENT, String.class),
    AGENT_URL_SAMPLE_RATE (new ConfigKey("agent.urlSampleRates"), ConfigGroup.AGENT, String.class),
    AGENT_BACKTRACE_MODULES (new ConfigKey("agent.backtraceModules"), ConfigGroup.AGENT, String[].class),
    AGENT_EXTENDED_BACK_TRACES (new ConfigKey("agent.extendedBackTraces"), ConfigGroup.AGENT, Boolean.class),
    AGENT_EXTENDED_BACK_TRACES_BY_MODULE (new ConfigKey("agent.extendedBackTracesByModule"), ConfigGroup.AGENT, String[].class),
    AGENT_HBASE_SCANNER_NEXT (new ConfigKey("agent.hbaseScannerNext"), ConfigGroup.AGENT, Boolean.class),
    AGENT_HIDE_PARAMS (new ConfigKey("agent.hideParams"), ConfigGroup.AGENT, String.class),
    AGENT_AKKA_ACTORS (new ConfigKey("agent.akkaActors"), ConfigGroup.AGENT, String[].class),
    AGENT_TIME_ADJUST_INTERVAL (new ConfigKey("agent.timeAdjustInterval"), ConfigGroup.AGENT, Integer.class),
    AGENT_CONTEXT_TTL(new ConfigKey("agent.contextTtl"), ConfigGroup.AGENT, Integer.class),
    AGENT_CONTEXT_MAX_EVENTS(new ConfigKey("agent.contextMaxEvents"), ConfigGroup.AGENT, Integer.class),
    AGENT_CONTEXT_MAX_BACKTRACES(new ConfigKey("agent.contextMaxBacktraces"), ConfigGroup.AGENT, Integer.class),
    AGENT_HOSTNAME_ALIAS (new ConfigKey("agent.hostnameAlias", EnvPrefix.PRODUCT + "HOSTNAME_ALIAS", "hostname_alias"), ConfigGroup.AGENT, String.class),
    AGENT_LOG_FILE(new ConfigKey(null, EnvPrefix.PRODUCT + "JAVA_LOG_FILE", "log_file"), ConfigGroup.AGENT, String.class),
    AGENT_COLLECTOR(new ConfigKey(null, EnvPrefix.PRODUCT + "COLLECTOR"), ConfigGroup.AGENT, String.class),
    AGENT_COLLECTOR_SERVER_CERT_LOCATION(new ConfigKey(null, EnvPrefix.PRODUCT + "TRUSTEDPATH"), ConfigGroup.AGENT, String.class),
    AGENT_EVENTS_FLUSH_INTERVAL(new ConfigKey(null, EnvPrefix.PRODUCT + "EVENTS_FLUSH_INTERVAL"), ConfigGroup.AGENT, Integer.class),
    AGENT_TRANSACTION_NAME_PATTERN (new ConfigKey("transaction.namePattern"), ConfigGroup.AGENT, String.class),
    AGENT_DOMAIN_PREFIXED_TRANSACTION_NAME(new ConfigKey("transaction.prependDomain"), ConfigGroup.AGENT, Boolean.class),
    AGENT_SYSMON_EARLY_START(new ConfigKey("agent.sysMonEarlyStart", EnvPrefix.PRODUCT + "SYSMON_EARLY_START"), ConfigGroup.AGENT, Boolean.class),
    AGENT_INIT_TIMEOUT(new ConfigKey("agent.initTimeout", EnvPrefix.PRODUCT + "INIT_TIMEOUT"), ConfigGroup.AGENT, Integer.class),
    AGENT_TRANSACTION_SETTINGS(new ConfigKey("agent.transactionSettings"), ConfigGroup.AGENT, String.class),
    //AGENT_INTERNAL_TRANSACTION_SETTINGS should NOT be specified directly in the json file. This is used to store the result after comparing AGENT_TRANSACTION_SETTINGS and AGENT_URL_SAMPLE_RATE - not an ideal solution as this is confusing
    AGENT_INTERNAL_TRANSACTION_SETTINGS(new ConfigKey("agent.internal.transactionSettings"), ConfigGroup.AGENT, String.class),
    AGENT_EC2_METADATA_TIMEOUT(new ConfigKey("agent.ec2MetadataTimeout", EnvPrefix.PRODUCT + "EC2_METADATA_TIMEOUT"), ConfigGroup.AGENT, Integer.class),
    AGENT_AZURE_VM_METADATA_TIMEOUT(new ConfigKey("agent.azureVmMetadataTimeout", EnvPrefix.PRODUCT +
            "AZURE_VM_METADATA_TIMEOUT"), ConfigGroup.AGENT, Integer.class),
    AGENT_AZURE_VM_METADATA_VERSION(new ConfigKey("agent.azureVmMetadataVersion", EnvPrefix.PRODUCT +
            "AZURE_VM_METADATA_VERSION"), ConfigGroup.AGENT, String.class),
    AGENT_TRIGGER_TRACE_ENABLED(new ConfigKey("agent.triggerTrace", EnvPrefix.PRODUCT + "TRIGGER_TRACE"), ConfigGroup.AGENT, String.class),
    AGENT_PROXY(new ConfigKey("agent.proxy",EnvPrefix.PRODUCT + "PROXY", "proxy"), ConfigGroup.AGENT, String.class),
    AGENT_RPC_CLIENT_TYPE(new ConfigKey("agent.rpcType", EnvPrefix.PRODUCT + "RPC_TYPE", "rpc_type"), ConfigGroup.AGENT, String.class), //not advertised
    AGENT_GRPC_COMPRESSION(new ConfigKey(null, EnvPrefix.PRODUCT + "GRPC_COMPRESSION", "grpc_compression"), ConfigGroup.AGENT, String.class), //not advertised
    AGENT_DISALLOW_UNSAFE(new ConfigKey("agent.disallowUnsafe"), ConfigGroup.AGENT, Boolean.class),
    AGENT_SQL_TAG(new ConfigKey("agent.sqlTag", EnvPrefix.PRODUCT + "SQL_TAG", "sql_tag"), ConfigGroup.AGENT, Boolean.class),
    MONITOR_JMX_SCOPES (new ConfigKey("monitor.jmx.scopes"), ConfigGroup.MONITOR, String.class),
    MONITOR_JMX_ENABLE (new ConfigKey("monitor.jmx.enable"), ConfigGroup.MONITOR, Boolean.class),
    MONITOR_JMX_MAX_ENTRY (new ConfigKey("monitor.jmx.maxEntry"), ConfigGroup.MONITOR, Integer.class),
    MONITOR_METRICS_FLUSH_INTERVAL(new ConfigKey(null, EnvPrefix.PRODUCT + "METRICS_FLUSH_INTERVAL"), ConfigGroup.MONITOR, Integer.class),

    MONITOR_SPAN_METRICS_ENABLE (new ConfigKey("monitor.spanMetrics.enable", EnvPrefix.PRODUCT + "SPAN_METRICS_ENABLE"), ConfigGroup.MONITOR, Boolean.class),

    PROFILER(new ConfigKey("profiler"), ConfigGroup.PROFILER, String.class),
    PROFILER_ENABLED_ENV_VAR(new ConfigKey(null, EnvPrefix.PRODUCT + "PROFILER_ENABLED", "profiler"), ConfigGroup.PROFILER, Boolean.class),
    PROFILER_INTERVAL_ENV_VAR(new ConfigKey(null, EnvPrefix.PRODUCT + "PROFILER_INTERVAL"), ConfigGroup.PROFILER, Integer.class);

    private static final Logger logger = LoggerFactory.getLogger();
    private final ConfigKey configKey;
    private Class<? extends Serializable> typeClass;
    private ConfigGroup group;
    private ConfigParser<?, ?> configParser;

    public static class EnvPrefix {
        public static final String PRODUCT = "SW_APM_";
    }

    private static class ConfigKey {
        private final String configFileKey;
        private final String environmentVariableKey;
        private final String[] agentArgumentKeys;

        public ConfigKey(String configFileKey) {
            this(configFileKey, null);
        }

        public ConfigKey(String configFileKey, String environmentVariableKey, String...agentArgumentKeys) {
            super();
            this.configFileKey = configFileKey;
            this.environmentVariableKey = environmentVariableKey;
            this.agentArgumentKeys = agentArgumentKeys;
        }
    }

    /**
     *
     * @param configKey             keys used to map to this property
     * @param group
     * @param typeClass             the Java Class of the property value
     */
    private ConfigProperty(ConfigKey configKey, ConfigGroup group, Class<? extends Serializable> typeClass) {
        this.configKey = configKey;
        this.typeClass = typeClass;
        this.group = group;
        registerLookup(this);
    }

    public void setParser(ConfigParser<?, ?> configParser) {
        this.configParser = configParser;
    }

    private static void registerLookup(ConfigProperty property) {
        String configFileKey = property.configKey.configFileKey;
        if (configFileKey != null) {
            if (ConfigPropertyRegistry.CONFIG_FILE_KEY_TO_PARAMETER.put(configFileKey, property) != null) { //put the key into the lookup map
                logger.warn("Config File Key [" + configFileKey + "]  has been defined more than once! Check " + ConfigProperty.class.getName() + " keys usage!");
            }
        }

        String environmentVariableKey = property.configKey.environmentVariableKey;
        if (environmentVariableKey != null) {
            if (ConfigPropertyRegistry.ENVIRONMENT_VARIABLE_KEY_TO_PARAMETER.put(environmentVariableKey, property) != null) { //put the key into the lookup map
                logger.warn("Environment Variable Key [" + environmentVariableKey + "]  has been defined more than once! Check " + ConfigProperty.class.getName() + " keys usage!");
            }
        }

        String[] agentArgumentKeys = property.configKey.agentArgumentKeys;
        for (String agentArgumentKey : agentArgumentKeys) { //put the argument name into the lookup map
            if (ConfigPropertyRegistry.AGENT_ARGUMENT_KEY_TO_PARAMETER.put(agentArgumentKey, property) != null) {
                logger.warn("Agent Argument Key [" + agentArgumentKey + "]  has been defined more than once!");
            }
        }
    }

    /**
     *
     * @param key   the key used in configuration property file
     * @return      the corresponding ConfigProperty by the key. Null if the property is not defined under that key
     */
    static ConfigProperty fromConfigFileKey(String key) {
        return ConfigPropertyRegistry.CONFIG_FILE_KEY_TO_PARAMETER.get(key);
    }

    public String getConfigFileKey() {
        return configKey.configFileKey;
    }

    public String getEnviromentVariableKey() {
        return configKey.environmentVariableKey;
    }

    public String[] getAgentArgumentKeys() {
        return configKey.agentArgumentKeys;
    }

    /**
     *
     * @param argumentKey  the argument name used in -javaagent
     * @return              the corresponding ConfigProperty by the agent argument key used. Null if the property is not defined under that argument key
     */
    public static ConfigProperty fromAgentArgumentKey(String argumentKey) {
        return ConfigPropertyRegistry.AGENT_ARGUMENT_KEY_TO_PARAMETER.get(argumentKey);
    }

    /**
     *
     * @param environmentVariableKey  the environment variable key used
     * @return              the corresponding ConfigProperty by the  environment variable key used. Null if the property is not defined under that  environment variable key
     */
    public static ConfigProperty fromEnvironmentVariableKey(String environmentVariableKey) {
        return ConfigPropertyRegistry.ENVIRONMENT_VARIABLE_KEY_TO_PARAMETER.get(environmentVariableKey);
    }

    public static Map<String, ConfigProperty> getEnviromentVariableMap() {
        return ConfigPropertyRegistry.ENVIRONMENT_VARIABLE_KEY_TO_PARAMETER;
    }

    /**
     *
     * @return the Java Class of the property value
     */
    public Class<? extends Serializable> getTypeClass() {
        return typeClass;
    }

    /**
     *
     * @return the Grouping of this ConfigProperty
     */
    ConfigGroup getGroup() {
        return group;
    }

    public ConfigParser<?, ?> getConfigParser() {
        return configParser;
    }

    static class ConfigPropertyRegistry {
        private static final Map<String, ConfigProperty> CONFIG_FILE_KEY_TO_PARAMETER = new HashMap<String, ConfigProperty>();
        private static final Map<String, ConfigProperty> ENVIRONMENT_VARIABLE_KEY_TO_PARAMETER = new HashMap<String, ConfigProperty>();
        private static final Map<String, ConfigProperty> AGENT_ARGUMENT_KEY_TO_PARAMETER = new HashMap<String, ConfigProperty>();
    }
}


