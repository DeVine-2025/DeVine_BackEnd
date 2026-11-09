package com.umc.devine.admin.payment.controller;

import com.umc.devine.admin.payment.dto.AdminPaymentReqDTO;
import com.umc.devine.admin.payment.dto.AdminPaymentResDTO;
import com.umc.devine.global.apiPayload.ApiResponse;
import com.umc.devine.global.dto.PageRequest;
import com.umc.devine.global.dto.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Admin Payment", description = "관리자 결제 관리 API (ROLE_ADMIN 필요)")
public interface AdminPaymentControllerDocs {

    @Operation(summary = "결제 내역 목록 조회 API",
            description = "유저 닉네임/상품/결제일 범위로 결제 내역을 필터링해 결제일 최신순으로 페이징 조회합니다. 기간 필터는 실제 결제 시각(paidAt) 기준이며 종료일은 포함됩니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK, 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "관리자 인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "관리자 권한 없음")
    })
    ApiResponse<PagedResponse<AdminPaymentResDTO.PaymentSummaryDTO>> searchPayments(
            AdminPaymentReqDTO.SearchDTO condition,
            PageRequest pageRequest
    );

    @Operation(summary = "결제 상세 조회 API",
            description = "결제 수단, 구매 상품 목록, 지급 크레딧, 환불 이력을 포함한 단건 상세 정보를 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK, 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "관리자 인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "관리자 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "결제 내역을 찾을 수 없음")
    })
    ApiResponse<AdminPaymentResDTO.PaymentDetailDTO> getPaymentDetail(
            @Parameter(description = "결제 ID") Long paymentId
    );

    @Operation(summary = "결제 환불 API",
            description = "PG사에 전액 취소를 요청하고 해당 결제로 지급된 리포트 생성권을 회수합니다. "
                    + "PG 취소 결과가 불명확한 경우 환불을 실패로 확정하지 않고 대사 대상(UNKNOWN)으로 남긴 뒤 504를 반환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK, 환불 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "환불 사유 누락, 결제 완료 상태가 아님, 이미 환불되었거나 환불 진행 중"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "회원 탈퇴 환불 신청(REQUESTED)이 있는 결제. 신청 금액만 부분 취소해야 함"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "관리자 인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "관리자 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "결제 내역을 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "PG 취소는 성공했으나 DB 반영 실패 (대사 대상)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "502", description = "PG 취소 거절"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "504", description = "PG 취소 결과 불명 (대사 대상)")
    })
    ApiResponse<AdminPaymentResDTO.RefundResultDTO> refund(
            @Parameter(description = "결제 ID") Long paymentId,
            @Valid @RequestBody AdminPaymentReqDTO.RefundDTO request
    );

    @Operation(summary = "회원 탈퇴 환불 신청 완료 처리 API",
            description = "운영자가 포트원 콘솔에서 신청 금액(refundAmount)만 부분 취소한 뒤 호출합니다. "
                    + "신청 상태를 COMPLETED로 바꾸고 환불 연락 이메일을 지웁니다. PG 호출은 하지 않습니다. "
                    + "관리자 '탈퇴 환불 신청 목록' 화면 전까지 쓰는 최소 API입니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK, 완료 처리"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "관리자 인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "관리자 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "환불 신청을 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "접수(REQUESTED) 상태의 탈퇴 환불 신청이 아님")
    })
    ApiResponse<AdminPaymentResDTO.WithdrawalRefundCompleteDTO> completeWithdrawalRefund(
            @Parameter(description = "환불 신청 ID") Long refundId
    );
}
