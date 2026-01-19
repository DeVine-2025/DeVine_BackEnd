package com.umc.devine.domain.techstack.enums;

public enum TechName {
    // 포지션 (Root)
    BACKEND, FRONTEND, INFRA,

    // 상세 기술
    JAVA, PYTHON, GO, C, KOTLIN, PHP, // Backend 언어
    SPRINGBOOT, NODEJS, EXPRESS, NESTJS, DJANGO, // Backend 프레임워크
    MONGODB, MYSQL, // Database
    JAVASCRIPT, TYPESCRIPT, REACT, VUEJS, NEXTJS, SVELTE, // Frontend
    REACT_NATIVE, FLUTTER, SWIFT, // Mobile
    AWS, FIREBASE, // Cloud
    DOCKER, KUBERNETES // Container
}