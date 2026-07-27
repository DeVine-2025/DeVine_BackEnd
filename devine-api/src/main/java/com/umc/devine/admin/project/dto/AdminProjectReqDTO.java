package com.umc.devine.admin.project.dto;

import com.umc.devine.global.dto.PageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import org.springframework.data.domain.Pageable;

public class AdminProjectReqDTO {

    @Builder
    @Schema(description = "관리자 프로젝트 목록 조회 요청")
    public record SearchReq(
            @Schema(description = "노출 상태 필터 (true=노출 중만, false=비노출만). 미지정 시 전체 조회", nullable = true)
            Boolean visible,

            @Schema(description = "페이지 번호 (1부터 시작)", example = "1", defaultValue = "1")
            @Min(value = 1, message = "페이지 번호는 1 이상이어야 합니다.")
            Integer page,

            @Schema(description = "페이지 크기", example = "10", defaultValue = "10")
            @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
            @Max(value = 100, message = "페이지 크기는 100 이하여야 합니다.")
            Integer size
    ) {
        public SearchReq {
            if (page == null) page = 1;
            if (size == null) size = 10;
        }

        public Pageable toPageable() {
            return PageRequest.of(page, size).toPageable();
        }

        /** 리포지토리는 hidden 기준으로 조회하므로 노출 필터를 뒤집어 전달한다. 미지정이면 null(전체). */
        public Boolean toHiddenFilter() {
            return visible == null ? null : !visible;
        }
    }

    @Builder
    @Schema(description = "프로젝트 노출 상태 변경 요청")
    public record UpdateVisibilityReq(
            @Schema(description = "변경할 노출 상태 (true=노출, false=비노출)", example = "false")
            @NotNull(message = "노출 상태는 필수입니다.")
            Boolean visible
    ) {}
}
