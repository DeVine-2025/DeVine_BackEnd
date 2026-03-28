package com.umc.devine.domain.ticket.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

public class TicketResDTO {

    @Builder
    @Schema(description = "티켓 상품 정보")
    public record TicketProductDTO(
            @Schema(description = "상품 ID", example = "1")
            Long id,

            @Schema(description = "상품명", example = "리포트 생성권 1개")
            String name,

            @Schema(description = "가격 (KRW)", example = "4900")
            Long price,

            @Schema(description = "제공 생성권 수", example = "1")
            Integer creditAmount
    ) {}

    @Builder
    @Schema(description = "티켓 상품 목록")
    public record TicketProductListDTO(
            List<TicketProductDTO> products
    ) {}

    @Builder
    @Schema(description = "리포트 생성권 잔여 수량")
    public record ReportCreditDTO(
            @Schema(description = "잔여 생성권 수", example = "3")
            Integer remainingCount
    ) {}
}
