package com.tracelytics.agent;

import com.tracelytics.logging.Logger;
import com.tracelytics.logging.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Loads the class resource of package `com.appoptics.apploader`
 *
 * Class resources of that package has been renamed to have `.classbytes` extension instead of `class`
 * to avoid the default bootstrap loader from loading it.
 *
 * This loader simply replace the `.class` file extension with `.classbytes`
 */
class AppLoaderClassLoader extends URLClassLoader {
    private static final String appLoaderResourcePrefix = "com/appoptics/apploader/";
    private final Map<String, List<Path>> packageResources;
    private static final String CLASSBYTES_EXTENSION = ".classbytes";
    private static final String CLASS_EXTENSION = ".class";
    private static final Logger logger = LoggerFactory.getLogger();

    AppLoaderClassLoader(File agentJar) throws MalformedURLException {
        super(new URL[] { agentJar.toURI().toURL() } );
        packageResources = locatePackageResources();
    }

    private Map<String, List<Path>> locatePackageResources() {
        Map<String, List<Path>> holder = new HashMap<String, List<Path>>();
        try {
            FileSystem fileSystem = FileSystems.newFileSystem(super.getResource(appLoaderResourcePrefix).toURI(), Collections.EMPTY_MAP);
            locatePackageResources(holder, fileSystem.getPath(appLoaderResourcePrefix));
        } catch (Exception e) {
            logger.warn("Failed to load package resource : " + e.getMessage(), e);
        }
        return holder;
    }

    private void locatePackageResources(final Map<String, List<Path>> holder, final Path currentDirectory) {
        final List<Path> filesInThisDirectory = new ArrayList<Path>();
        try {
            for (Path childPath : Files.newDirectoryStream(currentDirectory)) {
                String pathString = childPath.toString();
                if (pathString.endsWith(CLASSBYTES_EXTENSION)) {
                    filesInThisDirectory.add(childPath);
                } else {
                    locatePackageResources(holder, childPath);
                }

            }

        } catch (IOException e) {
            logger.warn("Failed to load package resource : " + e.getMessage(), e);
        }

        if (!filesInThisDirectory.isEmpty()) {
            holder.put(getPackageNameFromPath(currentDirectory), filesInThisDirectory);
        }
    }

    private static String getPackageNameFromPath(Path path) {
        String packageName = path.toString();
        if (packageName.startsWith("/")) {
            packageName = packageName.substring(1);
        }
        if (packageName.endsWith("/")) {
            packageName = packageName.substring(0, packageName.length() - 1);
        }
        packageName = packageName.replace('/', '.');
        return packageName;
    }

    private static String getClassNameFromPath(Path path) {
        String className = path.toString();
        if (className.endsWith(CLASSBYTES_EXTENSION)) {
            if (className.startsWith("/")) {
                className = className.substring(1);
            }
            className = className.substring(0, className.length() - CLASSBYTES_EXTENSION.length());
            className = className.replace('/', '.');
            return className;
        } else {
            logger.warn("Unexpected app loader class resource " + path + " does not end with " + CLASSBYTES_EXTENSION);
            return null;
        }
    }


    @Override
    public URL getResource(String name) {
        if (name.startsWith(appLoaderResourcePrefix) && name.endsWith(CLASS_EXTENSION) ) {
            String realName = name.substring(0, name.length() - CLASS_EXTENSION.length()) + CLASSBYTES_EXTENSION;
            return super.getResource(realName);
        } else {
            return null;
        }
    }

    public Map<String, byte[]> getPackageClasses(String packageName) {
        if (!packageResources.containsKey(packageName)) {
            return Collections.emptyMap();
        }
        Map<String, byte[]> result = new HashMap<String, byte[]>();

        for (Path classResourcePath : packageResources.get(packageName)) {
            try {
                String className = getClassNameFromPath(classResourcePath);
                if (className != null) {
                    InputStream stream = Files.newInputStream(classResourcePath);
                    byte[] classBytes = com.tracelytics.ext.google.common.io.ByteStreams.toByteArray(stream);
                    result.put(className, classBytes);
                }
            } catch (IOException e) {
                logger.warn("Failed to load app loader class resource " + classResourcePath + " : " + e.getMessage(), e);
            }
        }

        return result;
    }


}
