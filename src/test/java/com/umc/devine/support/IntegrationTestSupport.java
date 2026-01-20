package com.umc.devine.support;

import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.util.Set;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public abstract class IntegrationTestSupport {

    protected static final PostgreSQLContainer<?> POSTGRES_CONTAINER;
    protected static final GenericContainer<?> VALKEY_CONTAINER;

    static {
        POSTGRES_CONTAINER = new PostgreSQLContainer<>("pgvector/pgvector:pg17")
                .withDatabaseName("testdb")
                .withUsername("test")
                .withPassword("test")
                .withReuse(true)
                .waitingFor(Wait.forListeningPort());
        POSTGRES_CONTAINER.start();

        VALKEY_CONTAINER = new GenericContainer<>("valkey/valkey:9-alpine")
                .withExposedPorts(6379)
                .withReuse(true)
                .waitingFor(Wait.forListeningPort());
        VALKEY_CONTAINER.start();
    }

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        // PostgreSQL 컨테이너 연결 정보 주입
        registry.add("spring.datasource.url", POSTGRES_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES_CONTAINER::getUsername);
        registry.add("spring.datasource.password", POSTGRES_CONTAINER::getPassword);

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
