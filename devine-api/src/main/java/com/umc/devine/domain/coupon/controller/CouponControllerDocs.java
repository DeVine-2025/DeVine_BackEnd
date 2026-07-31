package com.umc.devine.domain.coupon.controller;

import com.umc.devine.domain.coupon.dto.CouponReqDTO;
import com.umc.devine.domain.coupon.dto.CouponResDTO;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.global.apiPayload.ApiResponse;
import com.umc.devine.global.security.CurrentMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Coupon", description = "쿠폰 관련 API")
public interface CouponControllerDocs {

    @Operation(summary = "쿠폰 코드 등록 API", description = "관리자가 배치 생성한 쿠폰 코드를 등록해 보유 쿠폰으로 전환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Created, 등록 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "이미 등록된 코드, 사용 불가능한 쿠폰"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증이 필요합니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 쿠폰 코드"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "분당 시도 횟수 초과 (브루트포스 방지)")
    })
    ApiResponse<CouponResDTO.MemberCouponDTO> registerByCode(
            @Parameter(hidden = true) @CurrentMember Member member,
            @Valid @RequestBody CouponReqDTO.RegisterCodeReq request
    );

    @Operation(summary = "내 보유 쿠폰 목록 조회 API", description = "로그인한 사용자가 보유한 쿠폰 목록을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK, 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증이 필요합니다.")
    })
    ApiResponse<CouponResDTO.MemberCouponListDTO> getMyCoupons(
            @Parameter(hidden = true) @CurrentMember Member member
    );

    @Operation(summary = "결제 할인 미리보기 API", description = "보유 쿠폰을 적용했을 때의 할인 금액과 최종 결제 금액을 계산합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK, 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "사용할 수 없는 쿠폰, 적용 대상 상품 불일치"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증이 필요합니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "보유 쿠폰을 찾을 수 없음")
    })
    ApiResponse<CouponResDTO.PreviewDTO> preview(
            @Parameter(hidden = true) @CurrentMember Member member,
            @Valid @RequestBody CouponReqDTO.PreviewReq request
    );
}
