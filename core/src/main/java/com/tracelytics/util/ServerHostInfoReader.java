package com.tracelytics.util;

import com.tracelytics.joboe.config.ConfigManager;
import com.tracelytics.joboe.config.ConfigProperty;
import com.tracelytics.joboe.rpc.HostType;
import com.tracelytics.joboe.HostId;
import com.tracelytics.joboe.span.impl.ScopeContextSnapshot;
import com.tracelytics.joboe.span.impl.ScopeManager;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.Metadata;
import com.tracelytics.logging.Logger;
import com.tracelytics.logging.LoggerFactory;
import com.tracelytics.util.HostInfoUtils.NetworkAddressInfo;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import com.google.auto.service.AutoService;

/**
 * Helper to extract information on the host this JVM runs on
 * @author pluk
 *
 */
@AutoService(HostInfoReader.class)
public class ServerHostInfoReader implements HostInfoReader {
    public static final ServerHostInfoReader INSTANCE = new ServerHostInfoReader();
    private static Logger logger = LoggerFactory.getLogger();
    
    private static final int HOST_ID_CHECK_INTERVAL = 60;

    private static String distro;
    private static HostId hostId; //lazily initialized to avoid cyclic init
    private static String hostname;
    private static UUID uuid;
    private static boolean checkedDistro = false;
    private static HostInfoUtils.OsType osType = HostInfoUtils.getOsType();

    static final String HOSTNAME_SEPARATOR = ";";

    static enum DistroType { AMAZON, UBUNTU, DEBIAN, REDHAT_BASED, SUSE, SLACKWARE, GENTOO }
    static final Map<DistroType, String> DISTRO_FILE_NAMES = new HashMap<DistroType, String>();
    
    public static final String HOSTNAME_ALIAS_KEY = "ConfiguredHostname";

    static {

        DISTRO_FILE_NAMES.put(DistroType.AMAZON, "/etc/system-release-cpe");
        DISTRO_FILE_NAMES.put(DistroType.REDHAT_BASED, "/etc/redhat-release");
        DISTRO_FILE_NAMES.put(DistroType.UBUNTU, "/etc/lsb-release");
        DISTRO_FILE_NAMES.put(DistroType.DEBIAN, "/etc/debian_version");
        DISTRO_FILE_NAMES.put(DistroType.SUSE, "/etc/SuSE-release");
        DISTRO_FILE_NAMES.put(DistroType.SLACKWARE, "/etc/slackware-version");
        DISTRO_FILE_NAMES.put(DistroType.GENTOO, "/etc/gentoo-release");
    }

    private ServerHostInfoReader() {
        //prevent instantiations
    }

    public String getAwsInstanceId() {
        return Ec2InstanceReader.getInstanceId();
    }
    
    public String getAwsAvailabilityZone() {
        return Ec2InstanceReader.getAvailabilityZone();
    }
    
    public String getDockerContainerId() {
        return DockerInfoReader.getDockerId();
    }
    
    public String getHerokuDynoId() {
        return HerokuDynoReader.getDynoId();
    }

    public UUID getUuid()
    {
        if (uuid == null)
        {
            uuid = UUID.randomUUID();
        }

        return uuid;
    }

    @Override
    public String getAzureInstanceId() {
        return AzureReader.getInstanceId();
    }

    // The network adapter status. It's used for Windows only.
    enum NicStatus {
        NOT_PRESENT("Not Present"),
        UP("Up"),
        DISCONNECTED("Disconnected");

        private final String desc;
        private static final Map<String,NicStatus> ENUM_MAP;

        static {
            Map<String,NicStatus> map = new ConcurrentHashMap<String, NicStatus>();
            for (NicStatus instance : NicStatus.values()) {
                map.put(instance.getDesc(),instance);
            }
            ENUM_MAP = Collections.unmodifiableMap(map);
        }

        NicStatus(String desc) {
            this.desc = desc;
        }

