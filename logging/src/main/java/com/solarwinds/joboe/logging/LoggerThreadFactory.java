package com.solarwinds.joboe.logging;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class LoggerThreadFactory implements ThreadFactory {
    private final String threadName;
    private static final Logger logger = LoggerFactory.getLogger();
    private final AtomicInteger count = new AtomicInteger(0);
    private final ThreadFactory threadFactory = Executors.defaultThreadFactory();

    private LoggerThreadFactory(String threadName) {
        String THREAD_NAME_PREFIX = "SolarwindsAPM";
        this.threadName = threadName != null ? THREAD_NAME_PREFIX + "-" + threadName : THREAD_NAME_PREFIX;
    }

    public static LoggerThreadFactory newInstance(String threadName) {
        return new LoggerThreadFactory(threadName);
    }

    @Override
    public Thread newThread(Runnable runnable) {
        Thread thread = threadFactory.newThread(runnable);
        thread.setDaemon(true);
        thread.setName(threadName + "-" + count.incrementAndGet());

        try {
            //Set contextClassLoader to null to avoid memory leak error message during tomcat shutdown see http://wiki.apache.org/tomcat/MemoryLeakProtection#cclThreadSpawnedByWebApp
            //It is ok to set it to null as we do not need servlet container class loader for spawned thread as they should only reference core sdk code or classes included in the agent jar
            thread.setContextClassLoader(null);
        } catch (SecurityException e) {
            logger.warn("Cannot set the context class loader of System Monitor threads to null. Tomcat might display warning message of memory leak during shutdown");
        }

        return thread;
    }
}
