package com.tracelytics.agent;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.URL;
import java.net.URLDecoder;
import java.util.jar.JarFile;

/**
 * A lean Agent that append the agent jar to bootstrap class loader search,
 * such that code injected to classes loaded by bootstrap class loader
 * (usually core jdk classes such as `ThreadPoolExecutor`) could load classes from the jar.  
 * 
 * This agent then delegates the rest of the processing to the actual {@link com.tracelytics.agent.Agent}
 * 
 * In order to avoid class loading conflict as documented in https://github.com/librato/joboe/pull/730,
 * this class should not have interactions with other agent classes
 * 
 * @author pluk
 *
 */
public class BootstrapAgent {
    //do NOT use our Logger here, otherwise this will be loaded by the app loader since this executes before appendToBootstrapClassLoaderSearch
    //this will become a problem for instrumentation that is loaded by the same app loader too as they will then use
    //Logger from app loader too and will have problem calling other classes that accept any argument of Logger as
    //those other classes are likely loaded by bootstrap loader so the definition of Logger (loaded by bootstrap)
    //is different than this Logger's app loader
    //private static Logger logger = LoggerFactory.getLogger();

    public static void premain(String agentArgs, Instrumentation inst) {
        appendToBootstrapLoader(inst);
        Agent.premain(agentArgs, inst);
    }
    
    private static boolean appendToBootstrapLoader(Instrumentation inst) {
        File agentJarPath = null;
        try {
            //agentJarPath = ResourceDirectory.getAgentJarPath(); //Do not call agent's class, otherwise might run into the same Logger cl problem
            agentJarPath = getAgentJarPath();
            if (agentJarPath == null) {
                System.err.println("Failed to find agent jar path");
                return false;
            }
            JarFile agentJarFile = new JarFile(agentJarPath);

            //Append to bootstrap so all classloader can load the agent files
            //Take note that there are files in the com.appoptics.apploader package that should NOT be loaded by boostrap loader
            //as classes within that package would reference framework classes that are only available in higher level
            //app classloader. Those class files are placed within the `apploader` folder (instead of root) within the agent jar therefore would
            //NOT be loaded by the bootstrap classloader
            inst.appendToBootstrapClassLoaderSearch(agentJarFile);
            return true;
        } catch (NoSuchMethodError e) {
            System.out.println("Running on java 1.5 or earlier, please append -Xbootclasspath/a:<agent jar location> to JVM arguments");
        } catch (Throwable e) {
            System.err.println("Failed to append agent jar path " + agentJarPath.getAbsolutePath() + " to bootstrap class loader");
            e.printStackTrace(System.err);
        }
        
        return false;
    }

    private static File getAgentJarPath() {
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
                    e.printStackTrace();
                } //in case there's space or url encoded character

            } else { //likely from testing code with access to compiled classes as well
                System.out.println("Failed to extract agent jar from classPhysicalUrl: " + classPhysicalUrl + ", try to get it from runtime instead");
                jarPath = getAgentJarPathFromRuntime();
            }
        }

        if (jarPath != null) {
            return new File(jarPath); //do not attempt to parse it directly to ensure OS independent
        } else {
            System.err.println("Failed to read jar agent location! classPhysicalUrl : " + classPhysicalUrl);
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

                System.out.println("Extracted agent jar location [" + agentValue + "] from runtime");
                return agentValue;
            }
        }

        System.err.println("Failed to extract agent jar location from runtime : " + runtimeMXBean.getInputArguments());
        return null;
    }

}
