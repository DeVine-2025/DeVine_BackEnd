package com.umc.devine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableJpaAuditing
@SpringBootApplication
@EnableScheduling
public class DeVineApplication {

	public static void main(String[] args) {
		SpringApplication.run(DeVineApplication.class, args);
	}

}
