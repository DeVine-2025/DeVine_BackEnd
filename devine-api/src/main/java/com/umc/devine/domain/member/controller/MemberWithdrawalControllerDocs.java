package com.umc.devine.domain.member.controller;

import com.umc.devine.domain.member.dto.WithdrawalReqDTO;
import com.umc.devine.domain.member.dto.WithdrawalResDTO;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.global.apiPayload.ApiResponse;
import com.umc.devine.global.security.ClerkPrincipal;
import com.umc.devine.global.security.CurrentMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@Tag(name = "Member Withdrawal", description = "회원 탈퇴 API. 정지/강제탈퇴 예정 회원도 호출할 수 있습니다.")
public interface MemberWithdrawalControllerDocs {

    @Operation(summary = "탈퇴 미리보기 API",
            description = "대기 중인 지원/제안, 지워질 리포트 수, 생성 중인 리포트 여부, 남은 생성권의 환불 가능/불가 수량과 환불 예정 금액을 조회합니다. "
                    + "탈퇴 화면을 띄울 때마다 다시 호출합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK, 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증이 필요합니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "가입되지 않았거나 이미 탈퇴한 회원입니다.")
    })
    ApiResponse<WithdrawalResDTO.PreviewDTO> getWithdrawalPreview(
            @Parameter(hidden = true) @CurrentMember(includeSanctioned = true) Member member,
            @Parameter(hidden = true) @AuthenticationPrincipal ClerkPrincipal principal
    );

    @Operation(summary = "대기 지원/제안 일괄 취소 API",
            description = "수락/거절 전인 지원/제안을 모두 취소하고 상대방에게 취소 알림을 보냅니다. 알림에 탈퇴 사실은 담지 않습니다. "
                    + "대기 건이 없으면 0건으로 성공합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK, 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증이 필요합니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "가입되지 않았거나 이미 탈퇴한 회원입니다.")
    })
    ApiResponse<WithdrawalResDTO.CancelPendingResultDTO> cancelPendingMatchings(
            @Parameter(hidden = true) @CurrentMember(includeSanctioned = true) Member member
    );

    @Operation(summary = "탈퇴 실행 API",
            description = "즉시 탈퇴합니다. 프로필, 리포트, 프로젝트, 보낸 채팅 메시지, 쿠폰을 지우고, 결제/환불 기록과 매칭 이력은 익명 상태로 보관합니다. "
                    + "환불을 신청하면 남은 생성권 중 환불 가능분을 결제 건별로 신청하고 운영자에게 알립니다. "
                    + "환불 여부와 상관없이 남은 생성권은 0이 됩니다. Clerk 계정/이미지 파일은 커밋 뒤 정리되며, 실패해도 탈퇴는 성공으로 처리됩니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK, 탈퇴 완료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "confirmed 누락/false, 환불 신청 시 연락 이메일 누락/형식 오류"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증이 필요합니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "가입되지 않았거나 이미 탈퇴한 회원입니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "대기 중인 지원/제안이 남아 있음. result.pendingCount에 건수")
    })
    ApiResponse<WithdrawalResDTO.WithdrawResultDTO> withdraw(
            @Parameter(hidden = true) @CurrentMember(includeSanctioned = true) Member member,
            @RequestBody @Valid WithdrawalReqDTO.WithdrawReq request
    );
}
