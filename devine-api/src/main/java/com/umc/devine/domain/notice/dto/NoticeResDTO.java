package com.umc.devine.domain.notice.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public class NoticeResDTO {

    @Schema(description = "공지사항 목록 항목 (본문 제외)")
    public record NoticeSummaryDTO(
            Long noticeId,
            String title,
            LocalDateTime createdAt
    ) {}

    @Schema(description = "공지사항 상세")
    public record NoticeDetailDTO(
            Long noticeId,
            String title,
            String content,
            LocalDateTime createdAt
    ) {}
}
