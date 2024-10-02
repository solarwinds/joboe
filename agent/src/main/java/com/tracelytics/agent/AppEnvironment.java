package com.tracelytics.agent;

public class AppEnvironment
{
    public static final String DEFAULT_APP_SERVER_NAME = "java";
    /* Determine application server name (tomcat, jboss, etc.) based on the runtime environment.
           http://stackoverflow.com/questions/4234633/determine-that-application-is-running-under-application-server
           http://stackoverflow.com/questions/673446/can-java-code-tell-if-it-is-in-an-app-server

            Tomcat:  http://petersnotes.blogspot.com/2009/04/tomcats-catalina-base-and-catalina-home.html
            JBoss: https://community.jboss.org/wiki/JBossProperties

     */
    public static String getAppServerName() {

        if (hasProperty("jboss.home.dir") || hasProperty("jboss.server.default.config") || hasProperty("org.jboss.resolver.warning") || hasProperty("org.jboss.boot.log.file") || hasProperty("jboss.modules.system.pkgs"))  {
            return "jboss";
        } else if (propertyContains("java.endorsed.dirs", "jboss")) {
            return "jboss"; // detects jboss 5
        } else if (hasProperty("com.sun.aas.instanceRoot") || hasProperty("com.sun.aas.installRoot")) {
            return "glassfish";
        } else if(hasProperty("catalina.home")) {
            return "tomcat";
        } else if (hasProperty("jetty.home")) {
            return "jetty";
        } else if (hasProperty("resin.home") || hasProperty("resin.root")) {
            return "resin";
        }

        // XXX: Figure out other app servers here
        return DEFAULT_APP_SERVER_NAME;
    }
    
    private static boolean hasProperty(String prop) {
        return System.getProperty(prop) != null;
    }
    
    private static boolean propertyContains(String prop, String contains) {
        String val = System.getProperty(prop);
        return (val != null && val.contains(contains));
    }
}
