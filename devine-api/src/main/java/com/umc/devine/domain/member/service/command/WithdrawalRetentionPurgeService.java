package com.umc.devine.domain.member.service.command;

import com.umc.devine.admin.complaint.repository.ComplaintRepository;
import com.umc.devine.admin.member.enums.MemberStatusAction;
import com.umc.devine.admin.member.repository.MemberStatusHistoryRepository;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.entity.MemberRestriction;
import com.umc.devine.domain.member.repository.MemberRestrictionRepository;
import com.umc.devine.domain.payment.entity.Payment;
import com.umc.devine.domain.payment.entity.PaymentRefund;
import com.umc.devine.domain.payment.entity.Transaction;
import com.umc.devine.domain.payment.enums.RefundStatus;
import com.umc.devine.domain.payment.enums.TransactionStatus;
import com.umc.devine.domain.payment.enums.TransactionType;
import com.umc.devine.domain.payment.repository.PaymentRefundRepository;
import com.umc.devine.domain.payment.repository.PaymentRepository;
import com.umc.devine.domain.project.entity.Project;
import com.umc.devine.domain.project.entity.mapping.Matching;
import com.umc.devine.domain.project.repository.MatchingRepository;
import com.umc.devine.domain.project.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class WithdrawalRetentionPurgeService {

    static final int MATCHING_RETENTION_YEARS = 1;
    static final int PAYMENT_RETENTION_YEARS = 5;

    private static final Set<RefundStatus> TERMINAL_REFUND = EnumSet.of(RefundStatus.COMPLETED, RefundStatus.FAILED);

    private final MemberStatusHistoryRepository memberStatusHistoryRepository;
    private final MatchingRepository matchingRepository;
    private final ProjectRepository projectRepository;
    private final MemberRestrictionRepository memberRestrictionRepository;
    private final ComplaintRepository complaintRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentRefundRepository paymentRefundRepository;

    @Transactional
    public int[] purgeMatchingHistory(LocalDateTime now) {
        Map<Long, LocalDateTime> withdrawnAt = withdrawnAtByMember();
        LocalDateTime threshold = now.minusYears(MATCHING_RETENTION_YEARS);
        int cleared = 0;
        int deleted = 0;

        for (Matching matching : matchingRepository.findAllWithWithdrawnParticipant()) {
            LocalDateTime developerAt = withdrawnAtOf(matching.getMember(), withdrawnAt);
            LocalDateTime pmAt = withdrawnAtOf(matching.getProject().getMember(), withdrawnAt);

            if (developerAt != null && pmAt != null) {
                LocalDateTime later = developerAt.isAfter(pmAt) ? developerAt : pmAt;
                if (!later.isAfter(threshold)) {
                    matchingRepository.delete(matching);
                    deleted++;
                    continue;
                }
            }

            LocalDateTime writerAt = matching.isApplyType() ? developerAt : pmAt;
            if (writerAt != null && !writerAt.isAfter(threshold) && matching.getContent() != null) {
                matching.clearContent();
                cleared++;
            }
        }
        return new int[]{cleared, deleted};
    }

    @Transactional
    public int purgeOrphanDeletedProjects() {
        List<Project> projects = projectRepository.findDeletedWithoutMatching();
        projectRepository.deleteAll(projects);
        return projects.size();
    }

    @Transactional
    public int purgeExpiredRestrictions(LocalDateTime now) {
        List<MemberRestriction> expired = memberRestrictionRepository.findAllByExpiresAtBefore(now);
        for (MemberRestriction restriction : expired) {
            Member member = restriction.getMember();
            memberStatusHistoryRepository.deleteAllByMemberExceptAction(member, MemberStatusAction.SELF_WITHDRAW);
            complaintRepository.clearComplainant(member);
            complaintRepository.clearRespondent(member);
            memberRestrictionRepository.delete(restriction);
        }
        return expired.size();
    }

    @Transactional
    public int purgeExpiredPayments(LocalDateTime now) {
        LocalDateTime threshold = now.minusYears(PAYMENT_RETENTION_YEARS);
        int deleted = 0;
        for (Long paymentId : paymentRepository.findIdsOfWithdrawnMembersCreatedBefore(threshold)) {
            Payment payment = paymentRepository.findById(paymentId).orElse(null);
            if (payment == null || paidAt(payment).isAfter(threshold)) {
                continue;
            }
            List<PaymentRefund> refunds = paymentRefundRepository.findAllByPaymentIdIn(List.of(paymentId));
            boolean refundsExpired = refunds.stream().allMatch(refund ->
                    TERMINAL_REFUND.contains(refund.getStatus()) && !lastChangedAt(refund).isAfter(threshold));
            if (!refundsExpired) {
                continue;
            }
            paymentRefundRepository.deleteAll(refunds);
            paymentRepository.delete(payment);
            deleted++;
        }
        return deleted;
    }

    private Map<Long, LocalDateTime> withdrawnAtByMember() {
        Map<Long, LocalDateTime> result = new HashMap<>();
        for (Object[] row : memberStatusHistoryRepository.findLatestActionTimeByMember(MemberStatusAction.SELF_WITHDRAW)) {
            result.put((Long) row[0], (LocalDateTime) row[1]);
        }
        return result;
    }

    private static LocalDateTime withdrawnAtOf(Member member, Map<Long, LocalDateTime> withdrawnAt) {
        return member.isWithdrawn() ? withdrawnAt.get(member.getId()) : null;
    }

    private static LocalDateTime paidAt(Payment payment) {
        return payment.getTransactions().stream()
                .filter(t -> t.getType() == TransactionType.PAYMENT && t.getStatus() == TransactionStatus.PAID)
                .map(Transaction::getPaidAt)
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .orElse(payment.getCreatedAt());
    }

    private static LocalDateTime lastChangedAt(PaymentRefund refund) {
        return refund.getUpdatedAt() != null ? refund.getUpdatedAt() : refund.getCreatedAt();
    }
}
