package com.umc.devine.admin.coupon.service.query;

import com.umc.devine.admin.coupon.dto.AdminCouponResDTO;
import com.umc.devine.domain.coupon.entity.Coupon;
import com.umc.devine.domain.coupon.entity.CouponCode;
import com.umc.devine.domain.coupon.enums.DiscountType;
import com.umc.devine.domain.coupon.exception.CouponException;
import com.umc.devine.domain.coupon.exception.code.CouponErrorReason;
import com.umc.devine.domain.coupon.repository.CouponCodeRepository;
import com.umc.devine.domain.coupon.repository.CouponRepository;
import com.umc.devine.global.dto.PagedResponse;
import com.umc.devine.global.dto.PageRequest;
import com.umc.devine.support.IntegrationTestSupport;
import org.junit.jupiter.api.AfterEach;
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
class AdminCouponQueryServiceTest extends IntegrationTestSupport {

    @Autowired
    private AdminCouponQueryService adminCouponQueryService;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private CouponCodeRepository couponCodeRepository;

    @AfterEach
    void tearDown() {
        couponCodeRepository.deleteAll();
        couponRepository.deleteAll();
    }

    private Coupon saveCoupon(int issued, int used, LocalDateTime validUntil) {
        return couponRepository.save(Coupon.builder()
                .name("쿠폰")
                .discountType(DiscountType.FIXED_AMOUNT)
                .discountValue(1000L)
                .issuedCount(issued)
                .usedCount(used)
                .validFrom(LocalDateTime.now().minusDays(10))
                .validUntil(validUntil)
                .isActive(true)
                .build());
    }

    @Nested
    @DisplayName("getCoupons")
    class GetCoupons {

        @Test
        @DisplayName("생성된 쿠폰을 최신순으로 페이징 조회한다")
        void returnsPagedCoupons() {
            saveCoupon(0, 0, LocalDateTime.now().plusDays(30));
            saveCoupon(0, 0, LocalDateTime.now().plusDays(30));

            PagedResponse<AdminCouponResDTO.CouponDTO> result =
                    adminCouponQueryService.getCoupons(PageRequest.of(1, 10));

            assertThat(result.getTotalElements()).isEqualTo(2);
            assertThat(result.getContent()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("getUsageStats")
    class GetUsageStats {

        @Test
        @DisplayName("쿠폰ID를 지정하면 해당 쿠폰의 사용률과 만료임박여부를 계산한다")
        void returnsSingleCouponStat() {
            Coupon coupon = saveCoupon(10, 3, LocalDateTime.now().plusDays(3));

            List<AdminCouponResDTO.UsageStatDTO> result = adminCouponQueryService.getUsageStats(coupon.getId());

            assertThat(result).hasSize(1);
            AdminCouponResDTO.UsageStatDTO stat = result.get(0);
            assertThat(stat.usageRate()).isEqualTo(0.3);
            assertThat(stat.isExpiringSoon()).isTrue();
        }

        @Test
        @DisplayName("발급수가 0이면 사용률은 0으로 계산된다")
        void usageRateIsZeroWhenNoneIssued() {
            Coupon coupon = saveCoupon(0, 0, LocalDateTime.now().plusDays(30));

            List<AdminCouponResDTO.UsageStatDTO> result = adminCouponQueryService.getUsageStats(coupon.getId());

            assertThat(result.get(0).usageRate()).isZero();
            assertThat(result.get(0).isExpiringSoon()).isFalse();
        }

        @Test
        @DisplayName("쿠폰ID가 없으면 전체 쿠폰의 현황을 조회한다")
        void returnsAllCouponStatsWhenIdMissing() {
            saveCoupon(5, 1, LocalDateTime.now().plusDays(30));
            saveCoupon(5, 1, LocalDateTime.now().plusDays(30));

            List<AdminCouponResDTO.UsageStatDTO> result = adminCouponQueryService.getUsageStats(null);

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("존재하지 않는 쿠폰ID면 예외가 발생한다")
        void throwsWhenCouponNotFound() {
            assertThatThrownBy(() -> adminCouponQueryService.getUsageStats(999_999L))
                    .isInstanceOf(CouponException.class)
                    .satisfies(e -> assertThat(((CouponException) e).getReason())
                            .isEqualTo(CouponErrorReason.COUPON_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("getCoupon")
    class GetCoupon {

        @Test
        @DisplayName("쿠폰 ID로 단건 상세 정보를 조회한다")
        void returnsCouponDetail() {
            Coupon coupon = saveCoupon(5, 2, LocalDateTime.now().plusDays(30));

            AdminCouponResDTO.CouponDTO result = adminCouponQueryService.getCoupon(coupon.getId());

            assertThat(result.couponId()).isEqualTo(coupon.getId());
            assertThat(result.name()).isEqualTo("쿠폰");
            assertThat(result.issuedCount()).isEqualTo(5);
            assertThat(result.usedCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("이 쿠폰으로 생성된 코드 목록을 함께 반환한다")
        void returnsCouponCodes() {
            Coupon coupon = saveCoupon(0, 0, LocalDateTime.now().plusDays(30));
            couponCodeRepository.save(CouponCode.of(coupon, "DETAIL001", 1));
            couponCodeRepository.save(CouponCode.of(coupon, "DETAIL002", 5));

            AdminCouponResDTO.CouponDTO result = adminCouponQueryService.getCoupon(coupon.getId());

            assertThat(result.codes()).extracting(AdminCouponResDTO.CouponCodeDTO::code)
                    .containsExactlyInAnyOrder("DETAIL001", "DETAIL002");
        }

        @Test
        @DisplayName("목록 조회에서는 codes가 null이다 (N+1 방지)")
        void listDoesNotIncludeCodes() {
            Coupon coupon = saveCoupon(0, 0, LocalDateTime.now().plusDays(30));
            couponCodeRepository.save(CouponCode.of(coupon, "LIST0001", 1));

            PagedResponse<AdminCouponResDTO.CouponDTO> result =
                    adminCouponQueryService.getCoupons(PageRequest.of(1, 10));

            assertThat(result.getContent().get(0).codes()).isNull();
        }

        @Test
        @DisplayName("존재하지 않는 쿠폰ID면 예외가 발생한다")
        void throwsWhenCouponNotFound() {
            assertThatThrownBy(() -> adminCouponQueryService.getCoupon(999_999L))
                    .isInstanceOf(CouponException.class)
                    .satisfies(e -> assertThat(((CouponException) e).getReason())
                            .isEqualTo(CouponErrorReason.COUPON_NOT_FOUND));
        }
    }
}
