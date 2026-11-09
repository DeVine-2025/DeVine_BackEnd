package com.umc.devine.domain.member.service.query;

import com.umc.devine.domain.member.dto.WithdrawalResDTO;
import com.umc.devine.domain.member.entity.Contact;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.enums.ContactType;
import com.umc.devine.domain.member.helper.PendingMatchingHelper;
import com.umc.devine.domain.member.repository.ContactRepository;
import com.umc.devine.domain.project.entity.mapping.Matching;
import com.umc.devine.domain.report.repository.DevReportRepository;
import com.umc.devine.domain.ticket.helper.CreditRefundCalculator;
import com.umc.devine.domain.ticket.helper.WithdrawalRefundPlanner;
import com.umc.devine.global.security.ClerkPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberWithdrawalQueryServiceImpl implements MemberWithdrawalQueryService {

    private final PendingMatchingHelper pendingMatchingHelper;
    private final DevReportRepository devReportRepository;
    private final WithdrawalRefundPlanner withdrawalRefundPlanner;
    private final ContactRepository contactRepository;

    @Override
    public WithdrawalResDTO.PreviewDTO getPreview(Member member, ClerkPrincipal principal) {
        List<WithdrawalResDTO.PendingMatchingDTO> pending = pendingMatchingHelper.findPending(member).stream()
                .map(matching -> toPendingDTO(matching, member))
                .toList();

        CreditRefundCalculator.Result refund = withdrawalRefundPlanner.plan(member, LocalDateTime.now());

        return WithdrawalResDTO.PreviewDTO.builder()
                .pendingMatchings(pending)
                .pendingCount(pending.size())
                .reportCount(devReportRepository.countCompletedMainReportsByMember(member))
                .generatingReport(devReportRepository.existsInProgressByMember(member))
                .remainingCredits(refund.remainingCredits())
                .refundableCredits(refund.refundableCredits())
                .nonRefundableCredits(refund.nonRefundableCredits())
                .refundAmount(refund.refundAmount())
                .refundContactEmail(findSignupEmail(member, principal))
                .build();
    }

    private WithdrawalResDTO.PendingMatchingDTO toPendingDTO(Matching matching, Member member) {
        return WithdrawalResDTO.PendingMatchingDTO.builder()
                .matchingId(matching.getId())
                .projectName(matching.getProject().getName())
                .counterpartNickname(PendingMatchingHelper.counterpartOf(matching, member).getNickname())
                .matchingType(matching.getMatchingType())
                .direction(PendingMatchingHelper.directionOf(matching, member))
                .build();
    }

    private String findSignupEmail(Member member, ClerkPrincipal principal) {
        if (principal != null && StringUtils.hasText(principal.getEmail())) {
            return principal.getEmail();
        }
        return contactRepository.findAllByMember(member).stream()
                .filter(contact -> contact.getContactType() == ContactType.EMAIL)
                .map(Contact::getValue)
                .findFirst()
                .orElse(null);
    }
}
