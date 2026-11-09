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

/**
 * 보관 기간이 끝난 탈퇴 데이터를 지운다(01-1 FUNC-MEM-007). 탈퇴일은 member_status_history의 자진 탈퇴 행 시각이다.
 * 단계마다 따로 트랜잭션을 쓰고, 실패한 건은 조건이 그대로라 다음 날 다시 잡힌다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WithdrawalRetentionPurgeService {

    static final int MATCHING_RETENTION_YEARS = 1;
    static final int PAYMENT_RETENTION_YEARS = 5;

    // 끝난(더 바뀌지 않는) 환불만 5년 기산 대상이다. 신청·진행·불명은 아직 처리 중이다
    private static final Set<RefundStatus> TERMINAL_REFUND = EnumSet.of(RefundStatus.COMPLETED, RefundStatus.FAILED);

    private final MemberStatusHistoryRepository memberStatusHistoryRepository;
    private final MatchingRepository matchingRepository;
    private final ProjectRepository projectRepository;
    private final MemberRestrictionRepository memberRestrictionRepository;
    private final ComplaintRepository complaintRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentRefundRepository paymentRefundRepository;

    /**
     * 1. 매칭 이력(대표 결정 10-28). 한쪽이 탈퇴하고 1년이 지나면 그 탈퇴자가 쓴 content만 비운다
     * (지원은 개발자가, 제안은 PM이 쓴 것). 양쪽 모두 탈퇴했으면 나중에 탈퇴한 쪽 기준 1년이 지나면 행을 지운다.
     *
     * @return [내용을 비운 건수, 지운 건수]
     */
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

    /** 2. 이름만 남긴 프로젝트(DELETED)를 가리키는 매칭이 0건이면 행을 지운다. */
    @Transactional
    public int purgeOrphanDeletedProjects() {
        List<Project> projects = projectRepository.findDeletedWithoutMatching();
        projectRepository.deleteAll(projects);
        return projects.size();
    }

    /**
     * 3. 만료된 최소식별정보(결정 C)를 지운다. 같은 회원에 대해 남겨 둔 상태 변경 이력(자진 탈퇴 행 제외)을 지우고
     * 신고 기록의 신고자·피신고자 칸을 비운다.
     */
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

    /** 4. 탈퇴 회원의 결제·환불 기록을 결제일과 환불 완료일이 모두 5년이 지나면 지운다(방침 3조 6)·9)). */
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

    // 자진 탈퇴 행이 없는 탈퇴(관리자 강제탈퇴 확정 등)는 기준일이 없어 이 배치 대상이 아니다
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
