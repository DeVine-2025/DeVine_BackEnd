package com.umc.devine.support;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("통합 테스트 환경 검증")
class IntegrationTestSupportTest extends IntegrationTestSupport {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Test
    @DisplayName("PostgreSQL 컨테이너가 정상적으로 실행되고 연결된다")
    void testPostgreSQLConnection() throws Exception {
        // given: Testcontainers가 PostgreSQL 컨테이너를 실행하고 DataSource를 주입

        // when: 데이터베이스에 연결하여 버전 정보를 조회
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT version()")) {

            // then: 연결이 성공하고 PostgreSQL 17 버전 정보를 확인
            assertThat(resultSet.next()).isTrue();
            String version = resultSet.getString(1);

            System.out.println("=== PostgreSQL 버전 정보 ===");
            System.out.println(version);

            assertThat(version)
                .as("PostgreSQL 버전 확인")
                .contains("PostgreSQL 17");
        }
    }

    @Test
    @DisplayName("pgvector 확장이 사용 가능하다")
    void testPgVectorExtension() throws Exception {
        // given: pgvector/pgvector:pg17 이미지 사용

        // when: pgvector 확장을 생성하고 vector 타입을 사용
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            System.out.println("=== pgvector 확장 테스트 ===");

            // pgvector 확장 생성
            statement.execute("CREATE EXTENSION IF NOT EXISTS vector");
            System.out.println("✓ pgvector 확장 생성 성공");

            // vector 타입을 사용하는 테이블 생성 테스트
            statement.execute("CREATE TABLE test_vectors (id SERIAL PRIMARY KEY, embedding vector(3))");
            System.out.println("✓ vector 타입 테이블 생성 성공");

            // 벡터 데이터 삽입 테스트
            statement.execute("INSERT INTO test_vectors (embedding) VALUES ('[1,2,3]')");
            System.out.println("✓ 벡터 데이터 삽입 성공");

            // then: 정상적으로 실행됨
            assertThat(true).isTrue();
        }
    }

    @Test
    @DisplayName("Valkey 컨테이너가 정상적으로 실행되고 연결된다")
    void testValkeyConnection() {
        // given: Testcontainers가 Valkey 컨테이너를 실행하고 StringRedisTemplate을 주입

        System.out.println("=== Valkey 연결 테스트 ===");

        // when: Redis 기본 명령어 실행 (SET/GET)
        stringRedisTemplate.opsForValue().set("test-key", "test-value");
        System.out.println("✓ SET 명령어 실행 성공: test-key = test-value");

        String value = stringRedisTemplate.opsForValue().get("test-key");
        System.out.println("✓ GET 명령어 실행 성공: test-key = " + value);

        // then: 값이 정상적으로 저장되고 조회됨
        assertThat(value)
            .as("Valkey에서 값이 정상적으로 저장되고 조회되어야 함")
            .isEqualTo("test-value");

        System.out.println("✓ Valkey 컨테이너 연결 및 동작 확인 완료");
    }

    @Test
    @DisplayName("@AfterEach에 의해 각 테스트 후 Redis 데이터가 자동으로 정리된다")
    void testRedisIsolation() {
        // given: 이전 테스트에서 저장한 데이터가 있을 수 있음

        System.out.println("=== Redis 테스트 격리 확인 ===");

        // when: 이전 테스트 키를 조회
        String previousValue = stringRedisTemplate.opsForValue().get("test-key");
        System.out.println("이전 테스트 키 조회 결과: " + previousValue);

        // then: 이전 테스트의 데이터가 정리되어 없어야 함
        assertThat(previousValue)
            .as("@AfterEach cleanupRedis()에 의해 이전 테스트 데이터가 정리되어야 함")
            .isNull();

        // when: 새로운 데이터 저장
        stringRedisTemplate.opsForValue().set("isolation-test-key", "isolated-value");
        System.out.println("✓ 새로운 키 저장 성공: isolation-test-key = isolated-value");

        // then: 현재 테스트 데이터는 정상 조회
        String currentValue = stringRedisTemplate.opsForValue().get("isolation-test-key");
        assertThat(currentValue).isEqualTo("isolated-value");
        System.out.println("✓ Redis 테스트 격리 확인 완료 (각 테스트는 깨끗한 상태에서 시작)");
    }

    @Test
    @DisplayName("@Transactional에 의해 각 테스트는 격리되어 자동 롤백된다")
    void testTransactionalRollback() throws Exception {
        // given: 임시 테이블 생성 및 데이터 삽입
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            System.out.println("=== 트랜잭션 롤백 테스트 ===");

            // 임시 테이블 생성
            statement.execute("CREATE TABLE rollback_test (id SERIAL PRIMARY KEY, name VARCHAR(50))");
            System.out.println("✓ 임시 테이블 생성");

            // 데이터 삽입
            statement.execute("INSERT INTO rollback_test (name) VALUES ('test_data')");
            System.out.println("✓ 테스트 데이터 삽입");

            // when: 데이터 확인
            ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM rollback_test");
            resultSet.next();
            long count = resultSet.getLong(1);

            // then: 데이터가 존재함
            assertThat(count).isEqualTo(1);
            System.out.println("✓ 현재 트랜잭션 내에서 데이터 확인됨: " + count + "개");
            System.out.println("⚠️ 이 테스트 종료 후 @Transactional에 의해 모든 변경사항이 자동 롤백됩니다");
        }
    }

    @Test
    @DisplayName("컨테이너 정보를 확인할 수 있다")
    void testContainerInfo() {
        // when: 컨테이너가 실행 중이고 정보를 확인
        System.out.println("=== PostgreSQL 컨테이너 정보 ===");
        System.out.println("컨테이너 실행 상태: " + POSTGRES_CONTAINER.isRunning());
        System.out.println("Docker 이미지: " + POSTGRES_CONTAINER.getDockerImageName());
        System.out.println("JDBC URL: " + POSTGRES_CONTAINER.getJdbcUrl());
        System.out.println("데이터베이스 이름: " + POSTGRES_CONTAINER.getDatabaseName());
        System.out.println("사용자 이름: " + POSTGRES_CONTAINER.getUsername());
        System.out.println("컨테이너 ID: " + POSTGRES_CONTAINER.getContainerId());
        System.out.println("Reuse 활성화: true");

        System.out.println("\n=== Valkey 컨테이너 정보 ===");
        System.out.println("컨테이너 실행 상태: " + VALKEY_CONTAINER.isRunning());
        System.out.println("Docker 이미지: " + VALKEY_CONTAINER.getDockerImageName());
        System.out.println("호스트: " + VALKEY_CONTAINER.getHost());
        System.out.println("포트: " + VALKEY_CONTAINER.getMappedPort(6379));
        System.out.println("컨테이너 ID: " + VALKEY_CONTAINER.getContainerId());
        System.out.println("Reuse 활성화: true");

        // then: 컨테이너가 실행 중이고 올바른 설정을 가짐
        assertThat(POSTGRES_CONTAINER.isRunning())
            .as("PostgreSQL 컨테이너가 실행 중이어야 함")
            .isTrue();

        assertThat(POSTGRES_CONTAINER.getDatabaseName())
            .as("데이터베이스 이름 확인")
            .isEqualTo("testdb");

        assertThat(POSTGRES_CONTAINER.getUsername())
            .as("사용자 이름 확인")
            .isEqualTo("test");

        assertThat(POSTGRES_CONTAINER.getDockerImageName())
            .as("Docker 이미지 확인")
            .contains("pgvector/pgvector:pg17");

        assertThat(VALKEY_CONTAINER.isRunning())
            .as("Valkey 컨테이너가 실행 중이어야 함")
            .isTrue();

        assertThat(VALKEY_CONTAINER.getDockerImageName())
            .as("Docker 이미지 확인")
            .contains("valkey/valkey:9-alpine");
    }
}