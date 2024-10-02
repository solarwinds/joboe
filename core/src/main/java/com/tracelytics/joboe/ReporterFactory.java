package com.tracelytics.joboe;

import java.io.IOException;
import java.util.Map;

import com.tracelytics.joboe.config.InvalidConfigException;
import com.tracelytics.joboe.rpc.Client;
import com.tracelytics.logging.Logger;
import com.tracelytics.logging.LoggerFactory;

/**
 * Provide methods to create {@link EventReporter}
 * @author pluk
 *
 */
public class ReporterFactory {
    private static Logger logger = LoggerFactory.getLogger();
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
    
    private static ReporterFactory singleton = null;
    
    /**
     * Returns a singleton of the ReporterFactory
     * @return
     * @throws InvalidConfigException
     */
    public static synchronized ReporterFactory getInstance() throws InvalidConfigException {
        if (singleton == null) {
            ReporterFactory factory = new ReporterFactory();
            try {
                factory.init(); 
                singleton = factory; //init ok, now assign it to the singleton
            } catch (RuntimeException e) {
                throw new InvalidConfigException("Failed to initialize " + ReporterFactory.class.getName() + "[" + e.getMessage() + "]", e);
            }
        }
        
        return singleton;
    }
    
    private ReporterFactory() {} //to prevent direct invocation of the ctor
    
    private void init() {
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
     * @return
     * @throws IOException
     */
    public UDPReporter buildUdpReporter() throws IOException {
        return buildUdpReporter(tracelyzerHost, tracelyzerPort);
    }
    
    /**
     * Builds a {@link UDPReporter}. Take note that this might create a singleton if the system has restrictions on UDP bind address/port, in such an
     * environment, the singleton will have the host/port set to the first call to this method, any other calls following with different host and port would NOT reset the host/port
     * 
     * @param host  Destination host
     * @param port  Destination port
     * @return
     * @throws IOException
     */
    UDPReporter buildUdpReporter(String host, Integer port) throws IOException {
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
    
    public RpcEventReporter buildRpcReporter(Client rpcClient) throws IOException {
        return new RpcEventReporter(rpcClient);
    }
    
    /**
     * Builds a {@link TestReporter}, take note that this reporter collects events from all threads
     * @return
     */
    public TestReporter buildTestReporter() {
        return buildTestReporter(false);
    }
    
    /**
     * Builds a {@link TestReporter}, which works in thread local manner according to the parameter isThreadLocal
     * @param isThreadLocal whether the <code>TestReporter</code> built should work in thread local manner
     * @return
     */
    public TestReporter buildTestReporter(boolean isThreadLocal) {
        return isThreadLocal ? new TestReporter() : new NonThreadLocalTestReporter();
    }
}
