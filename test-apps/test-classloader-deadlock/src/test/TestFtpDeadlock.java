package test;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

/**
 * Reproduce the dead lock problem as described in https://github.com/librato/joboe/pull/595
 * 
 * In order to reproduce:
 * 1. Put a break point (do not enable it though) at sun.net.www.protocol.jar.JarFileFactory within get(URL url, boolean useCaches), inside the synchronized (instance) block (line 62 for orcale jdk 1.8.0_65)
 * 2. Put a break point in this class in the Thread.run's connnection.getContentLength()
 * 3. Put a break point in this class at the main method url.openConnection()
 * 4. Run this program with the javaagent (ensure the breakpoint 1 is disabled)
 * 5. Debugger should stop at break point 2 and 3, now enable breakpoint 1.
 * 6. Resume break point 2 (connnection.getContentLength()), it will then break again at breakpoint 1
 * 7. Resume break point 3 (url.openConnection())
 * 8. Resume everything, now there should be deadlock due to issue 595 for legacy agent 
 * 
 * 
 * @author pluk
 *
 */
public class TestFtpDeadlock {
    private static URL url = null;
    static {
        try {
            url = new URL("jar:file://dummy.jar!/");
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    public static void main(String[] args) throws Exception {
        new Thread() {
            @Override
            public void run() {
                try {
//                    URL trampolineUrl = new URL("jar:file:/C:/Program%20Files/Java/jdk1.8.0_65/jre/lib/rt.jar!/sun/reflect/misc/Trampoline.class");
                    URL trampolineUrl = new URL("jar:file:/" + System.getProperty("java.home").replace('\\', '/').replace(" ", "%20") + "/lib/rt.jar!/sun/reflect/misc/Trampoline.class");
                    URLConnection connection = trampolineUrl.openConnection();
                    
                    connection.getContentLength();
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                
            }
        }.start();

        url.openConnection();
    }
}
