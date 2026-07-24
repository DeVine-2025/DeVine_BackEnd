package com.umc.devine.admin.coupon.service.command;

import com.umc.devine.admin.coupon.dto.AdminCouponReqDTO;
import com.umc.devine.admin.coupon.dto.AdminCouponResDTO;
import com.umc.devine.admin.coupon.exception.CouponAdminException;
import com.umc.devine.admin.coupon.exception.code.CouponAdminErrorReason;
import com.umc.devine.domain.coupon.entity.Coupon;
import com.umc.devine.domain.coupon.enums.DiscountType;
import com.umc.devine.domain.coupon.exception.CouponException;
import com.umc.devine.domain.coupon.exception.code.CouponErrorReason;
import com.umc.devine.domain.coupon.repository.CouponCodeRepository;
import com.umc.devine.domain.coupon.repository.CouponRepository;
import com.umc.devine.domain.coupon.repository.MemberCouponRepository;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.enums.MemberMainType;
import com.umc.devine.domain.member.enums.MemberStatus;
import com.umc.devine.domain.member.exception.MemberException;
import com.umc.devine.domain.member.exception.code.MemberErrorReason;
import com.umc.devine.domain.member.repository.MemberRepository;
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
class AdminCouponCommandServiceTest extends IntegrationTestSupport {

    @Autowired
    private AdminCouponCommandService adminCouponCommandService;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private CouponCodeRepository couponCodeRepository;

    @Autowired
    private MemberCouponRepository memberCouponRepository;

    @Autowired
    private MemberRepository memberRepository;

    @AfterEach
    void tearDown() {
        couponCodeRepository.deleteAll();
        memberCouponRepository.deleteAll();
        couponRepository.deleteAll();
        memberRepository.deleteAll();
    }

    private Member saveMember(String clerkId) {
        return memberRepository.save(Member.builder()
                .clerkId(clerkId)
                .nickname(clerkId)
                .used(MemberStatus.ACTIVE)
                .mainType(MemberMainType.DEVELOPER)
                .build());
    }

    private Coupon saveCoupon(DiscountType type, long value, Integer totalIssueLimit) {
        return couponRepository.save(Coupon.builder()
                .name("테스트 쿠폰")
                .discountType(type)
                .discountValue(value)
                .totalIssueLimit(totalIssueLimit)
                .issuedCount(0)
                .usedCount(0)
                .validFrom(LocalDateTime.now().minusDays(1))
                .validUntil(LocalDateTime.now().plusDays(7))
                .isActive(true)
                .build());
    }

    private AdminCouponReqDTO.CreateCouponReq createReq(DiscountType type, long value, Integer limit) {
        return new AdminCouponReqDTO.CreateCouponReq(
                "테스트 쿠폰", type, value, null,
                LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(7),
                limit, "설명"
        );
    }

    @Nested
    @DisplayName("createCoupon")
    class CreateCoupon {

        @Test
        @DisplayName("정액 쿠폰을 생성할 수 있다")
        void createsFixedAmountCoupon() {
            AdminCouponResDTO.CouponDTO result = adminCouponCommandService.createCoupon(
                    createReq(DiscountType.FIXED_AMOUNT, 1000, 100));

            assertThat(result.couponId()).isNotNull();
            assertThat(result.discountType()).isEqualTo(DiscountType.FIXED_AMOUNT);
            assertThat(result.issuedCount()).isZero();
        }

        @Test
        @DisplayName("유효기간 종료일이 시작일보다 빠르면 예외가 발생한다")
        void throwsWhenValidPeriodInverted() {
            AdminCouponReqDTO.CreateCouponReq request = new AdminCouponReqDTO.CreateCouponReq(
                    "쿠폰", DiscountType.FIXED_AMOUNT, 1000L, null,
                    LocalDateTime.now(), LocalDateTime.now().minusDays(1),
                    null, null
            );

            assertThatThrownBy(() -> adminCouponCommandService.createCoupon(request))
                    .isInstanceOf(CouponException.class)
                    .satisfies(e -> assertThat(((CouponException) e).getReason())
                            .isEqualTo(CouponErrorReason.VALID_PERIOD_INVALID));
        }

