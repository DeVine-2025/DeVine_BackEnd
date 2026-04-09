package com.umc.devine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.umc.devine")
@EnableJpaRepositories("com.umc.devine")
@EntityScan("com.umc.devine")
@EnableScheduling
public class DeVineRealtimeApplication {

    public static void main(String[] args) {
        SpringApplication.run(DeVineRealtimeApplication.class, args);
    }
}
