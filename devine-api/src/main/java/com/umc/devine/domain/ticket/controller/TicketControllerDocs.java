package com.umc.devine.domain.ticket.controller;

import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.ticket.dto.TicketResDTO;
import com.umc.devine.global.apiPayload.ApiResponse;
import com.umc.devine.global.security.CurrentMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Ticket", description = "티켓 관련 API")
public interface TicketControllerDocs {

    @Operation(summary = "티켓 상품 목록 조회 API", description = "판매 중인 티켓 상품 목록을 조회하는 API입니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK, 성공")
    })
    ApiResponse<TicketResDTO.TicketProductListDTO> getActiveProducts();

    @Operation(summary = "내 리포트 생성권 조회 API", description = "로그인한 사용자의 잔여 리포트 생성권 수량을 조회하는 API입니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK, 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증이 필요합니다.")
    })
    ApiResponse<TicketResDTO.ReportCreditDTO> getMyCredits(
            @Parameter(hidden = true) @CurrentMember Member member
    );
}
