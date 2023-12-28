package com.solarwinds.joboe.metrics.framework;

import com.solarwinds.joboe.core.logging.Logger;
import com.solarwinds.joboe.core.logging.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.ProtectionDomain;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

/**
 * Extracts and reports framework information by locating the jar used by a package. 
 *
 * We are using the MANIFEST.MF within the jar for framework information. Take note that if the information in the MANIFEST does not comply to the spec in 
 * <a href="http://docs.oracle.com/javase/7/docs/technotes/guides/jar/jar.html">...</a>
 * , then we will not report the framework
 *
 * @author Patson Luk
 *
 */
public class FrameworkRecorder {
    private static final Logger logger = LoggerFactory.getLogger();
    private static final VersionExtractor[] EXTRACTORS = new VersionExtractor[] { new BundleVersionExtractor(), new ImplementationVersionExtractor(), new SpecificationVersionExtractor() };
    private static final Set<FrameworkInfo> pendingFrameworks = new HashSet<FrameworkInfo>();
    private static final Map<URL, Boolean> processedSources = Collections.synchronizedMap(new WeakHashMap()); //synchronize it as it might hang indefinitely sometimes on docker instance, could be related to https://bugs.openjdk.java.net/browse/JDK-8075006
    
    /**
     * Extract and report the framework by reading the source jar of this ctClass
     * @param protectionDomain
     */
    public static void reportFramework(ProtectionDomain protectionDomain, ClassLoader classLoader, String packageName) {
        //skip core java classes as it might introduce deadlock, also the java version should have already been reported in init message
        if (packageName != null && (packageName.startsWith("java.") || packageName.startsWith("javax.")  || packageName.startsWith("sun."))) {
            return;
        }

        if (protectionDomain != null) {
            if (protectionDomain.getCodeSource() != null) {
                URL location = protectionDomain.getCodeSource().getLocation();
                if (location != null && !processedSources.containsKey(location)) {
                    processedSources.put(location, true);
                    FrameworkInfo frameworkInfo = extractFrameworkInfoFromSource(location);

                    if (frameworkInfo == null && packageName != null) {
                        logger.debug("Failed to extract framework info from source " + location + " try to load framework info using Package instead " + packageName);
                        frameworkInfo = extractFrameworkInfoFromPackage(classLoader, packageName);
                    }

                    if (frameworkInfo != null) {
                        logger.debug("Found framework info " + frameworkInfo + " for " + location);
                        pendingFrameworks.add(frameworkInfo);
                    } else {
                        logger.debug("Cannot find valid manifest info from the source file [" + location + "]");
                    }
                }
            }
        }
    }

    private static FrameworkInfo extractFrameworkInfoFromSource(URL sourceLocation) {
        InputStream sourceStream = null;
        try {
            sourceStream = sourceLocation.openStream();
            if (sourceStream != null) { //source stream could be null, for example file:/C:/apache-tomcat-9.0.30/webapps/test_jdbc_war/WEB-INF/classes/
                try (JarInputStream jarInputStream = new JarInputStream(sourceStream)) {
                    logger.debug("Extracting framework info from cp entry " + sourceLocation);
                    Manifest manifest = jarInputStream.getManifest();

                    if (manifest != null) {
                        Attributes attributes = manifest.getMainAttributes();
                        FrameworkInfo frameworkInfo = extractInfoFromAttributes(attributes);
                        if (frameworkInfo != null) {
                            logger.debug("Framework info extracted from [" + sourceLocation + "] : " + frameworkInfo);
                        } else {
                            logger.debug("No framework info extracted from [" + sourceLocation + "]");
                        }
                        return frameworkInfo;
                    }
                }
            }
        } catch (IOException e) {
            logger.debug("Source file entry [" + sourceLocation + "] is not a valid jar, skipping...");
        } catch (Throwable e) {
            logger.warn("Unexpected exception while parsing source file [" + sourceLocation + "] for framework versions, the entry would be skipped : " + e.getMessage(), e);
        } finally {
            if (sourceStream != null) {
                try {
                    sourceStream.close();
                } catch (IOException e) {
                    logger.warn("Failed to close source stream after framework extraction: " + e.getMessage(), e);
                }
            }
        }
        return null;
    }

