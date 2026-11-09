package com.umc.devine.domain.member.dto;

import java.time.LocalDateTime;

public class MemberBlockResDTO {
    public record Confirmation(String nickname, long pendingCount, String notice) {}
    public record BlockedMember(String nickname, String image, LocalDateTime blockedAt) {}
}
