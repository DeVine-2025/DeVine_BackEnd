package com.umc.devine.domain.member.service.command;

import com.umc.devine.domain.complaint.entity.Complaint;
import com.umc.devine.domain.complaint.enums.ComplaintStatus;
import com.umc.devine.domain.complaint.repository.ComplaintHistoryRepository;
import com.umc.devine.domain.complaint.repository.ComplaintRepository;
import com.umc.devine.admin.member.entity.MemberStatusHistory;
import com.umc.devine.admin.member.enums.MemberStatusAction;
import com.umc.devine.admin.member.repository.MemberStatusHistoryRepository;
import com.umc.devine.domain.auth.exception.AuthException;
import com.umc.devine.domain.auth.exception.code.AuthErrorReason;
import com.umc.devine.domain.bookmark.enums.BookmarkType;
import com.umc.devine.domain.bookmark.repository.BookmarkRepository;
import com.umc.devine.domain.chat.entity.ChatRoom;
import com.umc.devine.domain.chat.repository.ChatMessageRepository;
import com.umc.devine.domain.chat.repository.ChatRoomRepository;
import com.umc.devine.domain.coupon.repository.MemberCouponRepository;
import com.umc.devine.domain.image.entity.Image;
import com.umc.devine.domain.image.repository.ImageRepository;
import com.umc.devine.domain.member.dto.WithdrawalReqDTO;
import com.umc.devine.domain.member.dto.WithdrawalResDTO;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.entity.MemberRestriction;
import com.umc.devine.domain.member.entity.WithdrawalReasonStat;
import com.umc.devine.domain.member.exception.MemberException;
import com.umc.devine.domain.member.exception.code.MemberErrorReason;
import com.umc.devine.domain.member.helper.GithubDataCleanupHelper;
import com.umc.devine.domain.member.helper.PendingMatchingHelper;
import com.umc.devine.domain.member.helper.WithdrawalProperties;
import com.umc.devine.domain.member.repository.ContactRepository;
import com.umc.devine.domain.member.repository.MemberAgreementRepository;
import com.umc.devine.domain.member.repository.MemberRepository;
import com.umc.devine.domain.member.repository.MemberRestrictionRepository;
import com.umc.devine.domain.member.repository.WithdrawalReasonStatRepository;
import com.umc.devine.domain.notification.repository.NotificationRepository;
import com.umc.devine.domain.outbox.enums.OutboxTaskType;
import com.umc.devine.domain.payment.entity.PaymentRefund;
import com.umc.devine.domain.payment.repository.PaymentRefundRepository;
import com.umc.devine.domain.payment.repository.PaymentRepository;
import com.umc.devine.domain.project.entity.Project;
import com.umc.devine.domain.project.entity.mapping.Matching;
import com.umc.devine.domain.project.helper.MatchingNotificationHelper;
import com.umc.devine.domain.project.helper.ProjectDeletionHelper;
import com.umc.devine.domain.project.helper.ProjectLifecycleHelper;
import com.umc.devine.domain.project.repository.ProjectRepository;
import com.umc.devine.domain.techstack.repository.DevTechstackRepository;
import com.umc.devine.domain.ticket.entity.MemberReportCredit;
import com.umc.devine.domain.ticket.helper.CreditRefundCalculator;
import com.umc.devine.domain.ticket.helper.WithdrawalRefundPlanner;
import com.umc.devine.domain.ticket.repository.MemberReportCreditRepository;
import com.umc.devine.infrastructure.email.EmailNotificationEvent;
import com.umc.devine.infrastructure.outbox.OutboxTaskWriter;
import com.umc.devine.infrastructure.outbox.payload.MemberExternalCleanupPayload;
import com.umc.devine.infrastructure.outbox.payload.WithdrawalRefundMailPayload;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class MemberWithdrawalCommandServiceImpl implements MemberWithdrawalCommandService {

    private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");
    private static final int SANCTION_RETENTION_YEARS = 1;

    private final PendingMatchingHelper pendingMatchingHelper;
    private final ProjectRepository projectRepository;
    private final MatchingNotificationHelper notificationHelper;
    private final ProjectLifecycleHelper lifecycleHelper;
    private final ProjectDeletionHelper projectDeletionHelper;
    private final GithubDataCleanupHelper githubDataCleanupHelper;
    private final WithdrawalRefundPlanner withdrawalRefundPlanner;
    private final OutboxTaskWriter outboxTaskWriter;

    private final MemberRepository memberRepository;
    private final ContactRepository contactRepository;
    private final MemberAgreementRepository memberAgreementRepository;
    private final DevTechstackRepository devTechstackRepository;
    private final BookmarkRepository bookmarkRepository;
    private final ImageRepository imageRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final NotificationRepository notificationRepository;
    private final MemberCouponRepository memberCouponRepository;
    private final MemberReportCreditRepository memberReportCreditRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentRefundRepository paymentRefundRepository;
    private final MemberStatusHistoryRepository memberStatusHistoryRepository;
    private final ComplaintRepository complaintRepository;
    private final ComplaintHistoryRepository complaintHistoryRepository;
    private final WithdrawalReasonStatRepository withdrawalReasonStatRepository;
    private final MemberRestrictionRepository memberRestrictionRepository;
    private final WithdrawalProperties withdrawalProperties;
    private final ApplicationEventPublisher eventPublisher;
    private final EntityManager entityManager;

    @Override
    public WithdrawalResDTO.CancelPendingResultDTO cancelPendingMatchings(Member member) {
        pendingMatchingHelper.findPendingProjectIds(member).stream()
                .sorted()
                .forEach(projectRepository::findByIdForUpdate);

        List<Matching> pending = pendingMatchingHelper.findPending(member);

        Map<Long, Project> touchedProjects = new LinkedHashMap<>();
        for (Matching matching : pending) {
            matching.cancel();
            notificationHelper.notifyPendingCancelled(matching, member, PendingMatchingHelper.counterpartOf(matching, member));
            touchedProjects.putIfAbsent(matching.getProject().getId(), matching.getProject());
        }

        touchedProjects.values().forEach(lifecycleHelper::closeIfDeadlinePassedAndNoPending);

        return WithdrawalResDTO.CancelPendingResultDTO.builder()
                .canceledCount(pending.size())
                .build();
    }

    @Override
    public WithdrawalResDTO.WithdrawResultDTO withdraw(Member current, WithdrawalReqDTO.WithdrawReq request) {
        return withdraw(current, request, WithdrawalPreparation.none());
    }

    @Override
    public WithdrawalResDTO.WithdrawResultDTO withdraw(Member current, WithdrawalReqDTO.WithdrawReq request,
                                                       WithdrawalPreparation preparation) {
        LocalDateTime now = LocalDateTime.now();

        Member member = lockMember(current);
        List<Project> ownedProjects = projectRepository.findAllOwnedNotDeletedForUpdate(member);
        validateNoPending(member);

        boolean sanctionedTarget = withdrawalProperties.isRetainSanctionedMember() && member.isSanctioned();
        boolean retainSanctionRecords = sanctionedTarget && preparation.emailHash() != null;
        if (sanctionedTarget && !retainSanctionRecords) {
            alertSanctionHashMissing(member);
        }

        paymentRepository.findAllByMemberForUpdate(member);
        Optional<MemberReportCredit> lockedCredit = memberReportCreditRepository.findByMemberForUpdate(member);
        CreditRefundCalculator.Result refundPlan = withdrawalRefundPlanner.plan(member, now);
        boolean refundRequested = Boolean.TRUE.equals(request.refundRequested()) && refundPlan.refundableCredits() > 0;
        if (refundRequested && !StringUtils.hasText(request.refundContactEmail())) {
            throw new MemberException(MemberErrorReason.REFUND_CONTACT_EMAIL_REQUIRED);
        }

        String clerkId = member.getClerkId();

        if (retainSanctionRecords) {
            retainSanctionInfo(member, preparation.emailHash(), now);
        }

        List<Long> refundIds = refundRequested
                ? requestRefunds(refundPlan, request.refundContactEmail().trim())
                : List.of();
        lockedCredit.ifPresent(MemberReportCredit::forfeitAll);

        ownedProjects.forEach(projectDeletionHelper::delete);
        githubDataCleanupHelper.deleteAll(member);
        List<String> s3Keys = deleteMemberData(member);
        clearOperationalRecords(member, retainSanctionRecords);

        member.anonymizeForWithdrawal();
        memberStatusHistoryRepository.save(MemberStatusHistory.builder()
                .member(member)
                .action(MemberStatusAction.SELF_WITHDRAW)
                .status(member.getUsed())
                .build());

        if (!refundIds.isEmpty()) {
            outboxTaskWriter.enqueue(OutboxTaskType.WITHDRAWAL_REFUND_MAIL, new WithdrawalRefundMailPayload(refundIds));
        }
        outboxTaskWriter.enqueue(OutboxTaskType.GITHUB_GRANT_REVOKE, new MemberExternalCleanupPayload(clerkId, s3Keys));

        if (request.reason() != null) {
            withdrawalReasonStatRepository.save(WithdrawalReasonStat.builder()
                    .reason(request.reason())
                    .withdrawnOn(LocalDate.now(SERVICE_ZONE))
                    .build());
        }

        memberRepository.clearAuditColumns(member.getId());

        log.info("[MemberWithdrawal] 탈퇴 완료 - memberId: {}, refundRequests: {}", member.getId(), refundIds.size());

        return WithdrawalResDTO.WithdrawResultDTO.builder()
                .withdrawn(true)
                .refundRequested(refundRequested)
                .build();
    }

    private Member lockMember(Member current) {
        Member member = memberRepository.findByIdForUpdate(current.getId())
                .orElseThrow(() -> new AuthException(AuthErrorReason.NOT_REGISTERED));
        entityManager.refresh(member);
        if (member.isWithdrawn()) {
            throw new AuthException(AuthErrorReason.NOT_REGISTERED);
        }
        return member;
    }

    private void validateNoPending(Member member) {
        int pendingCount = pendingMatchingHelper.findPending(member).size();
        if (pendingCount > 0) {
            throw new MemberException(MemberErrorReason.WITHDRAWAL_PENDING_MATCHINGS_EXIST,
                    WithdrawalResDTO.PendingMatchingsRemainDTO.builder().pendingCount(pendingCount).build());
        }
    }

    private List<Long> requestRefunds(CreditRefundCalculator.Result refundPlan, String contactEmail) {
        List<Long> refundIds = new ArrayList<>();
        for (CreditRefundCalculator.Allocation allocation : refundPlan.allocations()) {
            PaymentRefund refund = paymentRefundRepository.save(PaymentRefund.requestForWithdrawal(
                    paymentRepository.getReferenceById(allocation.purchase().paymentId()),
                    allocation.amount(),
                    allocation.credits(),
                    contactEmail));
            refundIds.add(refund.getId());
        }
        return refundIds;
    }

    private List<String> deleteMemberData(Member member) {
        contactRepository.deleteAllByMember(member);
        memberAgreementRepository.deleteAllByMemberBulk(member);
        devTechstackRepository.deleteAllByMemberBulk(member);

        bookmarkRepository.deleteAllByMemberBulk(member);
        bookmarkRepository.deleteAllByTarget(BookmarkType.DEVELOPER, member.getId());

        List<Image> images = imageRepository.findAllByUploader(member);
        List<String> s3Keys = images.stream()
                .map(Image::getS3Key)
                .filter(StringUtils::hasText)
                .toList();
        imageRepository.deleteAll(images);

        chatMessageRepository.deleteAllBySender(member);
        chatRoomRepository.findAllByParticipant(member).forEach(room -> leave(room, member));

        notificationRepository.deleteAllByReceiver(member);
        notificationRepository.replaceSenderNameInContent(member,
                member.getNickname() + "님", Member.WITHDRAWN_DISPLAY_NAME + "님");
        memberCouponRepository.deleteAllByMemberBulk(member);
        return s3Keys;
    }

    private void leave(ChatRoom room, Member member) {
        if (room.isActiveMember(member.getId())) {
            room.leave(member.getId());
        }
    }

    private void clearOperationalRecords(Member member, boolean retainSanctionRecords) {
        if (!retainSanctionRecords) {
            memberStatusHistoryRepository.deleteAllByMemberBulk(member);
            complaintRepository.clearComplainant(member);
            complaintRepository.clearRespondent(member);
        }
        memberStatusHistoryRepository.clearProcessor(member);
        complaintRepository.clearResolver(member);
        complaintHistoryRepository.clearResolver(member);
    }

    private void retainSanctionInfo(Member member, String emailHash, LocalDateTime now) {
        List<MemberStatusHistory> histories = memberStatusHistoryRepository.findByMemberIdOrderByCreatedAtDesc(member.getId());
        String reason = histories.stream()
                .filter(h -> h.getAction() == MemberStatusAction.SUSPEND || h.getAction() == MemberStatusAction.FORCE_WITHDRAW)
                .map(MemberStatusHistory::getReason)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse(null);

        memberRestrictionRepository.save(MemberRestriction.builder()
                .member(member)
                .emailHash(emailHash)
                .reason(reason)
                .sanctionSummary(sanctionSummary(member, histories))
                .expiresAt(now.plusYears(SANCTION_RETENTION_YEARS))
                .build());
    }

    private String sanctionSummary(Member member, List<MemberStatusHistory> histories) {
        String sanctions = histories.stream()
                .map(h -> "%s(%s)".formatted(h.getAction(), h.getCreatedAt() == null ? "-" : h.getCreatedAt().toLocalDate()))
                .collect(java.util.stream.Collectors.joining(", "));
        Map<ComplaintStatus, Long> complaints = complaintRepository.findByRespondentMemberIdOrderByCreatedAtDesc(member.getId())
                .stream()
                .collect(java.util.stream.Collectors.groupingBy(Complaint::getStatus, java.util.stream.Collectors.counting()));
        return "제재 이력: [%s] / 피신고: %d건 %s".formatted(
                sanctions,
                complaints.values().stream().mapToLong(Long::longValue).sum(),
                complaints);
    }

    private void alertSanctionHashMissing(Member member) {
        log.warn("[MemberWithdrawal] 결정 C 대상인데 이메일 해시가 없어 일반 탈퇴로 처리 - memberId: {}", member.getId());
        eventPublisher.publishEvent(EmailNotificationEvent.builder()
                .to(withdrawalProperties.getOperatorEmail())
                .subject("[탈퇴] 정지 또는 소명 중인 회원의 재가입 제한 정보를 남기지 못했습니다")
                .body("""
                        정지 또는 자격상실 예정 회원이 탈퇴했지만 가입 이메일을 받지 못해 재가입 제한 정보를 남기지 못했습니다.<br>
                        탈퇴는 일반 탈퇴로 처리됐습니다(약관 제7조 8항).<br>
                        회원 ID: %d
                        """.formatted(member.getId()))
                .build());
    }
}
