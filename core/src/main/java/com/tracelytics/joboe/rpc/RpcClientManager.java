package com.tracelytics.joboe.rpc;

import java.io.File;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.Map;

import com.tracelytics.ext.javassist.bytecode.ClassFile;
import com.tracelytics.joboe.config.ConfigManager;
import com.tracelytics.joboe.config.ConfigProperty;
import com.tracelytics.joboe.rpc.Client.ClientType;
import com.tracelytics.joboe.rpc.grpc.GrpcClientManager;
import com.tracelytics.joboe.rpc.thrift.ThriftClientManager;
import com.tracelytics.logging.Logger;
import com.tracelytics.logging.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

/**
 * Manages creation of PRC {@link Client}
 * @author pluk
 *
 */
public abstract class RpcClientManager {
    private static final Logger logger = LoggerFactory.getLogger();
    private static final Map<ClientType, RpcClientManager> registeredManagers = new HashMap<ClientType, RpcClientManager>();
    static final URL DEFAULT_COLLECTER_CERT_LOCATION = RpcClientManager.class.getResource("/collector-ca.crt"); //cert by default included in the resource folder or root folder in jar

    static final String DEFAULT_HOST = "apm-collector.dc-01.cloud.solarwinds.com"; //default collector host: NH production
    static final int DEFAULT_PORT = 443; //default collector port

    protected static String collectorHost;
    protected static int collectorPort;
    protected static URL collectorCertLocation;
    private static ClientType configuredClientType;
    private static Boolean alpnAvailable = null;

    static {
        init((String) ConfigManager.getConfig(ConfigProperty.AGENT_COLLECTOR), (String) ConfigManager.getConfig(ConfigProperty.AGENT_COLLECTOR_SERVER_CERT_LOCATION), (String) ConfigManager.getConfig(ConfigProperty.AGENT_RPC_CLIENT_TYPE));
        registeredManagers.put(ClientType.THRIFT, new ThriftClientManager());
        registeredManagers.put(ClientType.GRPC, new GrpcClientManager());
    }

    //for existing unit test
    static void init(String collector, String collectorCertValue) {
        init(collector, collectorCertValue, null);
    }

    static void init(String collector, String collectorCertValue, String rpcType) {
        if (collector != null) { //use system env if defined
            setHostAndPortByVariable(collector);
        } else {
            collectorHost = DEFAULT_HOST;
            collectorPort = DEFAULT_PORT;
        }

        if (collectorCertValue != null) {
            logger.info("Setting RPC Client to use server cert at [" + collectorCertValue + "]");
            try {
                File collectorCert = new File(collectorCertValue);
                if (collectorCert.exists()) {
                    collectorCertLocation = collectorCert.toURI().toURL();
                }else {
                    logger.warn("Failed to load RPC collector server certificate from location [" + collectorCertValue + "], file does not exist!");
                    collectorCertLocation = DEFAULT_COLLECTER_CERT_LOCATION;
                }


            } catch (MalformedURLException e) {
                logger.warn("Failed to load RPC collector server certificate from location [" + collectorCertValue + "], using default location instead!");
                collectorCertLocation = DEFAULT_COLLECTER_CERT_LOCATION;
            }
        } else {
            collectorCertLocation = DEFAULT_COLLECTER_CERT_LOCATION;
        }

        if (rpcType != null) {
            try {
                configuredClientType = ClientType.valueOf(rpcType.toUpperCase());
                logger.info("Using RPC client type " + configuredClientType.name());
            } catch (IllegalArgumentException e) {
                logger.warn("rpc type [" + rpcType + "] does not map to any known RPC client type");
            }
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
                logger.warn("Failed to parse the port number from " + ConfigProperty.AGENT_COLLECTOR.getEnviromentVariableKey() + " : [" + variable + "]");
                collectorPort = DEFAULT_PORT;
            }
        }

        logger.info("Using RPC collector host [" + collectorHost + "] port [" + collectorPort + "] for RPC client creation");
    }

    public static Client getClient(OperationType operationType) throws ClientException {
        return getClient(operationType, (String) ConfigManager.getConfig(ConfigProperty.AGENT_SERVICE_KEY));
    }

    public static Client getClient(OperationType operationType, String serviceKey) throws ClientException {
        ClientType clientType;
        if (configuredClientType == null) { //By default, determine the clientType by java version
            if (ClassFile.MAJOR_VERSION >= ClassFile.JAVA_9) {
                clientType = ClientType.GRPC;
            } else if (ClassFile.MAJOR_VERSION == ClassFile.JAVA_8) {
                clientType = alpnAvailable() ? ClientType.GRPC : ClientType.THRIFT;
            } else {
                clientType = ClientType.THRIFT;
            }
        } else {
            clientType = configuredClientType;
        }

        return getClient(clientType, operationType, serviceKey);
    }

    /**
     * https://github.com/grpc/grpc-java/blob/v1.34.1/netty/src/main/java/io/grpc/netty/JettyTlsUtil.java#L39
     * @return
     */
    private static boolean alpnAvailable() {
        if (alpnAvailable != null) {
            return alpnAvailable;
        }
        try {
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, null, null);
            SSLEngine engine = context.createSSLEngine();
            Method getApplicationProtocol =
                    AccessController.doPrivileged(new PrivilegedExceptionAction<Method>() {
                        @Override
                        public Method run() throws Exception {
                            return SSLEngine.class.getMethod("getApplicationProtocol");
                        }
                    });
            getApplicationProtocol.invoke(engine);
            alpnAvailable = true;
        } catch (Throwable t) {
            alpnAvailable = false;
        }
        return alpnAvailable;
    }



    /**
     * Obtains a client by the <code>clientType</code> and <code>operationType</code>
     * @param clientType    The protocol/type of the Client. For example THRIFT
     * @param operationType The operation type this client will handle
     * @return
     * @throws ClientException
     */
    static Client getClient(ClientType clientType, OperationType operationType) throws ClientException {
       return getClient(clientType, operationType, (String) ConfigManager.getConfig(ConfigProperty.AGENT_SERVICE_KEY));
    }


    static Client getClient(ClientType clientType, OperationType operationType, String serviceKey) throws ClientException {
        logger.debug("Using " + clientType + " for rpc calls");

        RpcClientManager manager = registeredManagers.get(clientType);
        if (manager != null) {
            Client client = manager.getClientImpl(operationType, serviceKey);
            if (client == null) {
                throw new ClientException("Failed to create client for " + clientType);
            }
            return client;
        } else {
            throw new ClientException("Unknown Client type requested : " + clientType);
        }
    }



    protected abstract Client getClientImpl(OperationType operationType, String serviceKey);


    public enum OperationType { TRACING, PROFILING, SETTINGS, METRICS, STATUS }
}
