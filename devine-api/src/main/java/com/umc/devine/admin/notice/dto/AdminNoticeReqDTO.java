package com.umc.devine.admin.notice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public class AdminNoticeReqDTO {

    @Schema(description = "공지사항 생성 요청")
    public record CreateNoticeReq(
            @NotBlank(message = "제목은 필수입니다.")
            @Size(max = 100, message = "제목은 100자를 넘을 수 없습니다.")
            @Schema(description = "공지 제목", example = "서비스 점검 안내")
            String title,

            @NotBlank(message = "내용은 필수입니다.")
            @Schema(description = "공지 본문", example = "7월 30일 02:00~04:00 서비스 점검이 진행됩니다.")
            String content,

            @Schema(description = "게시 시작 일시. 미지정 시 시작 제한 없음", example = "2026-07-28T00:00:00", nullable = true)
            LocalDateTime displayStartAt,

            @Schema(description = "게시 종료 일시. 미지정 시 종료 제한 없음", example = "2026-07-31T23:59:59", nullable = true)
            LocalDateTime displayEndAt,

            @Schema(description = "노출 여부. 미지정 시 true", example = "true", defaultValue = "true", nullable = true)
            Boolean isExposed
    ) {}

    @Schema(description = "공지사항 수정 요청 (null인 필드는 변경하지 않음)")
    public record UpdateNoticeReq(
            @Size(max = 100, message = "제목은 100자를 넘을 수 없습니다.")
            @Schema(description = "공지 제목", nullable = true)
            String title,

            @Schema(description = "공지 본문", nullable = true)
            String content,

            @Schema(description = "게시 시작 일시", nullable = true)
            LocalDateTime displayStartAt,

            @Schema(description = "게시 종료 일시", nullable = true)
            LocalDateTime displayEndAt,

            @Schema(description = "true면 게시 기간을 모두 제거해 상시 노출로 되돌린다. (null과 '변경 안 함'을 구분하기 위한 플래그)",
                    example = "false", defaultValue = "false")
            boolean clearDisplayPeriod,

            @Schema(description = "노출 여부", nullable = true)
            Boolean isExposed
    ) {}
}
