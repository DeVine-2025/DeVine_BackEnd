package com.umc.devine.domain.coupon.service.command;

import com.umc.devine.domain.coupon.dto.CouponResDTO;
import com.umc.devine.domain.coupon.entity.Coupon;
import com.umc.devine.domain.coupon.entity.CouponCode;
import com.umc.devine.domain.coupon.enums.CouponCodeStatus;
import com.umc.devine.domain.coupon.enums.DiscountType;
import com.umc.devine.domain.coupon.exception.CouponException;
import com.umc.devine.domain.coupon.exception.code.CouponErrorReason;
import com.umc.devine.domain.coupon.repository.CouponCodeRepository;
import com.umc.devine.domain.coupon.repository.CouponRepository;
import com.umc.devine.domain.coupon.repository.MemberCouponRepository;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.enums.MemberMainType;
import com.umc.devine.domain.member.enums.MemberStatus;
import com.umc.devine.domain.member.repository.MemberRepository;
import com.umc.devine.support.IntegrationTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional(propagation = Propagation.NOT_SUPPORTED)
class CouponCommandServiceTest extends IntegrationTestSupport {

    @Autowired
    private CouponCommandService couponCommandService;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private CouponCodeRepository couponCodeRepository;

    @Autowired
    private MemberCouponRepository memberCouponRepository;

    @Autowired
    private MemberRepository memberRepository;

    private Member member;
    private Coupon coupon;

    @BeforeEach
    void setUp() {
        member = memberRepository.save(Member.builder()
                .clerkId("coupon-cmd-member")
                .nickname("coupon-cmd-member")
                .used(MemberStatus.ACTIVE)
                .mainType(MemberMainType.DEVELOPER)
                .build());

        coupon = couponRepository.save(Coupon.builder()
                .name("코드 등록 쿠폰")
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
    @DisplayName("registerByCode")
    class RegisterByCode {

        @Test
        @DisplayName("유효한 코드를 등록하면 보유 쿠폰이 생성되고 코드는 REDEEMED로 전환된다")
        void registersCode() {
            CouponCode couponCode = couponCodeRepository.save(CouponCode.of(coupon, "ABCD1234"));

            CouponResDTO.MemberCouponDTO result = couponCommandService.registerByCode("abcd1234", member);

            assertThat(result.couponName()).isEqualTo("코드 등록 쿠폰");
            CouponCode reloaded = couponCodeRepository.findById(couponCode.getId()).orElseThrow();
            assertThat(reloaded.getStatus()).isEqualTo(CouponCodeStatus.REDEEMED);
        }

        @Test
        @DisplayName("존재하지 않는 코드면 예외가 발생한다")
        void throwsWhenCodeNotFound() {
            assertThatThrownBy(() -> couponCommandService.registerByCode("NOPE0000", member))
                    .isInstanceOf(CouponException.class)
                    .satisfies(e -> assertThat(((CouponException) e).getReason())
                            .isEqualTo(CouponErrorReason.COUPON_CODE_NOT_FOUND));
        }

        @Test
        @DisplayName("이미 등록된 코드면 예외가 발생한다")
        void throwsWhenAlreadyRedeemed() {
            CouponCode couponCode = couponCodeRepository.save(CouponCode.of(coupon, "USED0001"));
            couponCommandService.registerByCode("USED0001", member);

            Member another = memberRepository.save(Member.builder()
                    .clerkId("coupon-cmd-member-2")
                    .nickname("coupon-cmd-member-2")
                    .used(MemberStatus.ACTIVE)
                    .mainType(MemberMainType.DEVELOPER)
                    .build());

            assertThatThrownBy(() -> couponCommandService.registerByCode(couponCode.getCode(), another))
                    .isInstanceOf(CouponException.class)
                    .satisfies(e -> assertThat(((CouponException) e).getReason())
                            .isEqualTo(CouponErrorReason.COUPON_CODE_ALREADY_REDEEMED));
        }

        @Test
        @DisplayName("쿠폰이 만료된 상태면 예외가 발생한다")
        void throwsWhenCouponExpired() {
            Coupon expired = couponRepository.save(Coupon.builder()
                    .name("만료 쿠폰")
                    .discountType(DiscountType.FIXED_AMOUNT)
                    .discountValue(1000L)
                    .issuedCount(1)
                    .usedCount(0)
                    .validFrom(LocalDateTime.now().minusDays(10))
                    .validUntil(LocalDateTime.now().minusDays(1))
                    .isActive(true)
                    .build());
            CouponCode couponCode = couponCodeRepository.save(CouponCode.of(expired, "EXPIRED1"));

            assertThatThrownBy(() -> couponCommandService.registerByCode(couponCode.getCode(), member))
                    .isInstanceOf(CouponException.class)
                    .satisfies(e -> assertThat(((CouponException) e).getReason())
                            .isEqualTo(CouponErrorReason.COUPON_NOT_USABLE));
        }
    }
}
