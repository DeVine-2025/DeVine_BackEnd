package com.umc.devine.admin.coupon.service.command;

import com.umc.devine.admin.coupon.converter.AdminCouponConverter;
import com.umc.devine.admin.coupon.dto.AdminCouponReqDTO;
import com.umc.devine.admin.coupon.dto.AdminCouponResDTO;
import com.umc.devine.admin.coupon.exception.CouponAdminException;
import com.umc.devine.admin.coupon.exception.code.CouponAdminErrorReason;
import com.umc.devine.domain.coupon.entity.Coupon;
import com.umc.devine.domain.coupon.entity.CouponCode;
import com.umc.devine.domain.coupon.entity.MemberCoupon;
import com.umc.devine.domain.coupon.enums.DiscountType;
import com.umc.devine.domain.coupon.exception.CouponException;
import com.umc.devine.domain.coupon.exception.code.CouponErrorReason;
import com.umc.devine.domain.coupon.repository.CouponCodeRepository;
import com.umc.devine.domain.coupon.repository.CouponRepository;
import com.umc.devine.domain.coupon.repository.MemberCouponRepository;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.exception.MemberException;
import com.umc.devine.domain.member.exception.code.MemberErrorReason;
import com.umc.devine.domain.member.repository.MemberRepository;
import com.umc.devine.domain.ticket.entity.TicketProduct;
import com.umc.devine.domain.ticket.exception.TicketException;
import com.umc.devine.domain.ticket.exception.code.TicketErrorReason;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminCouponCommandServiceImpl implements AdminCouponCommandService {

    private static final int CODE_GENERATION_MAX_RETRY = 5;
    private static final String CODE_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int CODE_LENGTH_MIN = 4;
    private static final int CODE_LENGTH_MAX = 20;
    private static final int CODE_COUNT_MAX = 1000;
    private static final int CODE_MAX_USES_MAX = 100_000;

    private final CouponRepository couponRepository;
    private final CouponCodeRepository couponCodeRepository;
    private final MemberCouponRepository memberCouponRepository;
    private final MemberRepository memberRepository;
    private final com.umc.devine.domain.ticket.repository.TicketProductRepository ticketProductRepository;
    private final SecureRandom random = new SecureRandom();

    @Override
    public AdminCouponResDTO.CouponDTO createCoupon(AdminCouponReqDTO.CreateCouponReq request) {
        if (!request.validFrom().isBefore(request.validUntil())) {
            throw new CouponException(CouponErrorReason.VALID_PERIOD_INVALID);
        }
        if (request.discountType() == DiscountType.FIXED_RATE
                && (request.discountValue() < 1 || request.discountValue() > 100)) {
            throw new CouponException(CouponErrorReason.DISCOUNT_VALUE_INVALID);
        }

        TicketProduct applicableTicketProduct = null;
        if (request.applicableTicketProductId() != null) {
            applicableTicketProduct = ticketProductRepository.findById(request.applicableTicketProductId())
                    .orElseThrow(() -> new TicketException(TicketErrorReason.PRODUCT_NOT_FOUND));
        }

        Coupon coupon = Coupon.builder()
                .name(request.name())
                .discountType(request.discountType())
                .discountValue(request.discountValue())
                .applicableTicketProduct(applicableTicketProduct)
                .totalIssueLimit(request.totalIssueLimit())
                .issuedCount(0)
                .usedCount(0)
                .validFrom(request.validFrom())
                .validUntil(request.validUntil())
                .isActive(true)
                .description(request.description())
                .build();

        return AdminCouponConverter.toCouponDTO(couponRepository.save(coupon));
    }

    @Override
    public AdminCouponResDTO.CouponDTO updateCoupon(Long couponId, AdminCouponReqDTO.UpdateCouponReq request) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new CouponException(CouponErrorReason.COUPON_NOT_FOUND));

        LocalDateTimeRange range = resolveValidPeriod(coupon, request);
        if (!range.from().isBefore(range.until())) {
            throw new CouponException(CouponErrorReason.VALID_PERIOD_INVALID);
        }

        coupon.update(
                request.name(),
                request.validFrom(),
                request.validUntil(),
                request.totalIssueLimit(),
                request.clearTotalIssueLimit(),
                request.isActive(),
                request.description()
        );

        return AdminCouponConverter.toCouponDTO(coupon);
    }

    private record LocalDateTimeRange(java.time.LocalDateTime from, java.time.LocalDateTime until) {}

    private LocalDateTimeRange resolveValidPeriod(Coupon coupon, AdminCouponReqDTO.UpdateCouponReq request) {
        var from = request.validFrom() != null ? request.validFrom() : coupon.getValidFrom();
        var until = request.validUntil() != null ? request.validUntil() : coupon.getValidUntil();
        return new LocalDateTimeRange(from, until);
    }

    @Override
    public AdminCouponResDTO.IssueResultDTO issueCoupon(Long couponId, AdminCouponReqDTO.IssueCouponReq request) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new CouponException(CouponErrorReason.COUPON_NOT_FOUND));

        if (!coupon.isIssuable()) {
            throw new CouponException(CouponErrorReason.COUPON_NOT_USABLE);
        }

        return switch (request.issueType()) {
            case ALL -> issueDirectly(coupon, memberRepository.findAllActive());
            case SPECIFIC -> issueToSpecific(coupon, request.nicknames());
            case CODE_GEN -> issueCodes(coupon, request.code(), request.codeLength(), request.codeCount(), request.maxUses());
        };
    }

    private AdminCouponResDTO.IssueResultDTO issueToSpecific(Coupon coupon, List<String> nicknames) {
        if (nicknames == null || nicknames.isEmpty()) {
            throw new CouponAdminException(CouponAdminErrorReason.INVALID_ISSUE_REQUEST);
        }
        List<Member> members = memberRepository.findAllByNicknameIn(nicknames);
        if (members.size() != nicknames.size()) {
            throw new MemberException(MemberErrorReason.NOT_FOUND);
        }
        return issueDirectly(coupon, members);
    }

    private AdminCouponResDTO.IssueResultDTO issueDirectly(Coupon coupon, List<Member> members) {
        if (members.isEmpty()) {
            return new AdminCouponResDTO.IssueResultDTO(0, null);
        }

        // 이미 이 쿠폰을 보유한 회원은 건너뛴다 — 관리자가 발급 버튼을 재클릭하거나 요청이 재시도되어도
        // 같은 회원에게 중복 발급되지 않도록 하기 위함.
        Set<Long> alreadyHoldingIds = memberCouponRepository.findMembersAlreadyHolding(coupon, members).stream()
                .map(Member::getId)
                .collect(java.util.stream.Collectors.toSet());
        List<Member> targets = members.stream()
                .filter(member -> !alreadyHoldingIds.contains(member.getId()))
                .toList();

        if (targets.isEmpty()) {
            return new AdminCouponResDTO.IssueResultDTO(0, null);
        }
        reserveIssueCount(coupon, targets.size());

        List<MemberCoupon> memberCoupons = targets.stream()
                .map(member -> MemberCoupon.issueTo(member, coupon))
                .toList();
        try {
            memberCouponRepository.saveAllAndFlush(memberCoupons);
        } catch (DataIntegrityViolationException e) {
            throw new CouponAdminException(CouponAdminErrorReason.CONCURRENT_ISSUE_CONFLICT);
        }

        return new AdminCouponResDTO.IssueResultDTO(targets.size(), null);
    }

    private AdminCouponResDTO.IssueResultDTO issueCodes(
            Coupon coupon, String explicitCode, Integer codeLength, Integer codeCount, Integer maxUses) {
        if (maxUses != null && (maxUses < 1 || maxUses > CODE_MAX_USES_MAX)) {
            throw new CouponAdminException(CouponAdminErrorReason.INVALID_ISSUE_REQUEST);
        }
        int perCodeCapacity = maxUses != null ? maxUses : 1;

        if (explicitCode != null && !explicitCode.isBlank()) {
            return issueExplicitCode(coupon, explicitCode, perCodeCapacity);
        }
        return issueGeneratedCodes(coupon, codeLength, codeCount, perCodeCapacity);
    }

    private AdminCouponResDTO.IssueResultDTO issueExplicitCode(
            Coupon coupon, String explicitCode, int perCodeCapacity) {
        String normalized = explicitCode.trim().toUpperCase();
        if (normalized.length() < CODE_LENGTH_MIN || normalized.length() > CODE_LENGTH_MAX
                || !normalized.matches("^[A-Z0-9]+$")) {
            throw new CouponAdminException(CouponAdminErrorReason.INVALID_ISSUE_REQUEST);
        }
        if (couponCodeRepository.existsByCode(normalized)) {
            throw new CouponAdminException(CouponAdminErrorReason.DUPLICATE_COUPON_CODE);
        }
        reserveIssueCount(coupon, perCodeCapacity);

        CouponCode couponCode;
        try {
            couponCode = couponCodeRepository.saveAndFlush(CouponCode.of(coupon, normalized, perCodeCapacity));
        } catch (DataIntegrityViolationException e) {
            throw new CouponAdminException(CouponAdminErrorReason.DUPLICATE_COUPON_CODE);
        }
        return new AdminCouponResDTO.IssueResultDTO(perCodeCapacity, List.of(couponCode.getCode()));
    }

    private AdminCouponResDTO.IssueResultDTO issueGeneratedCodes(
            Coupon coupon, Integer codeLength, Integer codeCount, int perCodeCapacity) {
        if (codeLength == null || codeLength < CODE_LENGTH_MIN || codeLength > CODE_LENGTH_MAX
                || codeCount == null || codeCount < 1 || codeCount > CODE_COUNT_MAX) {
            throw new CouponAdminException(CouponAdminErrorReason.INVALID_ISSUE_REQUEST);
        }
        reserveIssueCount(coupon, codeCount * perCodeCapacity);

        Set<String> generated = new HashSet<>();
        List<CouponCode> couponCodes = new ArrayList<>();
        for (int i = 0; i < codeCount; i++) {
            String code = generateUniqueCode(codeLength, generated);
            generated.add(code);
            couponCodes.add(CouponCode.of(coupon, code, perCodeCapacity));
        }
        couponCodeRepository.saveAll(couponCodes);

        return new AdminCouponResDTO.IssueResultDTO(
                codeCount * perCodeCapacity, couponCodes.stream().map(CouponCode::getCode).toList());
    }

    private void reserveIssueCount(Coupon coupon, int count) {
        if (couponRepository.incrementIssuedCountById(coupon.getId(), count) == 0) {
            throw new CouponException(CouponErrorReason.COUPON_ISSUE_LIMIT_EXCEEDED);
        }
    }

    private String generateUniqueCode(int length, Set<String> generatedInBatch) {
        for (int attempt = 0; attempt < CODE_GENERATION_MAX_RETRY; attempt++) {
            String candidate = randomCode(length);
            if (!generatedInBatch.contains(candidate) && !couponCodeRepository.existsByCode(candidate)) {
                return candidate;
            }
        }
        throw new CouponAdminException(CouponAdminErrorReason.CODE_GENERATION_FAILED);
    }

    private String randomCode(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CODE_ALPHABET.charAt(random.nextInt(CODE_ALPHABET.length())));
        }
        return sb.toString();
    }
}
