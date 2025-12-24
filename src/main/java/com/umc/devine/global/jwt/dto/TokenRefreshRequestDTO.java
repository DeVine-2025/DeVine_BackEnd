package com.umc.devine.global.jwt.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenRefreshRequestDTO {
    @NotBlank(message = "리프레쉬 토큰은 필수입니다")
    private String refreshToken;
}

