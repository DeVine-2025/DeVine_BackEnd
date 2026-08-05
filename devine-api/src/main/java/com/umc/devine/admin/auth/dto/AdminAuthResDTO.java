package com.umc.devine.admin.auth.dto;

import com.umc.devine.admin.enums.AdminLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class AdminAuthResDTO {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MeDTO {
        private String clerkId;
        private String email;
        private AdminLevel level;
    }
}