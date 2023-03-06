package com.tracelytics.util;

import com.tracelytics.joboe.HostId;
import com.tracelytics.logging.Logger;
import com.tracelytics.logging.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * Helper to extract information on the host this JVM runs on
 * @author pluk
 *
 */
public class HostInfoUtils {
    private static final Logger logger = LoggerFactory.getLogger();
    private static HostInfoReader reader;

    static {
        for (HostInfoReaderProvider provider : ServiceLoader.load(HostInfoReaderProvider.class)) {
            logger.debug("Use HostInfoReaderProvider implementation " + provider.getClass().getName());
            HostInfoUtils.reader = provider.getHostInfoReader();
            break; // use the first implementation found in the path
        }
        if (HostInfoUtils.reader == null) {
            reader = new DummyHostInfoReader();
        }
    }

    public static String getAzureInstanceId() {
        return reader.getAzureInstanceId();
    }

    public enum OsType {
        LINUX("Linux"), WINDOWS("Windows"), MAC("Mac"), UNKNOWN("Unknown");
        private final String label;

        private OsType(String label) {
            this.label = label;
        }
        
        public String getLabel() {
            return label;
        }
    }

    private HostInfoUtils() {
        //prevent instantiations
    }
    
    public static OsType getOsType() {
        String osName = System.getProperty("os.name");
        if (osName ==null) {
            logger.warn("Failed to identify OS, system property `os.name` is null");
            return OsType.UNKNOWN; 
        } else if (osName.toLowerCase().startsWith("windows")) {
            return OsType.WINDOWS;
        } else if (osName.toLowerCase().startsWith("linux")) {
            return OsType.LINUX;
        } else if (osName.toLowerCase().startsWith("mac")) {
            return OsType.MAC;
        } else {
            logger.info("Unsupported OS type detected: " + osName);
            return OsType.UNKNOWN;
        }
        
    }

    public static void init(HostInfoReader reader) {
        HostInfoUtils.reader = reader;
    }

    public static String getHostName() {
        return reader.getHostName();
    }

    public static HostId getHostId() {
        return reader.getHostId();
    }
    

    /**
     * Get a map of host information
     * @return
     */
    public static Map<String, Object> getHostMetadata() {
        return reader.getHostMetadata();
    }

    public static NetworkAddressInfo getNetworkAddressInfo() {
        return reader.getNetworkAddressInfo();
    }

    public static class NetworkAddressInfo {
        public NetworkAddressInfo(List<String> ipAddresses, List<String> macAddresses) {
            super();
            this.ipAddresses = ipAddresses;
            this.macAddresses = macAddresses;
        }
        private final List<String> ipAddresses;
        private final List<String> macAddresses;

        public List<String> getIpAddresses() {
            return ipAddresses;
        }

        public List<String> getMacAddresses() {
            return macAddresses;
        }
    }
}
