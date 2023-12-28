package com.solarwinds.joboe.core.rpc;

import com.solarwinds.joboe.core.config.ConfigManager;
import com.solarwinds.joboe.core.config.ConfigProperty;
import com.solarwinds.joboe.core.rpc.Client.ClientType;
import com.solarwinds.joboe.core.logging.Logger;
import com.solarwinds.joboe.core.logging.LoggerFactory;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Manages creation of PRC {@link Client}
 * @author pluk
 *
 */
public abstract class RpcClientManager {
    private static final Logger logger = LoggerFactory.getLogger();
    static final URL AO_DEFAULT_COLLECTER_CERT_LOCATION = getCertURL();

    private static URL getCertURL() {
        //cert by default included in the resource folder or root folder in jar
        URL url = RpcClientManager.class.getResource("/ao-collector.crt");
        if (url == null) {
            ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
            if (contextClassLoader != null) {
                url = contextClassLoader.getResource("ao-collector.crt");
            }
        }
        return url;
    }

    static final String DEFAULT_HOST = "apm.collector.na-01.cloud.solarwinds.com"; //default collector host: NH production
    static final int DEFAULT_PORT = 443; //default collector port

    protected static String collectorHost;
    protected static int collectorPort;
    protected static URL collectorCertLocation;

    static {
        init((String) ConfigManager.getConfig(ConfigProperty.AGENT_COLLECTOR), (String) ConfigManager.getConfig(ConfigProperty.AGENT_COLLECTOR_SERVER_CERT_LOCATION));
    }

    static void init(String collector, String collectorCertValue) {
        if (collector != null) { //use system env if defined
            setHostAndPortByVariable(collector);
        } else {
            collectorHost = DEFAULT_HOST;
            collectorPort = DEFAULT_PORT;
        }

        URL defaultCertLocation = collectorHost.contains("appoptics.com") ? AO_DEFAULT_COLLECTER_CERT_LOCATION : null;
        if (collectorCertValue != null) {
            logger.info("Setting RPC Client to use server cert at [" + collectorCertValue + "]");
            try {
                File collectorCert = new File(collectorCertValue);
                if (collectorCert.exists()) {
                    collectorCertLocation = collectorCert.toURI().toURL();
                }else {
                    logger.warn("Failed to load RPC collector server certificate from location [" + collectorCertValue + "], file does not exist!");
                    collectorCertLocation = defaultCertLocation;
                }


            } catch (MalformedURLException e) {
                logger.warn("Failed to load RPC collector server certificate from location [" + collectorCertValue + "], using default location instead!");
                collectorCertLocation = defaultCertLocation;
            }
        } else {
            logger.info(String.format("Setting RPC Client to use bundled Certificate: %s", defaultCertLocation));
            collectorCertLocation = defaultCertLocation;
        }
    }

    private static void setHostAndPortByVariable(String variable) {
        String[] tokens = variable.split(":");
        if (tokens.length == 1) {
            collectorHost = tokens[0];
            collectorPort = DEFAULT_PORT;
        } else {
            collectorHost = tokens[0];
            try {
                collectorPort = Integer.parseInt(tokens[1]);
            } catch (NumberFormatException e) {
                logger.warn("Failed to parse the port number from " + ConfigProperty.AGENT_COLLECTOR.getEnvironmentVariableKey() + " : [" + variable + "]");
                collectorPort = DEFAULT_PORT;
            }
        }

        logger.info("Using RPC collector host [" + collectorHost + "] port [" + collectorPort + "] for RPC client creation");
    }

    public static Client getClient(OperationType operationType) throws ClientException {
        return getClient(operationType, (String) ConfigManager.getConfig(ConfigProperty.AGENT_SERVICE_KEY));
    }

    public static Client getClient(OperationType operationType, String serviceKey) throws ClientException {
        return ClientManagerProvider.getClientManager(ClientType.GRPC)
                .orElseThrow(() -> new ClientException("Unknown Client type requested"))
                .getClientImpl(operationType, serviceKey);
    }

    abstract protected void close();

    protected abstract Client getClientImpl(OperationType operationType, String serviceKey);

    public enum OperationType { TRACING, PROFILING, SETTINGS, METRICS, STATUS }
}
