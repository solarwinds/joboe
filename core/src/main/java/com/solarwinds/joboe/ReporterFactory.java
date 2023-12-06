package com.solarwinds.joboe;

import java.io.IOException;
import java.util.Map;

import com.solarwinds.joboe.rpc.Client;
import com.solarwinds.joboe.rpc.ClientLoggingCallback;
import com.solarwinds.joboe.rpc.HostType;
import com.solarwinds.lambda.LambdaEventReporter;
import com.solarwinds.logging.Logger;
import com.solarwinds.logging.LoggerFactory;
import lombok.Getter;

/**
 * Provide methods to create {@link EventReporter}
 *
 * @author pluk
 */

public class ReporterFactory {
    private static final Logger logger = LoggerFactory.getLogger();
    private String tracelyzerHost = Constants.XTR_UDP_HOST;
    private int tracelyzerPort = Constants.XTR_UDP_PORT;
    private String datagramLocalAddress;
    private Integer datagramLocalPort;

    private static final String OPENSHIFT_TRACEVIEW_TLYZER_IP = "OPENSHIFT_TRACEVIEW_TLYZER_IP";
    private static final String OPENSHIFT_TRACEVIEW_TLYZER_PORT = "OPENSHIFT_TRACEVIEW_TLYZER_PORT";
    private static final String OPENSHIFT_TRACEVIEW_JAVA_DATAGRAM_IP = "OPENSHIFT_TRACEVIEW_JAVA_DATAGRAM_IP";
    private static final String OPENSHIFT_TRACEVIEW_JAVA_DATAGRAM_PORT = "OPENSHIFT_TRACEVIEW_JAVA_DATAGRAM_PORT";

    private static final String TRACELYTICS_UDPADDR = "TRACELYTICS_UDPADDR";
    private static final String TRACELYTICS_UDPPORT = "TRACELYTICS_UDPPORT";

    private static UDPReporter singletonUdpReporter;
    private static final Object singletonUdpReporterLock = new Object();

    @Getter(lazy = true)
    private static final ReporterFactory instance = new ReporterFactory();

    private ReporterFactory() {
        Map<String, String> env = System.getenv();

        if (env.containsKey(TRACELYTICS_UDPADDR)) {
            tracelyzerHost = env.get(TRACELYTICS_UDPADDR);
            logger.info("Setting Reporter to contact Tracelyzer host on [" + tracelyzerHost + "]");
        }
        if (env.containsKey(TRACELYTICS_UDPPORT)) {
            tracelyzerPort = Integer.parseInt(env.get(TRACELYTICS_UDPPORT));
            logger.info("Setting Reporter to contact Tracelyzer port on [" + tracelyzerPort + "]");
        }

        //open shift check
        if (env.containsKey(OPENSHIFT_TRACEVIEW_TLYZER_IP)) {
            tracelyzerHost = env.get(OPENSHIFT_TRACEVIEW_TLYZER_IP);
            logger.info("Running in OpenShift environment. Setting Reporter to contact Tracelyzer host on [" + tracelyzerHost + "]");
        }
        if (env.containsKey(OPENSHIFT_TRACEVIEW_TLYZER_PORT)) {
            tracelyzerPort = Integer.parseInt(env.get(OPENSHIFT_TRACEVIEW_TLYZER_PORT));
            logger.info("Running in OpenShift environment. Setting Reporter to contact Tracelyzer port on [" + tracelyzerPort + "]");
        }
        if (env.containsKey(OPENSHIFT_TRACEVIEW_JAVA_DATAGRAM_IP)) {
            datagramLocalAddress = env.get(OPENSHIFT_TRACEVIEW_JAVA_DATAGRAM_IP);
            logger.info("Running in OpenShift environment. Setting Reporter datagram port to [" + datagramLocalAddress + "]");
        }
        if (env.containsKey(OPENSHIFT_TRACEVIEW_JAVA_DATAGRAM_PORT)) {
            datagramLocalPort = Integer.parseInt(env.get(OPENSHIFT_TRACEVIEW_JAVA_DATAGRAM_PORT));
            logger.info("Running in OpenShift environment. Setting Reporter datagram port to [" + datagramLocalPort + "]");
        }

    }

    /**
     * Builds a {@link UDPReporter}. Take note that this might create a singleton if the system has restrictions on UDP bind address/port
     *
     * @return
     * @throws IOException
     */
    public UDPReporter createUdpReporter() throws IOException {
        return createUdpReporter(tracelyzerHost, tracelyzerPort);
    }

    /**
     * Builds a {@link UDPReporter}. Take note that this might create a singleton if the system has restrictions on UDP bind address/port, in such an
     * environment, the singleton will have the host/port set to the first call to this method, any other calls following with different host and port would NOT reset the host/port
     *
     * @param host Destination host
     * @param port Destination port
     * @return
     * @throws IOException
     */
    UDPReporter createUdpReporter(String host, Integer port) throws IOException {
        if (host == null || port == null) {
            logger.error("Cannot build UDPReporter. Host and/or port params are null!");
            return null;
        }

        logger.debug("Building UPD Reporter with host [" + host + "] and port [" + port + "]");

        if (datagramLocalAddress != null && datagramLocalPort != null) {
            //if using specific address and port, then we should only allow singleton; otherwise multiple UDP reporters will try to bind to the same address/port
            synchronized (singletonUdpReporterLock) {
                if (singletonUdpReporter == null) {
                    logger.debug("UPD Reporter specified datagram to bind on [" + datagramLocalAddress + ":" + datagramLocalPort + "]");
                    singletonUdpReporter = new UDPReporter(host, port, datagramLocalAddress, datagramLocalPort);
                }
            }

            return singletonUdpReporter;
        } else {
            return new UDPReporter(host, port);
        }
    }

    public RpcEventReporter createRpcReporter(Client rpcClient) throws IOException {
        return new RpcEventReporter(rpcClient);
    }

    /**
     * Builds a {@link TestReporter}, take note that this reporter collects events from all threads
     *
     * @return
     */
    public TestReporter createTestReporter() {
        return createTestReporter(false);
    }

    /**
     * Builds a {@link TestReporter}, which works in thread local manner according to the parameter isThreadLocal
     *
     * @param isThreadLocal whether the <code>TestReporter</code> built should work in thread local manner
     * @return
     */
    public TestReporter createTestReporter(boolean isThreadLocal) {
        return isThreadLocal ? new TestReporter() : new NonThreadLocalTestReporter();
    }

    public LambdaEventReporter createLambdaReporter(Client client) {
        return new LambdaEventReporter(client, new ClientLoggingCallback<>("sent lambda event"), new AtomicEventReporterStats(() -> 0));
    }

    public QueuingEventReporter createQueuingEventReporter(Client client) {
        return new QueuingEventReporter(client);
    }

    public EventReporter createHostTypeReporter(Client client, HostType hostType) {
        if (hostType == HostType.AWS_LAMBDA) {
            return createLambdaReporter(client);
        } else if (hostType == HostType.PERSISTENT) {
            return createQueuingEventReporter(client);
        }
        throw new RuntimeException(String.format("Unsupported host type: %s", hostType));
    }
}
