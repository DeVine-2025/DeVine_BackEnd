package com.umc.devine.testconfig;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Core 모듈 테스트용 Application.
 * @Configuration + @EnableAutoConfiguration을 사용하여 @SpringBootConfiguration 충돌을 방지.
 * 반드시 @SpringBootTest(classes = CoreTestApplication.class) 형태로 명시적으로 사용.
 */
@Configuration
@EnableAutoConfiguration
@ComponentScan("com.umc.devine")
@EnableJpaRepositories("com.umc.devine")
@EntityScan("com.umc.devine")
public class CoreTestApplication {
}
