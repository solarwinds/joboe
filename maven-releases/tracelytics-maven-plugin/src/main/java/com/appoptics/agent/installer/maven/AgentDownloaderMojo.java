package com.appoptics.agent.installer.maven;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Performs the download-agent of the maven plugin, which is mainly used to download the agent and the native libraries to an arbitrary location.
 * 
 * @author ibakalov
 */
@Mojo( name = "download-agent", requiresProject = false )
public class AgentDownloaderMojo extends AbstractAgentMojo {

    private static final String AGENT_DEFAULT_MAVEN_PATH = "./target";
    private static final String AGENT_DEFAULT_COMMANDLINE_PATH = ".";
    
    /**
     * Contains the injected value of the configuration property for the agent operating system. 
     */
    @Parameter( property = "agentOperatingSystem", defaultValue = "all" )
    private String agentOperatingSystem;

    
    /** 
     * The available values are "windows", "linux" and "all"
     *
     * @see com.appneta.agent.installer.maven.AbstractAgentMojo#getTargetOSTypes()
     */
    @Override
    protected List<OSType> getTargetOSTypes() throws MojoFailureException {
        String os = agentOperatingSystem;
        if (os != null) {
            os = os.toLowerCase();
        }
        
        if ("windows".equals(os)) {
            return Collections.singletonList(OSType.WINDOWS);
        } else if ("linux".equals(os)) {
            return Collections.singletonList(OSType.LINUX);
        } else if ("all".equals(os)) {
            List<OSType> allTypes = new ArrayList<AbstractAgentMojo.OSType>(4);
            for (OSType osType : OSType.values()) {
                if (!(OSType.UNKNOWN == osType)) {
                    allTypes.add(osType);
                }
            }
            return allTypes; 
        } else {
            throw new MojoFailureException("Unrecongized Agent installer value for property [agentOperatingSystem] : " + os);
        }
        
    }
    
    @Override
    protected String getTargetUnzipLocation() throws MojoFailureException {
        if (agentLocation != null && !"".equals(agentLocation)) {
            return agentLocation;
        }
        if (isCommandLine()) { //from command line 
            return AGENT_DEFAULT_COMMANDLINE_PATH;
        }
        return AGENT_DEFAULT_MAVEN_PATH; //from maven project
    
    }

    @Override
    protected void afterUnzip(String targetLocation) {
        // do nothing
    }

    @Override
    protected void beforeUnzip(String targetLocation) {
        //do nothing
    }
    
    
}
