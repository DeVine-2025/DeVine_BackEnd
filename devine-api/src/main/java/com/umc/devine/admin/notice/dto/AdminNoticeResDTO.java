package com.umc.devine.admin.notice.dto;

import com.umc.devine.domain.notice.enums.NoticeDisplayStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public class AdminNoticeResDTO {

    @Schema(description = "공지사항 정보 (관리자용)")
    public record NoticeDTO(
            Long noticeId,
            String title,
            String content,

            @Schema(description = "게시 시작 일시 (null이면 시작 제한 없음)", nullable = true)
            LocalDateTime displayStartAt,

            @Schema(description = "게시 종료 일시 (null이면 종료 제한 없음)", nullable = true)
            LocalDateTime displayEndAt,

            @Schema(description = "관리자가 설정한 노출 여부")
            Boolean isExposed,

            @Schema(description = "조회 시점 기준 실제 노출 상태 (DB 저장값이 아닌 파생값)")
            NoticeDisplayStatus displayStatus,

            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}
}
