package com.umc.devine.domain.coupon.service.query;

import com.umc.devine.domain.coupon.dto.CouponResDTO;
import com.umc.devine.domain.coupon.entity.Coupon;
import com.umc.devine.domain.coupon.entity.MemberCoupon;
import com.umc.devine.domain.coupon.enums.DiscountType;
import com.umc.devine.domain.coupon.exception.CouponException;
import com.umc.devine.domain.coupon.exception.code.CouponErrorReason;
import com.umc.devine.domain.coupon.repository.CouponRepository;
import com.umc.devine.domain.coupon.repository.MemberCouponRepository;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.enums.MemberMainType;
import com.umc.devine.domain.member.enums.MemberStatus;
import com.umc.devine.domain.member.repository.MemberRepository;
import com.umc.devine.domain.ticket.entity.TicketProduct;
import com.umc.devine.domain.ticket.repository.TicketProductRepository;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional(propagation = Propagation.NOT_SUPPORTED)
class CouponQueryServiceTest extends IntegrationTestSupport {

    @Autowired
    private CouponQueryService couponQueryService;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private MemberCouponRepository memberCouponRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private TicketProductRepository ticketProductRepository;

    private Member member;

    @BeforeEach
    void setUp() {
        member = memberRepository.save(Member.builder()
                .clerkId("coupon-query-member")
                .nickname("coupon-query-member")
                .used(MemberStatus.ACTIVE)
                .mainType(MemberMainType.DEVELOPER)
                .build());
    }

    @AfterEach
    void tearDown() {
        memberCouponRepository.deleteAll();
        couponRepository.deleteAll();
        memberRepository.deleteAll();
        ticketProductRepository.deleteAll();
    }

    private Coupon saveCoupon(DiscountType type, long value, TicketProduct applicable) {
        return couponRepository.save(Coupon.builder()
                .name("쿠폰")
                .discountType(type)
                .discountValue(value)
                .applicableTicketProduct(applicable)
                .issuedCount(1)
                .usedCount(0)
                .validFrom(LocalDateTime.now().minusDays(1))
                .validUntil(LocalDateTime.now().plusDays(7))
                .isActive(true)
                .build());
    }

    @Nested
    @DisplayName("getMyCoupons")
    class GetMyCoupons {

        @Test
        @DisplayName("보유 쿠폰 목록을 최신순으로 반환한다")
        void returnsMyCoupons() {
            Coupon coupon = saveCoupon(DiscountType.FIXED_AMOUNT, 1000, null);
            memberCouponRepository.save(MemberCoupon.issueTo(member, coupon));

            CouponResDTO.MemberCouponListDTO result = couponQueryService.getMyCoupons(member);

            assertThat(result.coupons()).hasSize(1);
            assertThat(result.coupons().get(0).couponName()).isEqualTo("쿠폰");
            assertThat(result.coupons().get(0).validUntil()).isNotNull();
            assertThat(result.coupons().get(0).isUsable()).isTrue();
        }

        @Test
        @DisplayName("쿠폰이 만료되면 보유 쿠폰 목록에는 남아있지만 isUsable은 false로 내려간다")
        void expiredCouponIsMarkedNotUsable() {
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
            memberCouponRepository.save(MemberCoupon.issueTo(member, expired));

            CouponResDTO.MemberCouponListDTO result = couponQueryService.getMyCoupons(member);

            assertThat(result.coupons()).hasSize(1);
            assertThat(result.coupons().get(0).isUsable()).isFalse();
        }

        @Test
        @DisplayName("관리자가 쿠폰을 비활성화하면 isUsable은 false로 내려간다")
        void deactivatedCouponIsMarkedNotUsable() {
            Coupon coupon = couponRepository.save(Coupon.builder()
                    .name("비활성 쿠폰")
                    .discountType(DiscountType.FIXED_AMOUNT)
                    .discountValue(1000L)
                    .issuedCount(1)
                    .usedCount(0)
                    .validFrom(LocalDateTime.now().minusDays(1))
                    .validUntil(LocalDateTime.now().plusDays(7))
                    .isActive(false)
                    .build());
            memberCouponRepository.save(MemberCoupon.issueTo(member, coupon));

            CouponResDTO.MemberCouponListDTO result = couponQueryService.getMyCoupons(member);

            assertThat(result.coupons().get(0).isUsable()).isFalse();
        }
    }

