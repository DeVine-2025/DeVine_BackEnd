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
        memberCouponRepository.deleteAll();
        couponCodeRepository.deleteAll();
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
                    "새 이름", null, null, null, false, false, null);

            AdminCouponResDTO.CouponDTO result = adminCouponCommandService.updateCoupon(coupon.getId(), request);

            assertThat(result.name()).isEqualTo("새 이름");
            assertThat(result.isActive()).isFalse();
        }

        @Test
        @DisplayName("존재하지 않는 쿠폰이면 예외가 발생한다")
        void throwsWhenCouponNotFound() {
            AdminCouponReqDTO.UpdateCouponReq request = new AdminCouponReqDTO.UpdateCouponReq(
                    "이름", null, null, null, false, null, null);

            assertThatThrownBy(() -> adminCouponCommandService.updateCoupon(999_999L, request))
                    .isInstanceOf(CouponException.class)
                    .satisfies(e -> assertThat(((CouponException) e).getReason())
                            .isEqualTo(CouponErrorReason.COUPON_NOT_FOUND));
        }

        @Test
        @DisplayName("clearTotalIssueLimit이 true면 발급 수량 제한을 무제한으로 되돌린다")
        void clearsTotalIssueLimit() {
            Coupon coupon = saveCoupon(DiscountType.FIXED_AMOUNT, 1000, 100);

            AdminCouponReqDTO.UpdateCouponReq request = new AdminCouponReqDTO.UpdateCouponReq(
                    null, null, null, null, true, null, null);

            AdminCouponResDTO.CouponDTO result = adminCouponCommandService.updateCoupon(coupon.getId(), request);

            assertThat(result.totalIssueLimit()).isNull();
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
                    new AdminCouponReqDTO.IssueCouponReq(AdminCouponReqDTO.IssueType.ALL, null, null, null, null, null);

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
                    AdminCouponReqDTO.IssueType.SPECIFIC, List.of(m1.getNickname(), m2.getNickname()), null, null, null, null);

            AdminCouponResDTO.IssueResultDTO result = adminCouponCommandService.issueCoupon(coupon.getId(), request);

            assertThat(result.issuedCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("SPECIFIC 방식인데 존재하지 않는 닉네임이 섞여 있으면 예외가 발생한다")
        void throwsWhenSpecificMemberNotFound() {
            Coupon coupon = saveCoupon(DiscountType.FIXED_AMOUNT, 1000, null);

            AdminCouponReqDTO.IssueCouponReq request = new AdminCouponReqDTO.IssueCouponReq(
                    AdminCouponReqDTO.IssueType.SPECIFIC, List.of("no-such-nickname"), null, null, null, null);

            assertThatThrownBy(() -> adminCouponCommandService.issueCoupon(coupon.getId(), request))
                    .isInstanceOf(MemberException.class)
                    .satisfies(e -> assertThat(((MemberException) e).getReason())
                            .isEqualTo(MemberErrorReason.NOT_FOUND));
        }

        @Test
        @DisplayName("SPECIFIC 방식인데 닉네임 목록이 비어있으면 예외가 발생한다")
        void throwsWhenSpecificMemberIdsEmpty() {
            Coupon coupon = saveCoupon(DiscountType.FIXED_AMOUNT, 1000, null);

            AdminCouponReqDTO.IssueCouponReq request = new AdminCouponReqDTO.IssueCouponReq(
                    AdminCouponReqDTO.IssueType.SPECIFIC, List.of(), null, null, null, null);

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
                    new AdminCouponReqDTO.IssueCouponReq(AdminCouponReqDTO.IssueType.CODE_GEN, null, 8, 5, null, null);

            AdminCouponResDTO.IssueResultDTO result = adminCouponCommandService.issueCoupon(coupon.getId(), request);

            assertThat(result.issuedCount()).isEqualTo(5);
            assertThat(result.generatedCodes()).hasSize(5);
            assertThat(result.generatedCodes()).doesNotHaveDuplicates();
            result.generatedCodes().forEach(code -> assertThat(code).hasSize(8));
        }

        @Test
        @DisplayName("코드를 직접 입력하면 그 코드로 쿠폰 코드가 하나 생성된다")
        void issuesExplicitCode() {
            Coupon coupon = saveCoupon(DiscountType.FIXED_AMOUNT, 1000, null);

            AdminCouponReqDTO.IssueCouponReq request = new AdminCouponReqDTO.IssueCouponReq(
                    AdminCouponReqDTO.IssueType.CODE_GEN, null, null, null, "welcome10", null);

            AdminCouponResDTO.IssueResultDTO result = adminCouponCommandService.issueCoupon(coupon.getId(), request);

            assertThat(result.issuedCount()).isEqualTo(1);
            assertThat(result.generatedCodes()).containsExactly("WELCOME10");
            Coupon reloaded = couponRepository.findById(coupon.getId()).orElseThrow();
            assertThat(reloaded.getIssuedCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("직접입력한 코드에 영문/숫자가 아닌 문자가 섞여 있으면 예외가 발생한다")
        void throwsWhenExplicitCodeHasNonAlphanumericChars() {
            Coupon coupon = saveCoupon(DiscountType.FIXED_AMOUNT, 1000, null);

            AdminCouponReqDTO.IssueCouponReq request = new AdminCouponReqDTO.IssueCouponReq(
                    AdminCouponReqDTO.IssueType.CODE_GEN, null, null, null, "쿠폰코드", null);

            assertThatThrownBy(() -> adminCouponCommandService.issueCoupon(coupon.getId(), request))
                    .isInstanceOf(CouponAdminException.class)
                    .satisfies(e -> assertThat(((CouponAdminException) e).getReason())
                            .isEqualTo(CouponAdminErrorReason.INVALID_ISSUE_REQUEST));
        }

        @Test
        @DisplayName("maxUses를 지정하면 발급 건수는 코드 개수가 아니라 코드 개수 * maxUses로 집계된다")
        void issuesExplicitCodeWithMaxUses() {
            Coupon coupon = saveCoupon(DiscountType.FIXED_AMOUNT, 1000, null);

            AdminCouponReqDTO.IssueCouponReq request = new AdminCouponReqDTO.IssueCouponReq(
                    AdminCouponReqDTO.IssueType.CODE_GEN, null, null, null, "share100", 100);

            AdminCouponResDTO.IssueResultDTO result = adminCouponCommandService.issueCoupon(coupon.getId(), request);

            assertThat(result.issuedCount()).isEqualTo(100);
            Coupon reloaded = couponRepository.findById(coupon.getId()).orElseThrow();
            assertThat(reloaded.getIssuedCount()).isEqualTo(100);
        }

        @Test
        @DisplayName("이미 존재하는 코드를 직접 입력하면 예외가 발생한다")
        void throwsWhenExplicitCodeAlreadyExists() {
            Coupon coupon = saveCoupon(DiscountType.FIXED_AMOUNT, 1000, null);
            adminCouponCommandService.issueCoupon(coupon.getId(), new AdminCouponReqDTO.IssueCouponReq(
                    AdminCouponReqDTO.IssueType.CODE_GEN, null, null, null, "DUPCODE1", null));

            AdminCouponReqDTO.IssueCouponReq duplicate = new AdminCouponReqDTO.IssueCouponReq(
                    AdminCouponReqDTO.IssueType.CODE_GEN, null, null, null, "DUPCODE1", null);

            assertThatThrownBy(() -> adminCouponCommandService.issueCoupon(coupon.getId(), duplicate))
                    .isInstanceOf(CouponAdminException.class)
                    .satisfies(e -> assertThat(((CouponAdminException) e).getReason())
                            .isEqualTo(CouponAdminErrorReason.DUPLICATE_COUPON_CODE));
        }

        @Test
        @DisplayName("발급 수량 제한을 초과하면 예외가 발생하고 아무것도 발급되지 않는다")
        void throwsWhenIssueLimitExceeded() {
            Coupon coupon = saveCoupon(DiscountType.FIXED_AMOUNT, 1000, 1);
            Member m1 = saveMember("limit-1");
            Member m2 = saveMember("limit-2");

            AdminCouponReqDTO.IssueCouponReq request = new AdminCouponReqDTO.IssueCouponReq(
                    AdminCouponReqDTO.IssueType.SPECIFIC, List.of(m1.getNickname(), m2.getNickname()), null, null, null, null);

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
                    new AdminCouponReqDTO.IssueCouponReq(AdminCouponReqDTO.IssueType.ALL, null, null, null, null, null);

            assertThatThrownBy(() -> adminCouponCommandService.issueCoupon(coupon.getId(), request))
                    .isInstanceOf(CouponException.class)
                    .satisfies(e -> assertThat(((CouponException) e).getReason())
                            .isEqualTo(CouponErrorReason.COUPON_NOT_USABLE));
        }

        @Test
        @DisplayName("이미 해당 쿠폰을 보유한 회원은 다시 발급되지 않는다 (재클릭/재시도 대비)")
        void skipsMembersAlreadyHoldingTheCoupon() {
            Coupon coupon = saveCoupon(DiscountType.FIXED_AMOUNT, 1000, null);
            Member m1 = saveMember("dup-1");
            Member m2 = saveMember("dup-2");

            AdminCouponReqDTO.IssueCouponReq firstRequest = new AdminCouponReqDTO.IssueCouponReq(
                    AdminCouponReqDTO.IssueType.SPECIFIC, List.of(m1.getNickname()), null, null, null, null);
            adminCouponCommandService.issueCoupon(coupon.getId(), firstRequest);

            AdminCouponReqDTO.IssueCouponReq retryRequest = new AdminCouponReqDTO.IssueCouponReq(
                    AdminCouponReqDTO.IssueType.SPECIFIC, List.of(m1.getNickname(), m2.getNickname()), null, null, null, null);
            AdminCouponResDTO.IssueResultDTO result = adminCouponCommandService.issueCoupon(coupon.getId(), retryRequest);

            assertThat(result.issuedCount()).isEqualTo(1);
            assertThat(memberCouponRepository.findByMemberOrderByCreatedAtDesc(m1)).hasSize(1);
            assertThat(memberCouponRepository.findByMemberOrderByCreatedAtDesc(m2)).hasSize(1);
        }

        @Test
        @DisplayName("이미 발급 한도에 도달해도 기존에 발급받은 쿠폰은 결제 등에서 계속 사용할 수 있다")
        void issuedCouponsRemainUsableAfterLimitReached() {
            Coupon coupon = saveCoupon(DiscountType.FIXED_AMOUNT, 1000, 1);
            Member m1 = saveMember("limit-usable-1");

            adminCouponCommandService.issueCoupon(coupon.getId(),
                    new AdminCouponReqDTO.IssueCouponReq(AdminCouponReqDTO.IssueType.SPECIFIC, List.of(m1.getNickname()), null, null, null, null));

            Coupon reloaded = couponRepository.findById(coupon.getId()).orElseThrow();
            assertThat(reloaded.getIssuedCount()).isEqualTo(reloaded.getTotalIssueLimit());
            assertThat(reloaded.isUsable()).isTrue();
            assertThat(reloaded.isIssuable()).isFalse();
        }

        @Test
        @DisplayName("CODE_GEN 방식은 코드 자릿수/개수 상한을 초과하면 예외가 발생한다")
        void throwsWhenCodeGenExceedsUpperBound() {
            Coupon coupon = saveCoupon(DiscountType.FIXED_AMOUNT, 1000, null);

            AdminCouponReqDTO.IssueCouponReq tooLong =
                    new AdminCouponReqDTO.IssueCouponReq(AdminCouponReqDTO.IssueType.CODE_GEN, null, 21, 1, null, null);
            assertThatThrownBy(() -> adminCouponCommandService.issueCoupon(coupon.getId(), tooLong))
                    .isInstanceOf(CouponAdminException.class)
                    .satisfies(e -> assertThat(((CouponAdminException) e).getReason())
                            .isEqualTo(CouponAdminErrorReason.INVALID_ISSUE_REQUEST));

            AdminCouponReqDTO.IssueCouponReq tooMany =
                    new AdminCouponReqDTO.IssueCouponReq(AdminCouponReqDTO.IssueType.CODE_GEN, null, 8, 1001, null, null);
            assertThatThrownBy(() -> adminCouponCommandService.issueCoupon(coupon.getId(), tooMany))
                    .isInstanceOf(CouponAdminException.class)
                    .satisfies(e -> assertThat(((CouponAdminException) e).getReason())
                            .isEqualTo(CouponAdminErrorReason.INVALID_ISSUE_REQUEST));
        }
    }
}
