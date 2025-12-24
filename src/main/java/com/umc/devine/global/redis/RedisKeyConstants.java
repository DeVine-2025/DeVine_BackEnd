package com.umc.devine.global.redis;

public class RedisKeyConstants {

    // 리프레쉬 토큰 관련
    public static final String REFRESH_TOKEN_PREFIX = "RT:";

    // 액세스 토큰 블랙리스트
    public static final String BLACKLIST_TOKEN_PREFIX = "blacklist_token:";

    // TTL 설정 (초)
    public static final long REFRESH_TOKEN_TTL = 604800L; // 7일
    public static final long ACCESS_TOKEN_TTL = 3600L;     // 60분
    public static final long BLACKLIST_TOKEN_TTL = 3600L;  // 60분 (액세스 토큰과 동일)

    // 키 생성 헬퍼 메서드
    public static String getRefreshTokenKey(Long userId) {
        return REFRESH_TOKEN_PREFIX + userId;
    }

    public static String getBlacklistTokenKey(String accessToken) {
        return BLACKLIST_TOKEN_PREFIX + accessToken;
    }
}

