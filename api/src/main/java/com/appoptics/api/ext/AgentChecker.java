package com.appoptics.api.ext;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.tracelytics.agent.Agent;
import com.tracelytics.agent.Agent.AgentStatus;
import com.tracelytics.joboe.StartupManager;

/**
 * Checker to ensure agent is available and ready to report data
 * @author pluk
 *
 */
public class AgentChecker {
    private static Logger logger = Logger.getLogger("agent-sdk");
    public static final String REQUIRED_AGENT_VERSION = "6.9.0"; //minimum core version in order to get functional handler, this number has to be updated if new methods are added
    static boolean isAgentAvailable = false;
    
    static {
        try {
            Class.forName("com.tracelytics.agent.Agent");
            
            if (Agent.getVersion() != null && compareVersion(Agent.getVersion(), REQUIRED_AGENT_VERSION) >= 0) {
                if (Agent.getStatus() == AgentStatus.INITIALIZED_SUCCESSFUL) {
                    isAgentAvailable = true;
                    
                    //if api call is invoked, we assume that system loading is ready, let's flag agent
                    StartupManager.flagSystemStartupCompleted();
                } else {
                    logger.warning("SDK calls would be ignored. Agent was not initialized properly");
                }
            } else {
                logger.warning("SDK calls would be ignored. Require -javaagent of at least version [" + REQUIRED_AGENT_VERSION + "] but found version [" + Agent.getVersion() + "] running");
            }
        } catch (ClassNotFoundException e) {
            /* This is expected in cases where the Tracelytics jar is not available */
            logger.log(Level.INFO, "Java Agent not available");
        } catch (NoClassDefFoundError e) {
            /* This is not so expected as ClassLoader is supposed to throw ClassNotFoundException, but some loaders might throw NoClassDefFoundError instead */
            logger.log(Level.INFO, "Java Agent not available");
        } catch (NoSuchMethodError e) {
            logger.log(Level.INFO, "Java Agent is older than [" + REQUIRED_AGENT_VERSION + "]. SDK calls would be ignored");
        }
        
    }
    
    /**
     * Returns whether the java agent is available via -javaagent. If the agent is not available, api calls will be ignored
     * @return
     */
    public static boolean isAgentAvailable() {
        return isAgentAvailable;
    }
    
    /**
     * Returns 1 if current version is newer than required version, 0 if the same, -1 otherwise
     * @param version
     * @param requiredAgentVersion
     * @return
     */
    static int compareVersion(String currentVersion, String requiredVersion) {
        try {
            return Version.getVersion(currentVersion).compareTo(Version.getVersion(requiredVersion));
        } catch (IllegalArgumentException e) {
            return -1;
        }
    }

    /**
     * Blocks until agent is ready (established connection with data collector) or timeout expired. 
     * 
     * Take note that if an agent is not ready, traces and metrics collected will not be processed.
     * 
     * Call this method to ensure agent is ready before reporting traces for one-off batch jobs
     *     
     * @param timeout
     * @param unit
     * @return  whether the agent is ready
     */
    public static boolean waitUntilAgentReady(long timeout, TimeUnit unit) {
        if (isAgentAvailable) {
            try {
                StartupManager.isAgentReady().get(timeout, unit);
                return true;
            } catch (Exception e) {
                logger.warning("Agent is still not ready after waiting for " + timeout + " " + unit);
                return false;
            }
        }
        return false;
    }
    
    private static class Version implements Comparable<Version>{
        private int majorVersion;
        private int minorVersion;
        private int patchVersion;
        private String extension;
        
        private Version(int majorVersion, int minorVersion, int patchVersion, String extension) {
            super();
            this.majorVersion = majorVersion;
            this.minorVersion = minorVersion;
            this.patchVersion = patchVersion;
            this.extension = extension;
        }

        public static Version getVersion(String versionString) throws IllegalArgumentException {
            String[] tokens = versionString.split("\\.");
            if (tokens.length != 3) {
                throw new IllegalArgumentException("Version String " + versionString + " is not in the expected format of x.x.x");
            }
            
            try {
                int majorVersion = Integer.parseInt(tokens[0]);
                int minorVersion = Integer.parseInt(tokens[1]);
                int extensionIndex = tokens[2].indexOf("-");
                int patchVersion;
                String extension;
                if (extensionIndex > 0) {
                    patchVersion = Integer.parseInt(tokens[2].substring(0, extensionIndex));
                    extension = tokens[2].substring(extensionIndex + 1);
                } else {
                    patchVersion = Integer.parseInt(tokens[2]);
                    extension = null;
                }
                
                return new Version(majorVersion, minorVersion, patchVersion, extension);
            } catch (Exception e) {
                throw new IllegalArgumentException("Version String " + versionString + " is not in the expected format of x.x.x", e);
            }
            
        }
        
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((extension == null) ? 0 : extension.hashCode());
            result = prime * result + majorVersion;
            result = prime * result + minorVersion;
            result = prime * result + patchVersion;
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Version other = (Version) obj;
            if (extension == null) {
                if (other.extension != null)
                    return false;
            } else if (!extension.equals(other.extension))
                return false;
            if (majorVersion != other.majorVersion)
                return false;
            if (minorVersion != other.minorVersion)
                return false;
            if (patchVersion != other.patchVersion)
                return false;
            return true;
        }

        @Override
        public int compareTo(Version other) {
            if (majorVersion > other.majorVersion) {
                return 1;
            } else if (majorVersion < other.majorVersion) {
                return -1;
            } else if (minorVersion > other.minorVersion) {
                return 1;
            } else if (minorVersion < other.minorVersion) {
                return -1;
            } else if (patchVersion > other.patchVersion) {
                return 1;
            } else if (patchVersion < other.patchVersion) {
                return -1;
            } else if (extension != null && other.extension != null) { //simply compare the extension string for now instead of doing further parsing
                return extension.compareTo(other.extension);
            } else if (extension == null && other.extension == null) {
                return 0;
            } else {
                return extension != null ? -1 : 1;
            }
            
        }
        
        
        
    }
}
