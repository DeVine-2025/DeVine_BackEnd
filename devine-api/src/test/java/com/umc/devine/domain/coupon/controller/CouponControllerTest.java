package com.umc.devine.domain.coupon.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.devine.domain.coupon.dto.CouponReqDTO;
import com.umc.devine.domain.coupon.entity.Coupon;
import com.umc.devine.domain.coupon.entity.CouponCode;
import com.umc.devine.domain.coupon.entity.MemberCoupon;
import com.umc.devine.domain.coupon.enums.DiscountType;
import com.umc.devine.domain.coupon.repository.CouponCodeRepository;
import com.umc.devine.domain.coupon.repository.CouponRepository;
import com.umc.devine.domain.coupon.repository.MemberCouponRepository;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.enums.MemberMainType;
import com.umc.devine.domain.member.enums.MemberStatus;
import com.umc.devine.domain.member.repository.MemberRepository;
import com.umc.devine.global.security.ClerkPrincipal;
import com.umc.devine.support.ControllerIntegrationTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CouponControllerTest extends ControllerIntegrationTestSupport {

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

    private Member member;
    private Authentication auth;
    private Coupon coupon;

    @BeforeEach
    void setUp() {
        member = memberRepository.save(Member.builder()
                .clerkId("coupon-ctrl-clerk")
                .nickname("coupon-ctrl-member")
                .used(MemberStatus.ACTIVE)
                .mainType(MemberMainType.DEVELOPER)
                .build());

        ClerkPrincipal principal = new ClerkPrincipal("coupon-ctrl-clerk", "coupon-ctrl@example.com", "테스트", null);
        auth = new UsernamePasswordAuthenticationToken(principal, null, Collections.emptyList());

        coupon = couponRepository.save(Coupon.builder()
                .name("컨트롤러 쿠폰")
                .discountType(DiscountType.FIXED_AMOUNT)
                .discountValue(1000L)
                .issuedCount(1)
                .usedCount(0)
                .validFrom(LocalDateTime.now().minusDays(1))
                .validUntil(LocalDateTime.now().plusDays(7))
                .isActive(true)
                .build());
    }

    @AfterEach
    void tearDown() {
        couponCodeRepository.deleteAll();
        memberCouponRepository.deleteAll();
        couponRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @Nested
    @DisplayName("POST /api/v1/coupon/register")
    class RegisterByCode {

        @Test
        @DisplayName("인증된 사용자가 코드를 등록한다")
        void registersCode() throws Exception {
            couponCodeRepository.save(CouponCode.of(coupon, "CTRL0001"));

            mockMvc.perform(post("/api/v1/coupon/register")
                            .with(authentication(auth))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new CouponReqDTO.RegisterCodeReq("CTRL0001"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.couponName").value("컨트롤러 쿠폰"));
        }

        @Test
        @DisplayName("인증 없이 요청하면 401을 반환한다")
        void returns401WithoutAuthentication() throws Exception {
            mockMvc.perform(post("/api/v1/coupon/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new CouponReqDTO.RegisterCodeReq("CTRL0001"))))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("분당 시도 횟수를 초과하면 429를 반환한다 (브루트포스 방지)")
        void returns429WhenRateLimitExceeded() throws Exception {
            for (int i = 0; i < 10; i++) {
                mockMvc.perform(post("/api/v1/coupon/register")
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CouponReqDTO.RegisterCodeReq("NO-SUCH-CODE"))));
            }

            mockMvc.perform(post("/api/v1/coupon/register")
                            .with(authentication(auth))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new CouponReqDTO.RegisterCodeReq("NO-SUCH-CODE"))))
                    .andExpect(status().isTooManyRequests());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/coupon/me")
    class GetMyCoupons {

        @Test
        @DisplayName("보유 쿠폰 목록을 조회한다")
        void returnsMyCoupons() throws Exception {
            memberCouponRepository.save(MemberCoupon.issueTo(member, coupon));

            mockMvc.perform(get("/api/v1/coupon/me").with(authentication(auth)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.coupons.length()").value(1));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/coupon/preview")
    class Preview {

        @Test
        @DisplayName("할인 미리보기를 계산한다")
        void calculatesPreview() throws Exception {
            MemberCoupon memberCoupon = memberCouponRepository.save(MemberCoupon.issueTo(member, coupon));

            CouponReqDTO.PreviewReq request = new CouponReqDTO.PreviewReq(memberCoupon.getId(), 9900L, null);

            mockMvc.perform(post("/api/v1/coupon/preview")
                            .with(authentication(auth))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.discountAmount").value(1000))
                    .andExpect(jsonPath("$.result.finalAmount").value(8900));
        }
    }
}
