package com.tracelytics.agent;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.URL;
import java.net.URLDecoder;

import com.tracelytics.logging.Logger;
import com.tracelytics.logging.LoggerFactory;

public class ResourceDirectory {
    private static final Logger logger = LoggerFactory.getLogger();
	public static final String JAVA_AGENT_CONFIG_FILE = "javaagent.json";
	
	private ResourceDirectory() {} //disallow instantiation
	
	public static String getAgentDirectory() {
	    File agentJarPath = getAgentJarPath();
        return agentJarPath != null ? agentJarPath.getParent() : null;
	}
	
	public static File getAgentJarPath() {
	    String classResourcePath = "/" + ResourceDirectory.class.getName().replace('.', '/') + ".class";
        URL classPhysicalUrl = ResourceDirectory.class.getResource(classResourcePath); //cannot use getProtectionDomain approach see https://github.com/librato/joboe/pull/102#issuecomment-18584154
        
        String jarPath = null;
        if (classPhysicalUrl != null) {
            //Can use ((JarURLConnection)classPhysicalUrl.openConnection()).getJarFile().getName() , but this will trigger various classloading in very initial stage of agent, which is undesirable
             
            //file:/user/local/appoptics/appoticsagent.jar!/com/appoptics/agent/ResourceDirectory.class
            final String prefix = "file:";
            final String suffix = "!" + classResourcePath;
            String classFilePath = classPhysicalUrl.getPath();
            
            if (classFilePath != null && classFilePath.startsWith(prefix) && classFilePath.endsWith(suffix)) {
                // /user/local/appoptics/appoptics-agent.jar
                jarPath = classFilePath.substring(prefix.length(), classFilePath.indexOf(suffix));
                try {
                    jarPath = URLDecoder.decode(jarPath, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    logger.warn(e.getMessage(), e);
                } //in case there's space or url encoded character
                
            } else { //likely from testing code with access to compiled classes as well
                logger.info("Failed to extract agent jar from classPhysicalUrl: " + classPhysicalUrl + ", try to get it from runtime instead");
                jarPath = getAgentJarPathFromRuntime();
            }
        }
        
        if (jarPath != null) {
            return new File(jarPath); //do not attempt to parse it directly to ensure OS independent
        } else {
            logger.warn("Failed to read jar agent location! classPhysicalUrl : " + classPhysicalUrl);
            return null;
        }
	}
	
	private static String getAgentJarPathFromRuntime() {
	    RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
	    final String javaAgentArgumentPrefix = "-javaagent:"; 
	    for (String argument : runtimeMXBean.getInputArguments()) {
	        if (argument.startsWith(javaAgentArgumentPrefix)) {
	            String agentValue = argument.substring(javaAgentArgumentPrefix.length());
	            //strip out the agent option
	            int optionMarkerIndex = agentValue.indexOf('=');
	            if (optionMarkerIndex != -1) {
	                agentValue = agentValue.substring(0, optionMarkerIndex);
	            }
	            
	            logger.debug("Extracted agent jar location [" + agentValue + "] from runtime");
	            return agentValue;
	        }
	    }
	    
	    logger.warn("Failed to extract agent jar location from runtime : " + runtimeMXBean.getInputArguments());
        return null;
    }


    public static String getJavaAgentConfigLocation() {
		return getAgentDirectory() + File.separator + JAVA_AGENT_CONFIG_FILE;
	}
	
}