    @Nested
    @DisplayName("preview")
    class Preview {

        @Test
        @DisplayName("정액 쿠폰 할인 금액을 계산한다")
        void calculatesFixedAmountDiscount() {
            Coupon coupon = saveCoupon(DiscountType.FIXED_AMOUNT, 1000, null);
            MemberCoupon memberCoupon = memberCouponRepository.save(MemberCoupon.issueTo(member, coupon));

            CouponResDTO.PreviewDTO result =
                    couponQueryService.preview(memberCoupon.getId(), 9900L, null, member);

            assertThat(result.discountAmount()).isEqualTo(1000L);
            assertThat(result.finalAmount()).isEqualTo(8900L);
        }

        @Test
        @DisplayName("정률 쿠폰 할인 금액을 계산한다")
        void calculatesFixedRateDiscount() {
            Coupon coupon = saveCoupon(DiscountType.FIXED_RATE, 20, null);
            MemberCoupon memberCoupon = memberCouponRepository.save(MemberCoupon.issueTo(member, coupon));

            CouponResDTO.PreviewDTO result =
                    couponQueryService.preview(memberCoupon.getId(), 10000L, null, member);

            assertThat(result.discountAmount()).isEqualTo(2000L);
            assertThat(result.finalAmount()).isEqualTo(8000L);
        }

        @Test
        @DisplayName("적용 대상 상품이 지정된 쿠폰은 해당 상품이 없으면 예외가 발생한다")
        void throwsWhenProductNotApplicable() {
            TicketProduct product = ticketProductRepository.save(TicketProduct.builder()
                    .name("리포트 생성권 1개").price(4900L).creditAmount(1).active(true).build());
            Coupon coupon = saveCoupon(DiscountType.FIXED_AMOUNT, 1000, product);
            MemberCoupon memberCoupon = memberCouponRepository.save(MemberCoupon.issueTo(member, coupon));

            assertThatThrownBy(() -> couponQueryService.preview(memberCoupon.getId(), 9900L, List.of(999L), member))
                    .isInstanceOf(CouponException.class)
                    .satisfies(e -> assertThat(((CouponException) e).getReason())
                            .isEqualTo(CouponErrorReason.COUPON_NOT_APPLICABLE_PRODUCT));
        }

        @Test
        @DisplayName("이미 사용된 쿠폰이면 예외가 발생한다")
        void throwsWhenAlreadyUsed() {
            Coupon coupon = saveCoupon(DiscountType.FIXED_AMOUNT, 1000, null);
            MemberCoupon memberCoupon = memberCouponRepository.save(MemberCoupon.issueTo(member, coupon));
            memberCoupon.use(null);
            memberCouponRepository.save(memberCoupon);

            assertThatThrownBy(() -> couponQueryService.preview(memberCoupon.getId(), 9900L, null, member))
                    .isInstanceOf(CouponException.class)
                    .satisfies(e -> assertThat(((CouponException) e).getReason())
                            .isEqualTo(CouponErrorReason.COUPON_ALREADY_USED));
        }

        @Test
        @DisplayName("보유하지 않은 쿠폰이면 예외가 발생한다")
        void throwsWhenNotOwned() {
            assertThatThrownBy(() -> couponQueryService.preview(999_999L, 9900L, null, member))
                    .isInstanceOf(CouponException.class)
                    .satisfies(e -> assertThat(((CouponException) e).getReason())
                            .isEqualTo(CouponErrorReason.MEMBER_COUPON_NOT_FOUND));
        }
    }
}