    /**
     * Extracts framework info by reading java.lang.Package from the ClassLoader
     * @param loader
     * @param packageName
     * @return a FrameworkInfo object if found valid info, otherwise returns null
     */
    static FrameworkInfo extractFrameworkInfoFromPackage(ClassLoader loader, String packageName) {
        Package packageInfo = new PackageClassLoader(loader).getPackage(packageName);
        if (packageInfo != null) {
            if (packageInfo.getImplementationTitle() != null) { //have to at least have title
                return new FrameworkInfo(packageInfo.getImplementationTitle(), packageInfo.getImplementationVersion());
            } else if (packageInfo.getImplementationVendor() != null) { //if implementation title is not found, use vendor name as title (this works for Jetty)
                return new FrameworkInfo(packageInfo.getImplementationVendor(), packageInfo.getImplementationVersion());
            } else if (packageInfo.getSpecificationTitle() != null) {
                return new FrameworkInfo(packageInfo.getSpecificationTitle(), packageInfo.getSpecificationVersion());
            } else if (packageInfo.getSpecificationVendor() != null) {
                return new FrameworkInfo(packageInfo.getSpecificationVendor(), packageInfo.getSpecificationVersion());
            }
        }
        return null;
    }

    /**
     * A classloader that exposes the getPackage method of its parent loader.
     * @author Patson Luk
     *
     */
    private static class PackageClassLoader extends URLClassLoader {
        public PackageClassLoader(ClassLoader parent) {
            super(new URL[0], parent);
        }

        @Override
        protected Package getPackage(String packageName) {
            return super.getPackage(packageName);
        }
    }

    static FrameworkInfo extractInfoFromAttributes(Attributes attributes) {
        for (VersionExtractor extractor : EXTRACTORS) {
            FrameworkInfo frameworkInfo = extractor.extract(attributes);
            if (frameworkInfo != null) {
                return frameworkInfo;
            }
        }
        return null;
    }


    /**
     * Get clone of the list of pending frameworks, this would also clear the existing pending frameworks
     * @return
     */
    public static Set<FrameworkInfo> consumePendingFrameworks() {
        HashSet<FrameworkInfo> clonedSet =  new HashSet<FrameworkInfo>(pendingFrameworks);
        pendingFrameworks.clear();

        return clonedSet;
    }

    private static abstract class VersionExtractor {
        private final String[] KEYS = getIdentityKeys();

        FrameworkInfo extract(Attributes attributes) {
            String version = attributes.getValue(getVersionKey());

            if (version != null) {
                for (String key : KEYS) {
                    String value = attributes.getValue(key);
                    if (value != null) {
                        logger.debug("Found [" + key + "] with value [" + value + "] while extracting framework version [" + version + "] from " + getClass().getSimpleName());
                        return new FrameworkInfo(value, version);
                    }
                }
            }
            return null;
        }

        protected abstract String[] getIdentityKeys();

        protected abstract String getVersionKey();
    }

    private static class BundleVersionExtractor extends VersionExtractor {
        @Override
        protected String[] getIdentityKeys() {
            return new String[]{"Bundle-SymbolicName", "Bundle-Name", "Bundle-Vendor"};
        }

        @Override
        protected String getVersionKey() {
            return "Bundle-Version";
        }
    }

    private static class ImplementationVersionExtractor extends VersionExtractor {
        @Override
        protected String[] getIdentityKeys() {
            return new String[]{"Implementation-Vendor-Id", "Implementation-Title", "Implementation-Vendor"};
        }

        @Override
        protected String getVersionKey() {
            return "Implementation-Version";
        }
    }

    private static class SpecificationVersionExtractor extends VersionExtractor {
        @Override
        protected String[] getIdentityKeys() {
            return new String[]{"Specification-Title", "Specification-Vendor"};
        }

        @Override
        protected String getVersionKey() {
            return "Specification-Version";
        }
    }

    @lombok.Getter
    public static class FrameworkInfo {
        private final String id;
        public FrameworkInfo(String id, String version) {
            this.id = id;
            this.version = version;
        }
        private final String version;


        @Override
        public String toString() {
            return (id != null ? id : "unknown") + (version != null ? (" | version : [" + version + "]") : "");
        }
    }
}
