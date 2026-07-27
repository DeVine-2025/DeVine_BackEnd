package com.umc.devine.admin.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;

public class AdminProjectResDTO {

    @Builder
    @Schema(description = "관리자 프로젝트 목록 항목")
    public record ProjectSummaryDTO(
            @Schema(description = "프로젝트 ID", example = "1")
            Long projectId,

            @Schema(description = "프로젝트 제목", example = "쇼핑몰 프로젝트")
            String title,

            @Schema(description = "글 작성자 닉네임", example = "devine_pm")
            String authorNickname,

            @Schema(description = "등록일")
            LocalDateTime createdAt,

            @Schema(description = "노출 상태 (true=노출, false=비노출)", example = "true")
            boolean visible
    ) {}

    @Builder
    @Schema(description = "프로젝트 노출 상태 변경 응답")
    public record UpdateVisibilityRes(
            @Schema(description = "프로젝트 ID", example = "1")
            Long projectId,

            @Schema(description = "변경된 노출 상태 (true=노출, false=비노출)", example = "false")
            boolean visible,

            @Schema(description = "노출 상태가 실제로 바뀌었는지 여부. false면 이미 동일한 상태였음(멱등 호출)", example = "true")
            boolean changed,

            @Schema(description = "처리자 회원 ID. 로그인 세션이 없으면 null", nullable = true)
            Long processorMemberId,

            @Schema(description = "처리 시각")
            LocalDateTime changedAt
    ) {}
}
