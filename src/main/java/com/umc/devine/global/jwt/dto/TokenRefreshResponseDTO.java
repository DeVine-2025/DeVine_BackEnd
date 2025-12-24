package com.umc.devine.global.jwt.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenRefreshResponseDTO {
    private String accessToken;
    private String refreshToken;
    private Long expiresIn; // 액세스 토큰 만료 시간 (초)
}

