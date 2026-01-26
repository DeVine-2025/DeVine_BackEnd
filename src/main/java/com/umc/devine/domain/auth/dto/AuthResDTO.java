package com.umc.devine.domain.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class AuthResDTO {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MeDTO {
        private String clerkId;
        private String email;
        private Long memberId;
        private Boolean isRegistered;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HealthDTO {
        private String status;
        private String message;
    }
}
