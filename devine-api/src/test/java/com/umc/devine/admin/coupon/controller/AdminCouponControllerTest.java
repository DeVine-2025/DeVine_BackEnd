package com.umc.devine.admin.coupon.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.devine.admin.coupon.dto.AdminCouponReqDTO;
import com.umc.devine.domain.coupon.entity.Coupon;
import com.umc.devine.domain.coupon.enums.DiscountType;
import com.umc.devine.domain.coupon.repository.CouponCodeRepository;
import com.umc.devine.domain.coupon.repository.CouponRepository;
import com.umc.devine.domain.coupon.repository.MemberCouponRepository;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.enums.MemberMainType;
import com.umc.devine.domain.member.enums.MemberStatus;
import com.umc.devine.domain.member.repository.MemberRepository;
import com.umc.devine.support.ControllerIntegrationTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AdminCouponControllerTest extends ControllerIntegrationTestSupport {

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private CouponCodeRepository couponCodeRepository;

    @Autowired
    private MemberCouponRepository memberCouponRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Coupon coupon;

    @BeforeEach
    void setUp() {
        coupon = couponRepository.save(Coupon.builder()
                .name("컨트롤러 테스트 쿠폰")
                .discountType(DiscountType.FIXED_AMOUNT)
                .discountValue(1000L)
                .issuedCount(0)
                .usedCount(0)
                .validFrom(LocalDateTime.now().minusDays(1))
                .validUntil(LocalDateTime.now().plusDays(7))
                .isActive(true)
                .build());
    }

    @AfterEach
    void tearDown() {
        memberCouponRepository.deleteAll();
        couponCodeRepository.deleteAll();
        couponRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @Nested
    @DisplayName("POST /admin/v1/coupon")
    class CreateCoupon {

        @Test
        @DisplayName("인증 없이도 쿠폰을 생성할 수 있다")
        void createsWithoutAuthentication() throws Exception {
            AdminCouponReqDTO.CreateCouponReq request = new AdminCouponReqDTO.CreateCouponReq(
                    "신규 쿠폰", DiscountType.FIXED_RATE, 10L, null,
                    LocalDateTime.now(), LocalDateTime.now().plusDays(30),
                    50, "설명"
            );

            mockMvc.perform(post("/admin/v1/coupon")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.result.name").value("신규 쿠폰"));
        }

        @Test
        @DisplayName("유효기간이 역전되면 400을 반환한다")
        void returns400WhenValidPeriodInverted() throws Exception {
            AdminCouponReqDTO.CreateCouponReq request = new AdminCouponReqDTO.CreateCouponReq(
                    "쿠폰", DiscountType.FIXED_AMOUNT, 1000L, null,
                    LocalDateTime.now(), LocalDateTime.now().minusDays(1),
                    null, null
            );

            mockMvc.perform(post("/admin/v1/coupon")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /admin/v1/coupon")
    class GetCoupons {

        @Test
        @DisplayName("쿠폰 목록을 페이징 조회한다")
        void returnsPagedList() throws Exception {
            mockMvc.perform(get("/admin/v1/coupon").param("page", "1").param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.content").isArray())
                    .andExpect(jsonPath("$.result.totalElements").value(1));
        }
    }

    @Nested
    @DisplayName("GET /admin/v1/coupon/{couponId}")
    class GetCoupon {

        @Test
        @DisplayName("쿠폰 상세 정보를 조회한다")
        void returnsCouponDetail() throws Exception {
            mockMvc.perform(get("/admin/v1/coupon/{couponId}", coupon.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.couponId").value(coupon.getId()))
                    .andExpect(jsonPath("$.result.name").value("컨트롤러 테스트 쿠폰"));
        }

        @Test
        @DisplayName("존재하지 않는 쿠폰이면 404를 반환한다")
        void returns404WhenNotFound() throws Exception {
            mockMvc.perform(get("/admin/v1/coupon/{couponId}", 999_999L))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PATCH /admin/v1/coupon/{couponId}")
    class UpdateCoupon {

        @Test
        @DisplayName("쿠폰을 수정한다")
        void updatesCoupon() throws Exception {
            AdminCouponReqDTO.UpdateCouponReq request = new AdminCouponReqDTO.UpdateCouponReq(
                    "수정된 이름", null, null, null, false, null);

            mockMvc.perform(patch("/admin/v1/coupon/{couponId}", coupon.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.name").value("수정된 이름"))
                    .andExpect(jsonPath("$.result.isActive").value(false));
        }

        @Test
        @DisplayName("존재하지 않는 쿠폰이면 404를 반환한다")
        void returns404WhenNotFound() throws Exception {
            AdminCouponReqDTO.UpdateCouponReq request = new AdminCouponReqDTO.UpdateCouponReq(
                    "이름", null, null, null, null, null);

            mockMvc.perform(patch("/admin/v1/coupon/{couponId}", 999_999L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /admin/v1/coupon/{couponId}/issue")
    class IssueCoupon {

        @Test
        @DisplayName("SPECIFIC 방식으로 지정 회원에게 발급한다")
        void issuesToSpecificMembers() throws Exception {
            Member member = memberRepository.save(Member.builder()
                    .clerkId("controller-test-member")
                    .nickname("ctrl-test-member")
                    .used(MemberStatus.ACTIVE)
                    .mainType(MemberMainType.DEVELOPER)
                    .build());

            AdminCouponReqDTO.IssueCouponReq request = new AdminCouponReqDTO.IssueCouponReq(
                    AdminCouponReqDTO.IssueType.SPECIFIC, List.of(member.getNickname()), null, null, null, null);

            mockMvc.perform(post("/admin/v1/coupon/{couponId}/issue", coupon.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.issuedCount").value(1));
        }

        @Test
        @DisplayName("CODE_GEN 방식으로 코드를 배치 생성한다")
        void issuesGeneratedCodes() throws Exception {
            AdminCouponReqDTO.IssueCouponReq request = new AdminCouponReqDTO.IssueCouponReq(
                    AdminCouponReqDTO.IssueType.CODE_GEN, null, 8, 3, null, null);

            mockMvc.perform(post("/admin/v1/coupon/{couponId}/issue", coupon.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.issuedCount").value(3))
                    .andExpect(jsonPath("$.result.generatedCodes.length()").value(3));
        }

        @Test
        @DisplayName("코드를 직접 입력하고 maxUses를 지정하면 공유 코드 1개가 생성된다")
        void issuesExplicitSharedCode() throws Exception {
            AdminCouponReqDTO.IssueCouponReq request = new AdminCouponReqDTO.IssueCouponReq(
                    AdminCouponReqDTO.IssueType.CODE_GEN, null, null, null, "summer2026", 50);

            mockMvc.perform(post("/admin/v1/coupon/{couponId}/issue", coupon.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.issuedCount").value(50))
                    .andExpect(jsonPath("$.result.generatedCodes[0]").value("SUMMER2026"));
        }
    }

    @Nested
    @DisplayName("GET /admin/v1/coupon/usage")
    class GetUsageStats {

        @Test
        @DisplayName("쿠폰ID 없이 요청하면 전체 쿠폰 현황을 반환한다")
        void returnsAllStatsWithoutId() throws Exception {
            mockMvc.perform(get("/admin/v1/coupon/usage"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result").isArray())
                    .andExpect(jsonPath("$.result.length()").value(1));
        }

        @Test
        @DisplayName("쿠폰ID를 지정하면 해당 쿠폰 현황만 반환한다")
        void returnsSingleStatWithId() throws Exception {
            mockMvc.perform(get("/admin/v1/coupon/usage").param("couponId", coupon.getId().toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.length()").value(1))
                    .andExpect(jsonPath("$.result[0].couponId").value(coupon.getId()));
        }
    }
}
