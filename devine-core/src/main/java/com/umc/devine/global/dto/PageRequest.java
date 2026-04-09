package com.umc.devine.global.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Pageable;

@Schema(description = "페이징 요청")
public record PageRequest(
        @Schema(description = "페이지 번호 (1부터 시작)", example = "1", defaultValue = "1")
        Integer page,

        @Schema(description = "페이지 크기", example = "10", defaultValue = "10")
        Integer size
) {
    public PageRequest {
        if (page == null || page < 1) page = 1;
        if (size == null || size < 1) size = 10;
    }

    public static PageRequest of(Integer page, Integer size) {
        return new PageRequest(page, size);
    }

    public Pageable toPageable() {
        return org.springframework.data.domain.PageRequest.of(page - 1, size);
    }
}
