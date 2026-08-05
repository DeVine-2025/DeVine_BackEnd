package com.umc.devine.admin.integration.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 상태 점검 대상 외부 연동.
 * 상수를 추가할 때는 external_integration_health 테이블의 CHECK 제약도 함께 확장해야 한다.
 */
@Getter
@AllArgsConstructor
public enum IntegrationType {

    CLERK_API("Clerk API"),
    CLERK_JWKS("Clerk JWKS"),
    GITHUB_API("GitHub API"),
    FASTAPI_AI("FastAPI AI 서버"),
    GEMINI("Gemini"),
    OPENAI("OpenAI"),
    PORTONE("PortOne"),
    ;

    private final String displayName;
}