package com.umc.devine.domain.member.service.command;

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
import com.umc.devine.domain.member.entity.WithdrawalReasonStat;
import com.umc.devine.domain.member.exception.MemberException;
import com.umc.devine.domain.member.exception.code.MemberErrorReason;
import com.umc.devine.domain.member.helper.GithubDataCleanupHelper;
import com.umc.devine.domain.member.helper.PendingMatchingHelper;
import com.umc.devine.domain.member.repository.ContactRepository;
import com.umc.devine.domain.member.repository.MemberAgreementRepository;
import com.umc.devine.domain.member.repository.MemberRepository;
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
import com.umc.devine.infrastructure.outbox.OutboxTaskWriter;
import com.umc.devine.infrastructure.outbox.payload.MemberExternalCleanupPayload;
import com.umc.devine.infrastructure.outbox.payload.WithdrawalRefundMailPayload;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final EntityManager entityManager;

    @Override
    public WithdrawalResDTO.CancelPendingResultDTO cancelPendingMatchings(Member member) {
        // 매칭 처리·마감과 같은 순서로 프로젝트를 먼저 잠근다. ID 순으로 잠가 교착을 피한다.
        pendingMatchingHelper.findPendingProjectIds(member).stream()
                .sorted()
                .forEach(projectRepository::findByIdForUpdate);

        // 잠근 뒤 다시 읽어, 그사이 상대가 수락·거절한 건은 건드리지 않는다
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
        LocalDateTime now = LocalDateTime.now();

        // 1. 사전 확인. 회원 행을 잠가 중복 요청을 차례로 처리하고, 잠근 뒤의 상태로 판단한다
        Member member = lockMember(current);
        List<Project> ownedProjects = projectRepository.findAllOwnedNotDeletedForUpdate(member);
        validateNoPending(member);

        // 2. 트랜잭션 전 준비: 환불 계산은 화면에서 받은 값을 믿지 않고 다시 한다
        // TODO(FUNC-MEM-006): 결정 C 대상·플래그 ON이면 Clerk에서 가입 이메일을 받아 해시를 만들어 둔다
        CreditRefundCalculator.Result refundPlan = withdrawalRefundPlanner.plan(member, now);
        boolean refundRequested = Boolean.TRUE.equals(request.refundRequested()) && refundPlan.refundableCredits() > 0;
        if (refundRequested && !StringUtils.hasText(request.refundContactEmail())) {
            throw new MemberException(MemberErrorReason.REFUND_CONTACT_EMAIL_REQUIRED);
        }

        // 아웃박스에 담을 값은 회원 행을 비우기 전에 읽어 둔다
        String clerkId = member.getClerkId();

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
        clearOperationalRecords(member);

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

    /** 회원·채팅·알림·쿠폰 그룹. 지울 S3 키를 돌려준다. */
    private List<String> deleteMemberData(Member member) {
        contactRepository.deleteAllByMember(member);
        memberAgreementRepository.deleteAllByMemberBulk(member);
        devTechstackRepository.deleteAllByMemberBulk(member);

        // 이 회원이 한 북마크 + 남이 이 회원을 북마크한 행. 이 회원 프로젝트의 북마크는 프로젝트 삭제 함수가 지웠다
        bookmarkRepository.deleteAllByMemberBulk(member);
        bookmarkRepository.deleteAllByTarget(BookmarkType.DEVELOPER, member.getId());

        // 프로필·프로젝트·에디터 이미지. 행은 지우고 S3 객체는 커밋 뒤 아웃박스로 지운다
        List<Image> images = imageRepository.findAllByUploader(member);
        List<String> s3Keys = images.stream()
                .map(Image::getS3Key)
                .filter(StringUtils::hasText)
                .toList();
        imageRepository.deleteAll(images);

        chatMessageRepository.deleteAllBySender(member);
        chatRoomRepository.findAllByParticipant(member).forEach(room -> leave(room, member));

        notificationRepository.deleteAllByReceiver(member);
        memberCouponRepository.deleteAllByMemberBulk(member);
        return s3Keys;
    }

    private void leave(ChatRoom room, Member member) {
        if (room.isActiveMember(member.getId())) {
            room.leave(member.getId());
        }
    }

    /** 운영 그룹: 상태 변경 이력, 신고 기록의 식별 칸. */
    private void clearOperationalRecords(Member member) {
        // TODO(FUNC-MEM-006): 결정 C 대상·플래그 ON이면 1년 동안 기존 이력과 신고 식별 칸을 남긴다
        memberStatusHistoryRepository.deleteAllByMemberBulk(member);
        memberStatusHistoryRepository.clearProcessor(member);

        complaintRepository.clearComplainant(member);
        complaintRepository.clearRespondent(member);
        complaintRepository.clearResolver(member);
        complaintHistoryRepository.clearResolver(member);
    }
}
