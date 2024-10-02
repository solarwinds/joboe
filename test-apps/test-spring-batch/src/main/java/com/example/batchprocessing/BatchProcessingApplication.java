package com.example.batchprocessing;

import com.appoptics.api.ext.AgentChecker;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.concurrent.TimeUnit;

@SpringBootApplication
public class BatchProcessingApplication {

	public static void main(String[] args) throws Exception {
		AgentChecker.waitUntilAgentReady(10, TimeUnit.SECONDS);
		SpringApplication.run(BatchProcessingApplication.class, args);
	}
}
