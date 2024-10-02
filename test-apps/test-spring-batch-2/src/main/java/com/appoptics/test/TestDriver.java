package com.appoptics.test;

import org.springframework.batch.core.launch.support.CommandLineJobRunner;

public class TestDriver {
    public static void main(String[] args) throws Exception {
        CommandLineJobRunner.main(new String[] { "classpath:/jobs/test-job.xml", "testJob" } );
    }
}
