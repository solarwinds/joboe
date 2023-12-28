package com.solarwinds.joboe.core.util;

import java.lang.management.ManagementFactory;

/**
 * Helper to extract information on the running JVM process
 * @author pluk
 *
 */
public final class JavaProcessUtils {
	private static Integer pid = null;
	private JavaProcessUtils() {
		//prevent instantiations
	}

	/**
	 * Copied from <code>Event.getPID()</code>
	 * 
	 * Retrieves PID from current Java process
	 * 
	 * @return
	 */
	public static int getPid() {
   // You'd think getting the PID would be simple: http://arhipov.blogspot.com/2011/01/java-snippet-get-pid-at-runtime.html
        if (pid == null) {
            String nameOfRunningVM = ManagementFactory.getRuntimeMXBean().getName();  
            int p = nameOfRunningVM.indexOf('@');  
            String pidStr = nameOfRunningVM.substring(0, p);
            try {
                pid = Integer.parseInt(pidStr);
            } catch(NumberFormatException ex) {
                // Shouldn't happen
                pid = -1;
            }
        }

        return pid;
	}
}
