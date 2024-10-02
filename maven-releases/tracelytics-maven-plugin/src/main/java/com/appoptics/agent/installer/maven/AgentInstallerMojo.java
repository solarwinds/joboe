package com.appoptics.agent.installer.maven;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * Performs the install-agent of the maven plugin, which is mainly used to install the agent to the local machine.
 * @author ibakalov
 */
@Mojo( name = "install-agent", requiresProject = false )
public class AgentInstallerMojo extends AbstractAgentMojo {

    private static final String CONFIG_NEW_FILE_NAME = "javaagent.json.new";
    private static final String CONFIG_BACKUP_FILE_NAME = "javaagent.json.bck";
    private static final String AGENT_DEFAULT_LINUX_PATH = "/usr/local/appoptics";
    private static final String AGENT_DEFAULT_WINDOWS_PATH = "AppNeta\\TraceView\\java";
    private static final String DEFAULT_PROGRAM_FILES_PATH = "C:\\Program Files";
    private static final String AGENT_LOCATION_SYSPROP_1 = "APPNETA_HOME";
    private static final String AGENT_LOCATION_SYSPROP_2 = "OPENSHIFT_TRACEVIEW_DIR";

    private OSType localOSType = getLocalOSType();
    private File configFile = null;
    private File backupFile = null;

    @Override
    protected List<OSType> getTargetOSTypes() throws MojoFailureException {
      List<OSType> result = new ArrayList<OSType>();
      result.add(localOSType);
      return result;
    }

    /**
     * Determines the target location for the agent installation.
     * @return the target location
     * @throws MojoFailureException cannot detect the local system and the target location is not specified by the user.
     */
    @Override
    protected String getTargetUnzipLocation() throws MojoFailureException {
        if (agentLocation != null && !"".equals(agentLocation)) {
            return agentLocation;
        }
        
        String targetLocation = System.getProperty(AGENT_LOCATION_SYSPROP_1, ""); //look up environment vars that the agent code logic is using 
        if (targetLocation == null || "".equals(targetLocation)) {
            targetLocation = System.getProperty(AGENT_LOCATION_SYSPROP_2, "");
        }
        
        log.info("Detected Operating System: " + localOSType);
        
        if (targetLocation == null || "".equals(targetLocation)) {
            if (localOSType == OSType.WINDOWS) {
                String programFilesDirectory = System.getenv("ProgramW6432"); //this is added since Windows Server R2 2008 and Windows 7
                if (programFilesDirectory == null) {
                    programFilesDirectory = System.getenv("ProgramFiles"); //try ProgramFiles then
                    if (programFilesDirectory == null) {
                        programFilesDirectory = DEFAULT_PROGRAM_FILES_PATH;
                    }
                }
                targetLocation = programFilesDirectory + File.separator + AGENT_DEFAULT_WINDOWS_PATH;
            } else if (localOSType == OSType.LINUX) {
                targetLocation = AGENT_DEFAULT_LINUX_PATH;
            } else if (localOSType == OSType.UNKNOWN) {
                log.warn("Unknown Operation System found");
                throw new MojoFailureException("Cannot detect the local operating system type and cannot determine the agent location.");
            }
        }
        return targetLocation;
    }
    
    @Override
    protected void beforeUnzip(String targetLocation) {
        configFile = new File(targetLocation, CONFIG_FILE_NAME);
        backupFile = new File(targetLocation, CONFIG_BACKUP_FILE_NAME);
        if (configFile.exists()) {
            if (backupFile.exists()) { //backup file should not exist unless previous plugin process was interrupted
                backupFile.delete();
            }
            configFile.renameTo(backupFile); //rename the existing config file temporarily to a backup name
        }
    }

    /**
     * Determines the local operating system by auto detection via system properties. 
     * @return The local operating system type
     */
    private OSType getLocalOSType() {
        String os = System.getProperty("os.name"); //get from java properties
        
        if (os != null) {
            os = os.toLowerCase();
        }
        if ((os != null && os.indexOf("win") >= 0) || (os==null && File.separatorChar == '\\')) {
            return OSType.WINDOWS;
        }
        if (( os != null && (os.indexOf("nix") >= 0 || os.indexOf("nux") >= 0 || os.indexOf("aix") > 0)) || (os==null && File.separatorChar == '/')) {
            return OSType.LINUX;
        } else {
            log.warn("Unsupported OS found: " + os);
            return OSType.UNKNOWN;
        }
        
    }

    @Override
    protected void afterUnzip(String targetLocation) {
        if (backupFile.exists()) { //check if an existing config file has been rename temporarily to the backup name
            if (configFile.exists()) { //check if the unzip operation has extracted the config file properly
                boolean filesEqual;
                try {
                    filesEqual = FileUtils.contentEquals(backupFile, configFile);
                } catch (IOException e) {
                    log.warn("Failed to compare the existing config file with the new config file");
                    filesEqual = false;
                }
                if (!filesEqual) {
                    File newConfigFile = new File(targetLocation, CONFIG_NEW_FILE_NAME); //rename the new config file to the new name (therefore not used by default)
                    if (newConfigFile.exists()) {
                        newConfigFile.delete();
                    }
                    
                    log.warn("An existing configuration file was found in the target folder. It will not be overwritten, and the new file will be placed in \"javaagent.json.new\". Please consider using it if you are installing a new version of the agent.");
                    configFile.renameTo(newConfigFile);
                    backupFile.renameTo(configFile);
                } else {
                    backupFile.delete();
                }
            } else {
                log.warn("Cannot find the " + CONFIG_FILE_NAME + " extracted from the agent zip");
                backupFile.renameTo(configFile);
            }
        }
    }
    

}