        @Test
        @DisplayName("정률 할인 값이 100을 초과하면 예외가 발생한다")
        void throwsWhenRateExceeds100() {
            assertThatThrownBy(() -> adminCouponCommandService.createCoupon(
                    createReq(DiscountType.FIXED_RATE, 101, null)))
                    .isInstanceOf(CouponException.class)
                    .satisfies(e -> assertThat(((CouponException) e).getReason())
                            .isEqualTo(CouponErrorReason.DISCOUNT_VALUE_INVALID));
        }
    }

    @Nested
    @DisplayName("updateCoupon")
    class UpdateCoupon {

        @Test
        @DisplayName("이름과 활성화 여부를 부분 수정할 수 있다")
        void updatesPartialFields() {
            Coupon coupon = saveCoupon(DiscountType.FIXED_AMOUNT, 1000, null);

            AdminCouponReqDTO.UpdateCouponReq request = new AdminCouponReqDTO.UpdateCouponReq(
                    "새 이름", null, null, null, false, null);

            AdminCouponResDTO.CouponDTO result = adminCouponCommandService.updateCoupon(coupon.getId(), request);

            assertThat(result.name()).isEqualTo("새 이름");
            assertThat(result.isActive()).isFalse();
        }

        @Test
        @DisplayName("존재하지 않는 쿠폰이면 예외가 발생한다")
        void throwsWhenCouponNotFound() {
            AdminCouponReqDTO.UpdateCouponReq request = new AdminCouponReqDTO.UpdateCouponReq(
                    "이름", null, null, null, null, null);

            assertThatThrownBy(() -> adminCouponCommandService.updateCoupon(999_999L, request))
                    .isInstanceOf(CouponException.class)
                    .satisfies(e -> assertThat(((CouponException) e).getReason())
                            .isEqualTo(CouponErrorReason.COUPON_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("issueCoupon")
    class IssueCoupon {

        @Test
        @DisplayName("ALL 방식으로 발급하면 활성 회원 전원에게 MemberCoupon이 생성된다")
        void issuesToAllActiveMembers() {
            Coupon coupon = saveCoupon(DiscountType.FIXED_AMOUNT, 1000, null);
            Member active1 = saveMember("active-1");
            Member active2 = saveMember("active-2");
            Member withdrawn = saveMember("withdrawn-1");
            withdrawn.withdraw();
            memberRepository.save(withdrawn);

            AdminCouponReqDTO.IssueCouponReq request =
                    new AdminCouponReqDTO.IssueCouponReq(AdminCouponReqDTO.IssueType.ALL, null, null, null);

            AdminCouponResDTO.IssueResultDTO result = adminCouponCommandService.issueCoupon(coupon.getId(), request);

            assertThat(result.issuedCount()).isEqualTo(2);
            assertThat(memberCouponRepository.findByMemberOrderByCreatedAtDesc(active1)).hasSize(1);
            assertThat(memberCouponRepository.findByMemberOrderByCreatedAtDesc(active2)).hasSize(1);
            assertThat(memberCouponRepository.findByMemberOrderByCreatedAtDesc(withdrawn)).isEmpty();
        }

        @Test
        @DisplayName("SPECIFIC 방식으로 지정한 회원들에게만 발급된다")
        void issuesToSpecificMembers() {
            Coupon coupon = saveCoupon(DiscountType.FIXED_AMOUNT, 1000, null);
            Member m1 = saveMember("specific-1");
            Member m2 = saveMember("specific-2");
            saveMember("not-targeted");

            AdminCouponReqDTO.IssueCouponReq request = new AdminCouponReqDTO.IssueCouponReq(
                    AdminCouponReqDTO.IssueType.SPECIFIC, List.of(m1.getId(), m2.getId()), null, null);

            AdminCouponResDTO.IssueResultDTO result = adminCouponCommandService.issueCoupon(coupon.getId(), request);

            assertThat(result.issuedCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("SPECIFIC 방식인데 존재하지 않는 회원 ID가 섞여 있으면 예외가 발생한다")
        void throwsWhenSpecificMemberNotFound() {
            Coupon coupon = saveCoupon(DiscountType.FIXED_AMOUNT, 1000, null);

            AdminCouponReqDTO.IssueCouponReq request = new AdminCouponReqDTO.IssueCouponReq(
                    AdminCouponReqDTO.IssueType.SPECIFIC, List.of(999_999L), null, null);

            assertThatThrownBy(() -> adminCouponCommandService.issueCoupon(coupon.getId(), request))
                    .isInstanceOf(MemberException.class)
                    .satisfies(e -> assertThat(((MemberException) e).getReason())
                            .isEqualTo(MemberErrorReason.NOT_FOUND));
        }

        @Test
        @DisplayName("SPECIFIC 방식인데 회원 ID 목록이 비어있으면 예외가 발생한다")
        void throwsWhenSpecificMemberIdsEmpty() {
            Coupon coupon = saveCoupon(DiscountType.FIXED_AMOUNT, 1000, null);

            AdminCouponReqDTO.IssueCouponReq request = new AdminCouponReqDTO.IssueCouponReq(
                    AdminCouponReqDTO.IssueType.SPECIFIC, List.of(), null, null);

            assertThatThrownBy(() -> adminCouponCommandService.issueCoupon(coupon.getId(), request))
                    .isInstanceOf(CouponAdminException.class)
                    .satisfies(e -> assertThat(((CouponAdminException) e).getReason())
                            .isEqualTo(CouponAdminErrorReason.INVALID_ISSUE_REQUEST));
        }

        @Test
        @DisplayName("CODE_GEN 방식으로 지정한 개수만큼 고유한 코드가 생성된다")
        void issuesGeneratedCodes() {
            Coupon coupon = saveCoupon(DiscountType.FIXED_AMOUNT, 1000, null);

            AdminCouponReqDTO.IssueCouponReq request =
                    new AdminCouponReqDTO.IssueCouponReq(AdminCouponReqDTO.IssueType.CODE_GEN, null, 8, 5);

            AdminCouponResDTO.IssueResultDTO result = adminCouponCommandService.issueCoupon(coupon.getId(), request);

            assertThat(result.issuedCount()).isEqualTo(5);
            assertThat(result.generatedCodes()).hasSize(5);
            assertThat(result.generatedCodes()).doesNotHaveDuplicates();
            result.generatedCodes().forEach(code -> assertThat(code).hasSize(8));
        }

        @Test
        @DisplayName("발급 수량 제한을 초과하면 예외가 발생하고 아무것도 발급되지 않는다")
        void throwsWhenIssueLimitExceeded() {
            Coupon coupon = saveCoupon(DiscountType.FIXED_AMOUNT, 1000, 1);
            Member m1 = saveMember("limit-1");
            Member m2 = saveMember("limit-2");

            AdminCouponReqDTO.IssueCouponReq request = new AdminCouponReqDTO.IssueCouponReq(
                    AdminCouponReqDTO.IssueType.SPECIFIC, List.of(m1.getId(), m2.getId()), null, null);

            assertThatThrownBy(() -> adminCouponCommandService.issueCoupon(coupon.getId(), request))
                    .isInstanceOf(CouponException.class)
                    .satisfies(e -> assertThat(((CouponException) e).getReason())
                            .isEqualTo(CouponErrorReason.COUPON_ISSUE_LIMIT_EXCEEDED));

            assertThat(memberCouponRepository.findByMemberOrderByCreatedAtDesc(m1)).isEmpty();
        }

        @Test
        @DisplayName("비활성/만료된 쿠폰은 발급할 수 없다")
        void throwsWhenCouponNotUsable() {
            Coupon coupon = couponRepository.save(Coupon.builder()
                    .name("만료 쿠폰")
                    .discountType(DiscountType.FIXED_AMOUNT)
                    .discountValue(1000L)
                    .issuedCount(0)
                    .usedCount(0)
                    .validFrom(LocalDateTime.now().minusDays(10))
                    .validUntil(LocalDateTime.now().minusDays(1))
                    .isActive(true)
                    .build());

            AdminCouponReqDTO.IssueCouponReq request =
                    new AdminCouponReqDTO.IssueCouponReq(AdminCouponReqDTO.IssueType.ALL, null, null, null);

            assertThatThrownBy(() -> adminCouponCommandService.issueCoupon(coupon.getId(), request))
                    .isInstanceOf(CouponException.class)
                    .satisfies(e -> assertThat(((CouponException) e).getReason())
                            .isEqualTo(CouponErrorReason.COUPON_NOT_USABLE));
        }
    }
}
