//package com.umc.devine.global.jwt;
//
//// TODO: User, UserRepository, TokenException, ErrorStatus, TokenResponseDTO, TokenRefreshResponseDTO 클래스 생성 후 주석 해제
//// 도메인 및 에러 처리 체계가 구축되면 아래 코드의 주석을 해제
//
///*
//import com.umc.devine.global.jwt.dto.TokenRefreshResponseDTO;
//import com.umc.devine.global.jwt.dto.TokenResponseDTO;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.data.redis.core.RedisTemplate;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.util.concurrent.TimeUnit;
//
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class TokenService {
//
//    private final JwtUtil jwtUtil;
//    private final UserRepository userRepository;
//    private final RedisTemplate<String, String> stringRedisTemplate;
//
//    /**
//     * 토큰 생성 및 Redis 저장
//     */
//    @Transactional
//    public TokenResponseDTO generateTokens(User user) {
//        // 액세스 토큰 생성
//        String accessToken = jwtUtil.generateToken(
//            user.getEmail(),
//            user.getRole().toString(),
//            user.getId()
//        );
//
//        // 리프레시 토큰 생성
//        String refreshToken = jwtUtil.generateRefreshToken(
//            user.getEmail(),
//            user.getId()
//        );
//
//        // Redis에 리프레시 토큰 저장 (기존 토큰 덮어쓰기)
//        String refreshTokenKey = RedisKeyConstants.getRefreshTokenKey(user.getId());
//        try {
//            stringRedisTemplate.opsForValue().set(
//                    refreshTokenKey,
//                    refreshToken,
//                    RedisKeyConstants.REFRESH_TOKEN_TTL,
//                    TimeUnit.SECONDS
//            );
//        } catch (Exception e) {
//            throw new TokenException(ErrorStatus.REDIS_SAVE_FAILED);
//        }
//
//        return TokenResponseDTO.builder()
//            .accessToken(accessToken)
//            .refreshToken(refreshToken)
//            .expiresIn(RedisKeyConstants.ACCESS_TOKEN_TTL)
//            .build();
//    }
//
//    /**
//     * 리프레시 토큰으로 액세스 토큰 갱신
//     */
//    @Transactional
//    public TokenRefreshResponseDTO refreshAccessToken(String refreshToken) {
//        // JWT 검증 및 예외 전환
//        if (!jwtUtil.validateToken(refreshToken)) {
//            throw new TokenException(ErrorStatus.INVALID_REFRESH_TOKEN);
//        }
//
//        // 사용자 정보 추출
//        String email = jwtUtil.extractUsername(refreshToken);
//        Long userId = jwtUtil.extractUserId(refreshToken);
//
//        // Redis에서 리프레시 토큰 조회
//        String refreshTokenKey = RedisKeyConstants.getRefreshTokenKey(userId);
//        String storedToken;
//
//        try {
//            storedToken = stringRedisTemplate.opsForValue().get(refreshTokenKey);
//        } catch (Exception e) {
//            log.error("Redis 조회 실패: {}", e.getMessage(), e);
//            throw new TokenException(ErrorStatus.REDIS_ACCESS_FAILED);
//        }
//
//        // 토큰 일치 여부 확인 및 예외 전환
//        if (storedToken == null || !refreshToken.equals(storedToken)) {
//            throw new TokenException(ErrorStatus.INVALID_REFRESH_TOKEN);
//        }
//
//        // User 정보 조회 및 예외 전환
//        User user = userRepository.findById(userId)
//                .orElseThrow(() -> new TokenException(ErrorStatus.NOT_FOUND_USER));
//
//        // 새 액세스 토큰 생성
//        String newAccessToken = jwtUtil.generateToken(
//                email,
//                user.getRole().toString(),
//                userId
//        );
//
//        log.info("토큰 갱신 완료 - 사용자: {}", email);
//
//        return TokenRefreshResponseDTO.builder()
//                .accessToken(newAccessToken)
//                .refreshToken(refreshToken)
//                .expiresIn(RedisKeyConstants.ACCESS_TOKEN_TTL)
//                .build();
//    }
//
//    /**
//     * 로그아웃 (리프레시 토큰 삭제)
//     */
//    @Transactional
//    public void logout(Long userId) {
//        String refreshTokenKey = RedisKeyConstants.getRefreshTokenKey(userId);
//        try {
//            // Redis에서 리프레시 토큰 삭제
//            stringRedisTemplate.delete(refreshTokenKey);
//            log.info("로그아웃 완료 - 사용자 ID: {}", userId);
//
//        } catch (Exception e) {
//            log.error("로그아웃 실패: {}", e.getMessage());
//            throw new TokenException(ErrorStatus.LOGOUT_FAILED);
//        }
//    }
//
//    /**
//     * 액세스 토큰을 블랙리스트에 추가
//     */
//    public void blacklistAccessToken(String accessToken) {
//        try {
//            String blacklistKey = RedisKeyConstants.getBlacklistTokenKey(accessToken);
//            stringRedisTemplate.opsForValue().set(
//                blacklistKey,
//                "true",
//                RedisKeyConstants.BLACKLIST_TOKEN_TTL,
//                TimeUnit.SECONDS
//            );
//        } catch (Exception e) {
//            throw new TokenException(ErrorStatus.LOGOUT_FAILED);
//        }
//    }
//
//    /**
//     * 액세스 토큰이 블랙리스트에 있는지 확인
//     */
//    public boolean isTokenBlacklisted(String accessToken) {
//        try {
//            String blacklistKey = RedisKeyConstants.getBlacklistTokenKey(accessToken);
//            return Boolean.TRUE.equals(stringRedisTemplate.hasKey(blacklistKey));
//        } catch (Exception e) {
//            log.error("블랙리스트 확인 실패: {}", e.getMessage());
//            return false;
//        }
//    }
//}
//*/
