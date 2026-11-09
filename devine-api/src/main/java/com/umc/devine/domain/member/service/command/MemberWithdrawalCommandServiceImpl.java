package com.umc.devine.domain.member.service.command;

import com.umc.devine.admin.complaint.entity.Complaint;
import com.umc.devine.admin.complaint.enums.ComplaintStatus;
import com.umc.devine.admin.complaint.repository.ComplaintHistoryRepository;
import com.umc.devine.admin.complaint.repository.ComplaintRepository;
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
        // 매칭 처리/마감과 같은 순서로 프로젝트를 먼저 잠근다. ID 순으로 잠가 교착을 피한다.
        pendingMatchingHelper.findPendingProjectIds(member).stream()
                .sorted()
                .forEach(projectRepository::findByIdForUpdate);

        // 잠근 뒤 다시 읽어, 그사이 상대가 수락/거절한 건은 건드리지 않는다
        List<Matching> pending = pendingMatchingHelper.findPending(member);

        Map<Long, Project> touchedProjects = new LinkedHashMap<>();
        for (Matching matching : pending) {
            matching.cancel();
            notificationHelper.notifyPendingCancelled(matching, member, PendingMatchingHelper.counterpartOf(matching, member));
            touchedProjects.putIfAbsent(matching.getProject().getId(), matching.getProject());
        }

        // 03 마감 확인 훅: 마감일 지남 상태에서 대기 건이 0이 되면 마감한다
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

        // 1. 사전 확인. 회원 행을 잠가 중복 요청을 차례로 처리하고, 잠근 뒤의 상태로 판단한다
        Member member = lockMember(current);
        List<Project> ownedProjects = projectRepository.findAllOwnedNotDeletedForUpdate(member);
        validateNoPending(member);

        // 결정 C(FUNC-MEM-006): 대상이고 플래그가 켜져 있고 해시를 만들었으면 1년 보관한다. 해시가 없으면 일반 탈퇴로 처리한다
        boolean sanctionedTarget = withdrawalProperties.isRetainSanctionedMember() && member.isSanctioned();
        boolean retainSanctionRecords = sanctionedTarget && preparation.emailHash() != null;
        if (sanctionedTarget && !retainSanctionRecords) {
            alertSanctionHashMissing(member);
        }

        // 2. 환불 계산은 화면에서 받은 값을 믿지 않고 다시 한다
        CreditRefundCalculator.Result refundPlan = withdrawalRefundPlanner.plan(member, now);
        boolean refundRequested = Boolean.TRUE.equals(request.refundRequested()) && refundPlan.refundableCredits() > 0;
        if (refundRequested && !StringUtils.hasText(request.refundContactEmail())) {
            throw new MemberException(MemberErrorReason.REFUND_CONTACT_EMAIL_REQUIRED);
        }

        // 아웃박스에 담을 값은 회원 행을 비우기 전에 읽어 둔다
        String clerkId = member.getClerkId();

        // 3. 한 트랜잭션: 결정 C 보관(FUNC-MEM-006 2번). 사유/이력은 아래에서 지우기 전에 읽는다
        if (retainSanctionRecords) {
            retainSanctionInfo(member, preparation.emailHash(), now);
        }

        // 3. 한 트랜잭션: 환불 신청(FUNC-MEM-004 1번)과 생성권 소멸
        List<Long> refundIds = refundRequested
                ? requestRefunds(refundPlan, request.refundContactEmail().trim())
                : List.of();
        memberReportCreditRepository.findByMemberForUpdate(member)
                .ifPresent(credit -> credit.forfeitAll());

        // 3. 한 트랜잭션: 01-1 기능명세서 5장 테이블별 처리
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

        // 커밋 뒤 할 일(FUNC-MEM-004 2번, FUNC-MEM-005)
        if (!refundIds.isEmpty()) {
            outboxTaskWriter.enqueue(OutboxTaskType.WITHDRAWAL_REFUND_MAIL, new WithdrawalRefundMailPayload(refundIds));
        }
        outboxTaskWriter.enqueue(OutboxTaskType.GITHUB_GRANT_REVOKE, new MemberExternalCleanupPayload(clerkId, s3Keys));

        // 5. 탈퇴 사유는 회원 ID 없이 사유와 날짜만 남긴다(통계용)
        if (request.reason() != null) {
            withdrawalReasonStatRepository.save(WithdrawalReasonStat.builder()
                    .reason(request.reason())
                    .withdrawnOn(LocalDate.now(SERVICE_ZONE))
                    .build());
        }

        // 감사 칼럼의 clerk_id는 회원 행의 마지막 변경을 flush한 뒤 비운다. 이 뒤로는 회원 행을 바꾸지 않는다
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
        // 인증 단계에서 읽은 엔티티가 영속성 컨텍스트에 남아 있을 수 있어, 잠근 뒤 다시 읽는다
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

    /** 회원, 채팅, 알림, 쿠폰 그룹. 지울 S3 키를 돌려준다. */
    private List<String> deleteMemberData(Member member) {
        contactRepository.deleteAllByMember(member);
        memberAgreementRepository.deleteAllByMemberBulk(member);
        devTechstackRepository.deleteAllByMemberBulk(member);

        // 이 회원이 한 북마크 + 남이 이 회원을 북마크한 행. 이 회원 프로젝트의 북마크는 프로젝트 삭제 함수가 지웠다
        bookmarkRepository.deleteAllByMemberBulk(member);
        bookmarkRepository.deleteAllByTarget(BookmarkType.DEVELOPER, member.getId());

        // 프로필, 프로젝트, 에디터 이미지. 행은 지우고 S3 객체는 커밋 뒤 아웃박스로 지운다
        List<Image> images = imageRepository.findAllByUploader(member);
        List<String> s3Keys = images.stream()
                .map(Image::getS3Key)
                .filter(StringUtils::hasText)
                .toList();
        imageRepository.deleteAll(images);

        chatMessageRepository.deleteAllBySender(member);
        chatRoomRepository.findAllByParticipant(member).forEach(room -> leave(room, member));

        notificationRepository.deleteAllByReceiver(member);
        // 상대가 받은 알림 본문의 "{닉네임}님"을 바꾼다. 회원 행을 익명화하기 전에 원래 닉네임으로 찾는다
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

    /**
     * 운영 그룹: 상태 변경 이력, 신고 기록의 식별 칸.
     * 결정 C로 보관하면 이 회원의 기존 이력과 신고 식별 칸을 1년 남기고, 만료되면 FUNC-MEM-007 배치가 정리한다.
     * 이 회원이 처리자(관리자)로 남은 칸은 보관과 상관없이 비운다.
     */
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

    /** 결정 C(FUNC-MEM-006 2번): 이메일 해시, 사유(정지/자격상실 사유), 신고/제재 이력 요약, 만료일(탈퇴일 + 1년). */
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

    // 탈퇴를 늦추지 않는다(약관 제7조 8항): 해시 없이 일반 탈퇴로 진행하고 커밋 뒤 운영자에게 알린다
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
