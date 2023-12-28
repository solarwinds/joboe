package com.solarwinds.joboe.core.util;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import com.solarwinds.joboe.core.logging.Logger;
import com.solarwinds.joboe.core.logging.LoggerFactory;

public class DaemonThreadFactory implements ThreadFactory {
    private static final Logger logger = LoggerFactory.getLogger();
    private final String threadName;
    private static final String THREAD_NAME_PREFIX = "SolarwindsAPM";
    private final AtomicInteger count = new AtomicInteger(0);

    private DaemonThreadFactory(String threadName) {
        this.threadName = threadName != null ? THREAD_NAME_PREFIX + "-" + threadName : THREAD_NAME_PREFIX;
    }

    public static DaemonThreadFactory newInstance(String threadName) {
        return new DaemonThreadFactory(threadName);
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread t = Executors.defaultThreadFactory().newThread(r);
        t.setDaemon(true);
        t.setName(threadName + "-" + count.incrementAndGet());
        try {
            //Set contextClassLoader to null to avoid memory leak error message during tomcat shutdown see http://wiki.apache.org/tomcat/MemoryLeakProtection#cclThreadSpawnedByWebApp
            //It is ok to set it to null as we do not need servlet container class loader for spawned thread as they should only reference core sdk code or classes included in the agent jar
            t.setContextClassLoader(null);
        } catch (SecurityException e) {
            logger.warn("Cannot set the context class loader of System Monitor threads to null. Tomcat might display warning message of memory leak during shutdown");
        }

        return t;
    }
}
