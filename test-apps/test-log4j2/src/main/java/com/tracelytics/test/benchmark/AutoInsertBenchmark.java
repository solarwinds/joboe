package com.tracelytics.test.benchmark;

import com.appoptics.api.ext.AgentChecker;
import com.appoptics.api.ext.Trace;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.builder.api.AppenderComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.api.LayoutComponentBuilder;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.slf4j.MDC;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class AutoInsertBenchmark {
    private static final int CALL_COUNT = 100000;
//    private static final int CALL_COUNT = 1;
    private static final int RUN_COUNT = 100;

    public static void main(String[] args) throws IOException {
        AgentChecker.waitUntilAgentReady(10, TimeUnit.SECONDS);

        Trace.startTrace("with-trace").report();
        List<Long> mdcDurations = runTests("mdc");

        System.out.println(Trace.endTrace("with-trace"));

        Statistics<Long> mdcStats = new Statistics<Long>(mdcDurations);

        System.out.println("Median : " + mdcStats.getMedian());
        System.out.println("Mean : " + mdcStats.getMean());
    }


    private static List<Long> runTests(String name) {
        List<Long> durations = new ArrayList<Long>();
        configure(name);

        Logger logger = LogManager.getLogger(name);
        for (int i = 0; i < RUN_COUNT; i++) {
            
            Long start = System.currentTimeMillis();
            for (int j = 0; j < CALL_COUNT; j++) {
                logger.info("a log message here");
            }
            long end = System.currentTimeMillis();
            long duration = end - start;
            durations.add(duration * 1000000 / CALL_COUNT); //duration per call in nanoseconds

            if (RUN_COUNT >= 100 && i % (RUN_COUNT / 100) == 0) {
                System.out.println(i / (RUN_COUNT / 100) + " % " + name + " : " + duration);
            }
            
            MDC.put("ao.traceId", "1234" + 0);

        }
        LogManager.shutdown();
        System.gc();
        return durations;
    }


    private static void configure(String name) {
        ConfigurationBuilder<BuiltConfiguration> builder = ConfigurationBuilderFactory.newConfigurationBuilder();
        AppenderComponentBuilder appenderBuilder = builder.newAppender(name, "FILE");
        appenderBuilder.addAttribute("fileName", name);
        appenderBuilder.addAttribute("append", false);
        appenderBuilder.addAttribute("immediateFlush", false);

        LayoutComponentBuilder jsonLayout = builder.newLayout("JsonLayout").addAttribute("compact", true);
        jsonLayout.addComponent(builder.newKeyValuePair("ao.traceIi", "$${ctx:ao.traceId}"));
        appenderBuilder.add(jsonLayout);
        builder.add(appenderBuilder);

        builder.add(builder.newRootLogger(Level.INFO).add(builder.newAppenderRef(name)));
        Configurator.initialize(builder.build());
    }
}
