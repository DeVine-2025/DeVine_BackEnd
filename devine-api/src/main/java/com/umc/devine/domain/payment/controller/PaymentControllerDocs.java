package com.umc.devine.domain.payment.controller;

import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.payment.dto.PaymentReqDTO;
import com.umc.devine.domain.payment.dto.PaymentResDTO;
import com.umc.devine.domain.payment.enums.PgProvider;
import com.umc.devine.global.apiPayload.ApiResponse;
import com.umc.devine.global.security.CurrentMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Payment", description = "결제 관련 API")
public interface PaymentControllerDocs {

    @Operation(summary = "결제 완료 API", description = "PortOne 결제 완료 후 서버에서 검증하고 저장하는 API입니다. 구매 항목(items)에 티켓 상품 ID와 수량을 포함해야 합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Created, 결제 완료 및 리포트 생성권 지급"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "이미 처리된 결제, 금액/상태 불일치, 존재하지 않는 상품"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증이 필요합니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "502", description = "PortOne API 오류")
    })
    ApiResponse<PaymentResDTO.PaymentDTO> completePayment(
            @Parameter(hidden = true) @CurrentMember Member member,
            @Valid @RequestBody PaymentReqDTO.CompletePaymentDTO request
    );

    @Operation(summary = "내 결제 목록 조회 API", description = "로그인한 사용자의 결제 내역을 조회하는 API입니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK, 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증이 필요합니다.")
    })
    ApiResponse<PaymentResDTO.PaymentListDTO> getMyPayments(
            @Parameter(hidden = true) @CurrentMember Member member
    );

    @Operation(summary = "채널키 조회 API", description = "PG사에 해당하는 PortOne 채널키를 조회하는 API입니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK, 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "지원하지 않는 PG사")
    })
    ApiResponse<PaymentResDTO.ChannelKeyDTO> getChannelKey(
            @Parameter(description = "PG사 (NHN_KCP, KG_INICIS, KAKAOPAY)", example = "NHN_KCP")
            @RequestParam PgProvider pg
    );
}
