package com.example.SmartLearningPlatformBackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class SmartLearningPlatformBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(SmartLearningPlatformBackendApplication.class, args);
	}

}
