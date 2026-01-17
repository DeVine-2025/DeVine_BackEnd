package com.umc.devine.support;

import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Set;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Transactional
public abstract class IntegrationTestSupport {

    @Container
    @ServiceConnection
    protected static final PostgreSQLContainer<?> POSTGRES_CONTAINER =
        new PostgreSQLContainer<>("pgvector/pgvector:pg17")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true); // 로컬 개발 시 컨테이너 재사용

    @Container
    protected static final GenericContainer<?> VALKEY_CONTAINER =
        new GenericContainer<>("valkey/valkey:9-alpine")
            .withExposedPorts(6379)
            .withReuse(true); // 로컬 개발 시 컨테이너 재사용

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        // Valkey 컨테이너의 동적 포트를 Spring Boot의 Redis 설정에 주입
        registry.add("spring.data.redis.host", VALKEY_CONTAINER::getHost);
        registry.add("spring.data.redis.port", () -> VALKEY_CONTAINER.getMappedPort(6379));
        registry.add("spring.data.redis.password", () -> ""); // 테스트 환경에서는 비밀번호 없음
    }

    @AfterEach
    void cleanupRedis() {
        // 각 테스트 후 Redis의 모든 키를 정리하여 테스트 격리 보장
        if (redisTemplate != null) {
            Set<String> keys = redisTemplate.keys("*");
            if (!keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        }
    }
}