        public String getDesc() {
            return this.desc;
        }

        public static NicStatus fromDesc(String desc) {
            return ENUM_MAP.get(desc);
        }
    }

    /**
     * Get the network adapters status by executing a command on Windows.
     * @param nicStatusMap
     */
    private static void buildNicStatusMap(Map<String, NicStatus> nicStatusMap) {
        String output;
        try {
            output = ExecUtils.exec("powershell.exe Get-NetAdapter -IncludeHidden | Select-Object InterfaceDescription,Status | Format-Table -AutoSize",
                    System.getProperty("line.separator"));
        } catch (Exception e) {
            logger.info("Failed to obtain nic status from exec `Get-NetAdapter` : " + e.getMessage());
            return;
        }

        String[] lines = output.split(System.getProperty("line.separator"));
        if (lines.length < 3) {
            logger.info("No enough data received from exec `Get-NetAdapter`(" + lines.length + "): " + Arrays.toString(lines));
            return;
        }

        String header = lines[0];
        int statusStartPoint = header.indexOf("Status");
        if (statusStartPoint == -1) {
            logger.info("Failed to obtain nic status as the header `Status` is not found. " + Arrays.toString(lines));
            return;
        }

        lines = Arrays.copyOfRange(lines, 2, lines.length);

        for (String line : lines) {
            if (statusStartPoint > line.length()) {
                continue;
            }
            String name = line.substring(0, statusStartPoint).trim();
            NicStatus status = NicStatus.fromDesc(line.substring(statusStartPoint).trim());
            logger.debug("Get device display name=" + name + ", status=" + status);
            nicStatusMap.put(name, status);
        }

    }
    /**
     * Extracts network interface info from the system. Take note that loop back, point-to-point and non-physical addresses are excluded
     * @return
     */
    @Override
    public NetworkAddressInfo getNetworkAddressInfo() {
        try {
            List<String> ips = new ArrayList<String>();
            List<String> macAddresses = new ArrayList<String>();

            // map of device id -> status
            Map<String, NicStatus> nicStatusMap = new HashMap<String, NicStatus>();
            boolean isWindowsIPv4 = false;
            if (osType.equals(HostInfoUtils.OsType.WINDOWS) && Boolean.getBoolean("java.net.preferIPv4Stack")) {
                buildNicStatusMap(nicStatusMap);
                isWindowsIPv4 = true;
            }

            for (NetworkInterface networkInterface : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                try {
                    logger.debug("Found network interface " + networkInterface.getName() + " " + networkInterface.getDisplayName());
                    if (!networkInterface.isLoopback() && !networkInterface.isPointToPoint() && isPhysicalInterface(networkInterface) && !isGhostHyperV(networkInterface)) {
                        logger.debug("Processing physical network interface " + networkInterface.getName() + " " + networkInterface.getDisplayName());
                        boolean hasIp = false;
                        for (InetAddress address : Collections.list(networkInterface.getInetAddresses())) {
                            String ipAddress = address.getHostAddress();
                            logger.debug("Found ip address " + ipAddress);
                            if (!ips.contains(ipAddress)) {
                                ips.add(ipAddress);
                            }
                            hasIp = true;
                        }

                        if (isWindowsIPv4) { //extra check for windows IPv4 preferred environment, see https://github.com/librato/joboe/pull/1090 for details
                            NicStatus status = nicStatusMap.get(networkInterface.getDisplayName());
                            logger.debug("Checking " + networkInterface.getName() + ", " + networkInterface.getDisplayName() + ", status=" + status);
                            if (!(NicStatus.UP.equals(status) || NicStatus.DISCONNECTED.equals(status))) {
                                // ignore disabled/null NIC on Windows when "-Djava.net.preferIPv4Stack=true" is set
                                logger.debug("Ignore disabled/null network adapter " + networkInterface.getDisplayName() + ", status=" + status);
                                continue;
                            }
                            // We cannot simply filter out NICs without an IP for all the scenarios. This is because if an NIC is DISCONNECTED, it will have
                            // some kind of `link local` IP address (169.254. 0.0/16). The link local IP can be fetched by the Go API (used by the
                            // host agent) but not by the Java Windows API when `java.net.preferIPv4Stack`=true.
                            // Therefore, if the isWindowsIPv4 is true (check that when it's true), we'll accept the DISCONNECTED NIC without
                            // considering if it has an IP. For all other cases, that NIC will be filtered out if it doesn't have an IP address.
                            if (NicStatus.UP.equals(status) && !hasIp) {
                                logger.debug("Ignore network adapter which is up but no IP assigned: " + networkInterface.getDisplayName());
                                continue;
                            }
                        } else if (!hasIp){
                            logger.debug("Ignore network adapter without an IP: " + networkInterface.getName());
                            continue;
                        }
                        //add mac addresses too
                        byte[] hwAddr = networkInterface.getHardwareAddress();
                        if ((hwAddr != null) && (hwAddr.length != 0)) {
                            String macAddress = getMacAddressFromBytes(hwAddr); 
                            logger.debug("Found MAC address " + macAddress);
                            if (!macAddresses.contains(macAddress)) {
                                macAddresses.add(macAddress);
                            }
                        }
                    }
                } catch (NoSuchMethodError e) {
                    logger.debug("Failed to get network info for " +  networkInterface.getName() + ", probably running JDK 1.5 or earlier");
                } catch (SocketException e) {
                    logger.debug("Failed to get network info for " +  networkInterface.getName() + ":" + e.getMessage());
                }
            }
            logger.debug("All MAC addresses accepted: " + Arrays.toString(macAddresses.toArray()));
            return new NetworkAddressInfo(ips, macAddresses);
        } catch (SocketException e) {
            logger.warn("Failed to get network info : " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Determines whether a network interface is physical (for Linux only). 
     * 
     * By our definition, a network interface is considered physical if its link in /sys/class/net does NOT contain "/virtual/"
     * https://github.com/librato/joboe/issues/728
     * 
     * Take note that this definition is different from NetworkInterface.isVirtual()
     *  
     * @param networkInterface
     * @return
     */
    private static boolean isPhysicalInterface(NetworkInterface networkInterface) {
        if (osType != HostInfoUtils.OsType.LINUX) { //not applicable to non-linux systems, consider all interfaces physical
            return true;
        }
        
        //cannot use java.nio.file.Files.readSymbolicLink as it's not available in jdk 1.6
        String interfaceFilePath = "/sys/class/net/" + networkInterface.getName();
        File interfaceFile = new File(interfaceFilePath);
        try {
            return !interfaceFile.getCanonicalPath().contains("/virtual/");
        } catch (IOException e) {
            logger.warn("Error identifying network interface in " + interfaceFilePath + " message : " + e.getMessage(), e);
            return true; //having problem identifying the interface, treat it as physical
        }
    }

    /**
     * To detect whether it is a ghost Microsoft Hyper-V Network Adapter.
     *
     * Since in IPv4 preferred environment, `NetworkInterface.getAll` might return ghost Microsoft Hyper-V Network Adapter
     *
     * https://swicloud.atlassian.net/browse/AO-14670 for details
     * @param networkInterface
     * @return  false if it's NOT a Microsoft Hyper-V Network Adapter or it's UP
     */
    private static boolean isGhostHyperV(NetworkInterface networkInterface) {
        final String hyperVPrefix = "Microsoft Hyper-V Network Adapter";
        try {
            String displayName = networkInterface.getDisplayName();
            return displayName != null && displayName.startsWith(hyperVPrefix) && !networkInterface.isUp();
        } catch (SocketException e) {
            logger.debug("Cannot call isUp on " + networkInterface.getDisplayName(), e);
            return false;
        }
    }

    private static String getMacAddressFromBytes(byte[] bytes) {
        StringBuilder sb = new StringBuilder(18);
        for (byte b : bytes) {
            if (sb.length() > 0)
                sb.append(':');
            sb.append(String.format("%02x", b));
        }
        return sb.toString().toUpperCase();
    }

    @Override
    public synchronized String getHostName() {
        if (hostname == null) { //only load host name on the first time as we wish to minimize spawning processes
            hostname = loadHostName();
        }
        return hostname;
    }

    @Override
    public synchronized HostId getHostId() {
        if (hostId == null) {
            Metadata existingMetdataContext = null;
            ScopeContextSnapshot scopeContextSnapshot = null;
            try {
                existingMetdataContext = Context.getMetadata();
                scopeContextSnapshot = ScopeManager.INSTANCE.getSnapshot();

                Context.clearMetadata(); //make sure our init route does not accidentally trigger tracing instrumentations
                ScopeManager.INSTANCE.removeAllScopes(); //make sure no scope is active for this thread

                //synchronously get it once first to ensure it's available at the return statement
                hostId = buildHostId();
                //also start the background checker here
                startHostIdChecker();
            } finally {
                Context.setMetadata(existingMetdataContext); //set the existing context back
                scopeContextSnapshot.restore();
            }
        }
        return hostId;
    }
    
    public static String loadHostName() {
        String hostName = loadHostNameFromExec();
        
        if (hostName != null) {
            return hostName;
        }
        
        hostName = loadHostNameFromInetAddress();
        
        if (hostName != null) {
            return hostName;
        }
        
        // We've failed to get an IP address, so as a last resort...
        hostName = "unknown_hostname";
        return hostName;
    }
    
    /**
     * This was copied from JAgentInfo.java
     * 
     * Returns system host name using external 'hostname' command.
     *
     * This feels lame, but there is no other way to get a hostname that definitively matches
     * the gethostname() C library function we use elsewhere (liboboe, tracelyzer, etc.) without
     * resorting to JNI. We only do this at startup so I think we can live with it.
     *
     * 'InetAddress.getHostName()' and 'getCanonicalHostName()' may vary or fail based on 
     * DNS settings, /etc/hosts settings, etc. and just aren't guaranteed to be the same 
     * as gethostname(), even though on many systems they are.
     *
     * Also see http://stackoverflow.com/questions/7348711/recommended-way-to-get-hostname-in-java
     */
    private static String loadHostNameFromExec() {
        try {
            return ExecUtils.exec("hostname");
        } catch (Exception e) {
            //in some "slim" os, it might not have `hostname`. For example Orcale linux slim
            logger.info("Failed to obtain host name from exec `hostname` : " + e.getMessage());
            return null;
        }
    }
    
    /**
     * This was copied from JAgentInfo.java
     * @return
     */
    private static String loadHostNameFromInetAddress() {
        try {
            InetAddress addr = InetAddress.getLocalHost();
            return addr.getCanonicalHostName();
        } catch(UnknownHostException hostEx) {
            // Our hostname doesn't resolve, likely a mis-configured host, so fallback to using the first non-loopback IP address
            try {
                Enumeration <NetworkInterface> netInts = NetworkInterface.getNetworkInterfaces();
                NetworkInterface netInt;
                Enumeration <InetAddress> addrs;
                InetAddress addr;
                
                while (netInts.hasMoreElements()) {
                    netInt = netInts.nextElement();
                    addrs = netInt.getInetAddresses();
                
                    while (addrs.hasMoreElements()) {
                        addr = addrs.nextElement();
                
                        if (!addr.isLoopbackAddress() && !addr.isLinkLocalAddress()) {
                            return addr.getHostAddress();
                        }
                    }
                }
                
            } catch(SocketException socketEx) {
                logger.warn("Unable to retrieve network interfaces", socketEx);
            }
            
            return null;
        }
    }
    
    public static String getDistro() {
        if (!checkedDistro) {
            if (osType == HostInfoUtils.OsType.LINUX) {
                distro = getLinuxDistro();
            } else if (osType == HostInfoUtils.OsType.WINDOWS) {
                distro = getWindowsDistro();
            }
            checkedDistro = true;
        }
        return distro; //could be null for unsupported system
    }


    @Override
    /**
     * Get a map of host information
     * @return
     */
    public Map<String, Object> getHostMetadata() {
        HashMap<String, Object> infoMap = new HashMap<String, Object>();
        
        String hostnameAlias = (String) ConfigManager.getConfig(ConfigProperty.AGENT_HOSTNAME_ALIAS);
        if (hostnameAlias != null) {
            infoMap.put(HOSTNAME_ALIAS_KEY, hostnameAlias);
        }
        
        addOsInfo(infoMap);
        addNetworkAddresses(infoMap);
        
        return infoMap;
    }
    
    private static void addOsInfo(Map<String, Object> infoMap) {
        infoMap.put("UnameSysName", osType.getLabel());
        infoMap.put("UnameVersion", ManagementFactory.getOperatingSystemMXBean().getVersion());
        
        String distro = getDistro();
        if (distro != null) {
            infoMap.put("Distro", distro);
        }
    }

    private void addNetworkAddresses(Map<String, Object> infoMap) {
        NetworkAddressInfo networkInfo = getNetworkAddressInfo();
        if (networkInfo != null) {
            infoMap.put("IPAddresses", networkInfo.getIpAddresses());
        }
    }

    /**
     * 
     * Identifies the distro value on a Linux system
     * 
     * Code logic copied from https://github.com/librato/oboe/blob/a3dd998e7ea239e3d5e5c7ece8c635c3ff61c903/liboboe/reporter/ssl.cc#L559
     * @return
     */
    private static String getLinuxDistro() {
        // Note: Order of checking is important because some distros share same file names but with different function.
        // Keep this order: redhat based -> ubuntu -> debian
        
        BufferedReader fileReader = null;
        
        try {
            if ((fileReader = getFileReader(DistroType.REDHAT_BASED)) != null) {
                // Redhat, CentOS, Fedora
                return getRedHatBasedDistro(fileReader);
            } else if ((fileReader = getFileReader(DistroType.AMAZON)) != null) {
                // Amazon Linux
                return getAmazonLinuxDistro(fileReader);
            } else if ((fileReader = getFileReader(DistroType.UBUNTU)) != null) {
                // Ubuntu
                return getUbuntuDistro(fileReader);
            } else if ((fileReader = getFileReader(DistroType.DEBIAN)) != null) {
                // Debian
                return getDebianDistro(fileReader);
            } else if ((fileReader = getFileReader(DistroType.SUSE)) != null) {
                // Novell SuSE
                return getNovellSuseDistro(fileReader);
            } else if ((fileReader = getFileReader(DistroType.SLACKWARE)) != null) {
                // Slackware
                return getSlackwareDistro(fileReader);
            } else if ((fileReader = getFileReader(DistroType.SLACKWARE)) != null) {
                return getGentooDistro(fileReader);
            } else {
                return "Unknown";
            }
        } catch (IOException e) {
            logger.warn("Problem reading distro file : " + e.getMessage(), e);
            return "Unknown";
        } finally {
            if (fileReader != null) {
                try {
                    fileReader.close();
                } catch (IOException e) {
                    logger.warn(e.getMessage(), e);
                }
            }
        }
    }
    
    private static String getWindowsDistro() {
        return ManagementFactory.getOperatingSystemMXBean().getName();
    }
    
    static String getGentooDistro(BufferedReader fileReader) throws IOException {
        String line;
        return (line = fileReader.readLine()) != null ? line : "Gentoo unknown";
    }

    static String getSlackwareDistro(BufferedReader fileReader) throws IOException {
        String line;
        return (line = fileReader.readLine()) != null ? line : "Slackware unknown";
    }

    static String getNovellSuseDistro(BufferedReader fileReader) throws IOException {
        String line;
        return (line = fileReader.readLine()) != null ? line : "Novell SuSE unknown";
    }

    static String getDebianDistro(BufferedReader fileReader) throws IOException {
        String line;
        return (line = fileReader.readLine()) != null ? ("Debian " + line) : "Debian unknown";
    }

    static String getRedHatBasedDistro(BufferedReader fileReader) throws IOException {
        String line;
        return (line = fileReader.readLine()) != null ? line : "Red Hat based unknown";
    }
    
    static String getAmazonLinuxDistro(BufferedReader fileReader) throws IOException {
        String line;
        if ((line = fileReader.readLine()) != null) {
            String[] tokens = line.split(":");
            if (tokens.length >= 5) {
                String patch = tokens[4];
                return "Amzn Linux " + patch;
            }
        }
        return "Amzn Linux unknown";
    }
    
    static String getUbuntuDistro(BufferedReader fileReader) throws IOException {
        String line;
        while ((line = fileReader.readLine()) != null) {
            String[] tokens = line.split("=");
            if (tokens.length >= 2 && "DISTRIB_DESCRIPTION".equals(tokens[0])) {
                // trim trailing/leading quotes
                String patch = tokens[1];
                if (patch.startsWith("\"")) {
                    patch = patch.substring(1);
                } 
                if (patch.endsWith("\"")) {
                    patch = patch.substring(0, patch.length() - 1);
                }
                
                return patch;
            }
        }
        return "Ubuntu unknown";
    }
    
    
    
    private static BufferedReader getFileReader(DistroType distroType) {
        String path = DISTRO_FILE_NAMES.get(distroType);
        if (path == null) {
            logger.warn("Unexpected distroType lookup: " + distroType);
            return null;
        }
        File file = new File(path);
        try {
            return file.exists() ? new BufferedReader(new FileReader(file)) : null;
        } catch (FileNotFoundException e) {
            logger.warn(e.getMessage(), e);
            return null;
        }
    }
    
    private void startHostIdChecker() {
        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1, DaemonThreadFactory.newInstance("host-id-checker"));
        //align to minute, figure out the delay in ms
        long delay = 60 * 1000 - System.currentTimeMillis() % (60 * 1000); 
        executorService.scheduleAtFixedRate(new Runnable() {
            public void run() {
                hostId = buildHostId();
            }
        }, delay, HOST_ID_CHECK_INTERVAL * 1000, TimeUnit.MILLISECONDS);
    }
    
    private HostId buildHostId() {
        NetworkAddressInfo networkAddressInfo = getNetworkAddressInfo();
        List<String> macAddresses = networkAddressInfo != null ? networkAddressInfo.getMacAddresses() : Collections.<String>emptyList();
        //assume all that uses HostInfoUtils are persistent server, can be improved to recognize different types later
        return new HostId(getHostName(), JavaProcessUtils.getPid(), macAddresses, Ec2InstanceReader.getInstanceId(), Ec2InstanceReader.getAvailabilityZone(), DockerInfoReader.getDockerId(), HerokuDynoReader.getDynoId(), AzureReader.getInstanceId(), HostType.PERSISTENT, UamsClientIdReader.getUamsClientId(), getUuid());
    }
    
    public static class Ec2InstanceReader {
        private static final int TIMEOUT_DEFAULT = 1000;
        private static final int TIMEOUT_MIN = 0; //do not wait at all, disable retrieval
        private static final int TIMEOUT_MAX = 3000;

        private static final int TIMEOUT = getTimeout();

        private static int getTimeout() {
            Integer configValue = (Integer) ConfigManager.getConfig(ConfigProperty.AGENT_EC2_METADATA_TIMEOUT);
            if (configValue == null) {
                return TIMEOUT_DEFAULT;
            } else if (configValue > TIMEOUT_MAX) {
                logger.warn("EC2 metadata read timeout cannot be greater than " + TIMEOUT_MAX + " millisec but found [" + configValue + "]. Using " + TIMEOUT_MAX + " instead.");
                return TIMEOUT_MAX;
            } else if (configValue < TIMEOUT_MIN) {
                logger.warn("EC2 metadata read timeout cannot be smaller than " + TIMEOUT_MIN + " millisec but found [" + configValue + "]. Using " + TIMEOUT_MIN + " instead, which essentially disable reading EC2 metadata");
                return TIMEOUT_MIN;
            } else {
                return configValue;
            }
        }

        /**
         * System property for overriding the Amazon EC2 Instance Metadata Service
         * endpoint.
         */
        public static final String EC2_METADATA_SERVICE_OVERRIDE_SYSTEM_PROPERTY = "com.amazonaws.sdk.ec2MetadataServiceEndpointOverride";
        
        private static final String EC2_METADATA_SERVICE_URL = "http://169.254.169.254";
        private static final String INSTANCE_ID_PATH = "latest/meta-data/instance-id";
        private static final String AVAILABILITY_ZONE_PATH = "latest/meta-data/placement/availability-zone";
        
        private String instanceId;
        private String availabilityZone;
        
        private static final Ec2InstanceReader SINGLETON = new Ec2InstanceReader();
        
        public static String getInstanceId() {
            return SINGLETON.instanceId;
        }
        
        public static String getAvailabilityZone() {
            return SINGLETON.availabilityZone;
        }
        
        private Ec2InstanceReader() {
            initialize();
        }
        
        private void initialize() {
            if (TIMEOUT == TIMEOUT_MIN) { //disable reader
                return;
            }
            instanceId = getResourceOnEndpoint(INSTANCE_ID_PATH);
            if (instanceId != null) { //only proceed if instance id can be found
                availabilityZone = getResourceOnEndpoint(AVAILABILITY_ZONE_PATH);
                logger.debug("Found EC2 instance id " + instanceId + " availability zone: " + availabilityZone);
            }
        }
        
        private static String getResourceOnEndpoint(String relativePath) {
            HttpURLConnection connection =  null;
            BufferedReader reader = null;
            try {
                URI uri = new URI(getMetadataHost() + "/" + relativePath);
                connection = (HttpURLConnection) uri.toURL().openConnection(Proxy.NO_PROXY);
                connection.setConnectTimeout(TIMEOUT);
                connection.setReadTimeout(TIMEOUT);

                int statusCode = connection.getResponseCode();

                if (statusCode == HttpURLConnection.HTTP_OK) {
                    reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    return reader.readLine();
                } else {
                    return null;
                } 
            } catch (IOException e) { 
                logger.debug("Timeout on reading EC2 metadata after waiting for [" + TIMEOUT + "] milliseconds. Probably not an EC2 instance");
                return null; 
            } catch (URISyntaxException e) {
                logger.warn(e.getMessage(), e);
                return null;
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }
        
        private static String getMetadataHost() {
            String host = System.getProperty(EC2_METADATA_SERVICE_OVERRIDE_SYSTEM_PROPERTY);
            return host != null ? host : EC2_METADATA_SERVICE_URL;
        }
    }
    
    public static class DockerInfoReader {
        public static final String DEFAULT_LINUX_DOCKER_FILE_LOCATION = "/proc/self/cgroup";
        private static final int DOCKER_ID_LENGTH = 64;
        private static final Set<String> DOCKER_CGROUP_PATH_TOKEN = new HashSet<String>();
        
        static {
            DOCKER_CGROUP_PATH_TOKEN.add("docker");
            DOCKER_CGROUP_PATH_TOKEN.add("ecs");
            DOCKER_CGROUP_PATH_TOKEN.add("kubepods");
            DOCKER_CGROUP_PATH_TOKEN.add("docker.service");
        }
        
        private String dockerId;
        
        static final DockerInfoReader SINGLETON = new DockerInfoReader();
        
        public static String getDockerId() {
            return SINGLETON.dockerId;
        }
        
        private DockerInfoReader() {
            if (osType == HostInfoUtils.OsType.LINUX) {
                initializeLinux(DEFAULT_LINUX_DOCKER_FILE_LOCATION);
            } else if (osType == HostInfoUtils.OsType.WINDOWS) {
                initializeWindows();
            }
            
            if (dockerId != null) {
                logger.debug("Found Docker instance ID :" + this.dockerId);
            } else {
                logger.debug("Cannot locate Docker id, not a Docker container");
            }
        }
        
        void initializeLinux(String dockerFileLocation) {
            BufferedReader reader = null;
            
            try {
                reader = new BufferedReader(new FileReader(dockerFileLocation));
                String line;
                
                // refers to logic from c-lib https://github.com/librato/oboe/blob/af14cd2daaba7b6c21fa4aa780b222f02fe95f07/liboboe/reporter/ssl.cc#L1134
                // iterate over each line and look for the keyword "docker" or "ecs"
                while ((line = reader.readLine()) != null) {
                    List<String> tokens = Arrays.asList(line.split("/"));
                    if (!Collections.disjoint(tokens, DOCKER_CGROUP_PATH_TOKEN)) { //if the tokens contains any of the valid "docker" label segments in it
                        for (String token : tokens) {
                            if (token.length() == DOCKER_ID_LENGTH) {
                                dockerId = token;
                                return;
                            }
                        }
                    }
                }
                
                dockerId = null;
            } catch (FileNotFoundException e) {
                dockerId = null;
                logger.debug("Cannot locate docker id as file " + dockerFileLocation + " cannot be found : " + e.getMessage());
            } catch (IOException e) {
                dockerId = null;
                logger.debug("Cannot locate docker id as file " + dockerFileLocation + " throws IOException : " + e.getMessage());
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        logger.warn(e.getMessage(), e);
                    }   
                }
            }
        }
        
        /**
         * Initializes Windows docker ID if the java process is running within a Windows docker container
         * 
         * Determines if it is a Windows container by checking if `cexecsvc` exists in `powershell get-process`
         * 
         * If so, set the host name as Docker ID 
         * 
         */
        private void initializeWindows() {
            try {
                String getContainerTypeResult = ExecUtils.exec("powershell Get-ItemProperty -Path HKLM:\\SYSTEM\\CurrentControlSet\\Control\\ -Name \"ContainerType\"");
                if (getContainerTypeResult != null && !"".equals(getContainerTypeResult)) {
                    dockerId = ServerHostInfoReader.INSTANCE.getHostName();
                }
            } catch (Exception e) {
                logger.info("Failed to identify whether this windows system is a docker container: " + e.getMessage());
            }
        }
    }
    
    
    public static class HerokuDynoReader {
        private static final String DYNO_ENV_VARIABLE = "DYNO";
        private static final HerokuDynoReader SINGLETON = new HerokuDynoReader();
        
        private final String dynoId;
        
        public static String getDynoId() {
            return SINGLETON.dynoId;
        }
        
        private HerokuDynoReader() {
            this.dynoId = System.getenv(DYNO_ENV_VARIABLE);
            if (this.dynoId != null) {
                logger.debug("Found Heroku Dyno ID: " + this.dynoId);
            }
        }
     }

    public static class AzureReader {
        private static final String INSTANCE_ID_ENV_VARIABLE = "WEBSITE_INSTANCE_ID";
        private static final AzureReader SINGLETON = new AzureReader();

        private final String instanceId;

        public static String getInstanceId() {
            return SINGLETON.instanceId;
        }

        private AzureReader() {
            this.instanceId = System.getenv(INSTANCE_ID_ENV_VARIABLE);
            if (this.instanceId != null) {
                logger.debug("Found Azure instance ID: " + this.instanceId);
            }
        }
    }
}
