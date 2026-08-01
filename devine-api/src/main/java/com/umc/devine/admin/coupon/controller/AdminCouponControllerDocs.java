package com.umc.devine.admin.coupon.controller;

import com.umc.devine.admin.coupon.dto.AdminCouponReqDTO;
import com.umc.devine.admin.coupon.dto.AdminCouponResDTO;
import com.umc.devine.global.apiPayload.ApiResponse;
import com.umc.devine.global.dto.PagedResponse;
import com.umc.devine.global.dto.PageRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Tag(name = "Admin Coupon", description = "관리자 쿠폰 관리 API (인증/인가 양식 확정 전까지 임시로 인증 없이 사용 가능)")
public interface AdminCouponControllerDocs {

    @Operation(summary = "쿠폰 생성 API", description = "할인 방식/값, 적용 대상 상품, 유효기간, 발급 수량 제한을 지정해 쿠폰을 생성합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Created, 쿠폰 생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "필수값 누락, 유효기간 역전, 할인 값 범위 오류")
    })
    ApiResponse<AdminCouponResDTO.CouponDTO> createCoupon(
            @Valid @RequestBody AdminCouponReqDTO.CreateCouponReq request
    );

    @Operation(summary = "쿠폰 목록 조회 API", description = "생성된 쿠폰 목록을 최신순으로 페이징 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK, 성공")
    })
    ApiResponse<PagedResponse<AdminCouponResDTO.CouponDTO>> getCoupons(PageRequest pageRequest);

    @Operation(summary = "쿠폰 상세 조회 API", description = "쿠폰 ID로 단건 상세 정보를 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK, 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "쿠폰을 찾을 수 없음")
    })
    ApiResponse<AdminCouponResDTO.CouponDTO> getCoupon(
            @Parameter(description = "쿠폰 ID") Long couponId
    );

    @Operation(summary = "쿠폰 수정 API", description = "이름/유효기간/발급수량제한/활성화여부/설명을 수정합니다. null인 필드는 변경하지 않습니다. 할인 방식/값/적용상품은 수정할 수 없습니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK, 수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "유효기간 역전"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "쿠폰을 찾을 수 없음")
    })
    ApiResponse<AdminCouponResDTO.CouponDTO> updateCoupon(
            @Parameter(description = "쿠폰 ID") Long couponId,
            @Valid @RequestBody AdminCouponReqDTO.UpdateCouponReq request
    );

    @Operation(summary = "쿠폰 발급 API", description = "전체 회원 일괄 발급(ALL) / 특정 회원 지정 발급(SPECIFIC) / 코드 배치 생성(CODE_GEN) 중 하나의 방식으로 쿠폰을 발급합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Created, 발급 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "발급 방식별 필수값 누락, 발급 수량 제한 초과, 사용 불가 쿠폰"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "쿠폰 또는 회원을 찾을 수 없음")
    })
    ApiResponse<AdminCouponResDTO.IssueResultDTO> issueCoupon(
            @Parameter(description = "쿠폰 ID") Long couponId,
            @Valid @RequestBody AdminCouponReqDTO.IssueCouponReq request
    );

    @Operation(summary = "쿠폰 사용 현황 조회 API", description = "쿠폰ID를 지정하면 해당 쿠폰만, 지정하지 않으면 전체 쿠폰의 발급수/사용수/사용률/만료임박여부를 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK, 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "쿠폰을 찾을 수 없음")
    })
    ApiResponse<List<AdminCouponResDTO.UsageStatDTO>> getUsageStats(
            @Parameter(description = "쿠폰 ID (선택, 없으면 전체 목록)") @RequestParam(required = false) Long couponId
    );
}
