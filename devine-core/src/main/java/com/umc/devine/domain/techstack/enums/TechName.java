package com.umc.devine.domain.techstack.enums;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum TechName {
    // 포지션 (Root)
    BACKEND, FRONTEND, INFRA,

    // Frontend - 언어/프레임워크
    JAVASCRIPT, TYPESCRIPT, REACT, VUEJS, NEXTJS, SVELTE,

    // Frontend - 모바일
    REACT_NATIVE, FLUTTER, KOTLIN, SWIFT,

    // Backend - 언어
    JAVA, PYTHON, GO, C, PHP,

    // Backend - 프레임워크
    SPRINGBOOT, NODEJS, EXPRESS, NESTJS, DJANGO,

    // Backend - 데이터베이스
    MONGODB, MYSQL,

    // Infra - 클라우드
    AWS, FIREBASE,

    // Infra - 컨테이너
    DOCKER, KUBERNETES;

    private static final Map<String, TechName> LOOKUP =
            Arrays.stream(values()).collect(Collectors.toMap(Enum::name, Function.identity()));

    /** 이름으로 TechName을 찾는다. 매칭되는 값이 없거나 null이면 빈 Optional. (valueOf와 달리 예외를 던지지 않음) */
    public static Optional<TechName> from(String name) {
        return Optional.ofNullable(LOOKUP.get(name));
    }
}