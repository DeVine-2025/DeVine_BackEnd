package com.umc.devine.global.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@Builder
@Schema(description = "페이징 응답")
public class PagedResponse<T> {

    @Schema(description = "콘텐츠 리스트")
    private List<T> content;

    @Schema(description = "현재 페이지 번호 (1부터 시작)", example = "1")
    private int page;

    @Schema(description = "페이지 크기", example = "10")
    private int size;

    @Schema(description = "전체 요소 개수", example = "100")
    private long totalElements;

    @Schema(description = "전체 페이지 수", example = "10")
    private int totalPages;

    @Schema(description = "첫 페이지 여부", example = "true")
    private boolean isFirst;

    @Schema(description = "마지막 페이지 여부", example = "false")
    private boolean isLast;

    public static <T> PagedResponse<T> of(Page<T> page) {
        return PagedResponse.<T>builder()
                .content(page.getContent())
                .page(page.getNumber() + 1)
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .isFirst(page.isFirst())
                .isLast(page.isLast())
                .build();
    }

    public static <T, U> PagedResponse<U> of(Page<T> page, List<U> convertedContent) {
        return PagedResponse.<U>builder()
                .content(convertedContent)
                .page(page.getNumber() + 1)
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .isFirst(page.isFirst())
                .isLast(page.isLast())
                .build();
    }
}