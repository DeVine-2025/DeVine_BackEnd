package com.umc.devine.domain.member;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.devine.admin.complaint.entity.Complaint;
import com.umc.devine.admin.complaint.enums.ComplaintTargetType;
import com.umc.devine.admin.complaint.repository.ComplaintRepository;
import com.umc.devine.admin.complaint.service.query.ComplaintQueryService;
import com.umc.devine.admin.member.entity.MemberStatusHistory;
import com.umc.devine.admin.member.enums.MemberStatusAction;
import com.umc.devine.admin.member.repository.MemberStatusHistoryRepository;
import com.umc.devine.admin.payment.service.AdminWithdrawalRefundService;
import com.umc.devine.admin.payment.service.RefundTxService;
import com.umc.devine.domain.bookmark.entity.Bookmark;
import com.umc.devine.domain.bookmark.enums.BookmarkType;
import com.umc.devine.domain.bookmark.repository.BookmarkRepository;
import com.umc.devine.domain.category.entity.Category;
import com.umc.devine.domain.category.enums.CategoryGenre;
import com.umc.devine.domain.category.repository.CategoryRepository;
import com.umc.devine.domain.chat.entity.ChatMessage;
import com.umc.devine.domain.chat.entity.ChatRoom;
import com.umc.devine.domain.chat.repository.ChatMessageRepository;
import com.umc.devine.domain.chat.repository.ChatRoomRepository;
import com.umc.devine.domain.image.entity.Image;
import com.umc.devine.domain.image.enums.ImageType;
import com.umc.devine.domain.image.repository.ImageRepository;
import com.umc.devine.domain.member.dto.WithdrawalReqDTO;
import com.umc.devine.domain.member.dto.WithdrawalResDTO;
import com.umc.devine.domain.member.entity.Contact;
import com.umc.devine.domain.member.entity.GitRepoUrl;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.entity.MemberAgreement;
import com.umc.devine.domain.member.enums.ContactType;
import com.umc.devine.domain.member.enums.MemberMainType;
import com.umc.devine.domain.member.enums.MemberStatus;
import com.umc.devine.domain.member.enums.WithdrawalReason;
import com.umc.devine.domain.member.exception.MemberException;
import com.umc.devine.domain.member.exception.code.MemberErrorReason;
import com.umc.devine.domain.member.repository.ContactRepository;
import com.umc.devine.domain.member.repository.GitRepoUrlRepository;
import com.umc.devine.domain.member.repository.MemberAgreementRepository;
import com.umc.devine.domain.member.repository.MemberRepository;
import com.umc.devine.domain.member.repository.TermsRepository;
import com.umc.devine.domain.member.repository.WithdrawalReasonStatRepository;
import com.umc.devine.domain.member.service.command.MemberWithdrawalCommandService;
import com.umc.devine.domain.notification.entity.Notification;
import com.umc.devine.domain.notification.enums.NotificationType;
import com.umc.devine.domain.notification.repository.NotificationRepository;
import com.umc.devine.domain.outbox.entity.OutboxTask;
import com.umc.devine.domain.outbox.enums.OutboxTaskType;
import com.umc.devine.domain.outbox.repository.OutboxTaskRepository;
import com.umc.devine.domain.payment.entity.Payment;
import com.umc.devine.domain.payment.entity.PaymentRefund;
import com.umc.devine.domain.payment.entity.Transaction;
import com.umc.devine.domain.payment.enums.PaymentMethod;
import com.umc.devine.domain.payment.enums.RefundSource;
import com.umc.devine.domain.payment.enums.RefundStatus;
import com.umc.devine.domain.payment.enums.TransactionStatus;
import com.umc.devine.domain.payment.enums.TransactionType;
import com.umc.devine.domain.payment.exception.PaymentException;
import com.umc.devine.domain.payment.exception.code.PaymentErrorReason;
import com.umc.devine.domain.payment.repository.PaymentRefundRepository;
import com.umc.devine.domain.payment.repository.PaymentRepository;
import com.umc.devine.domain.project.entity.Project;
import com.umc.devine.domain.project.entity.ProjectRequirementMember;
import com.umc.devine.domain.project.entity.mapping.Matching;
import com.umc.devine.domain.project.enums.*;
import com.umc.devine.domain.project.enums.mapping.MatchingStatus;
import com.umc.devine.domain.project.enums.mapping.MatchingType;
import com.umc.devine.domain.project.repository.MatchingRepository;
import com.umc.devine.domain.project.repository.ProjectRepository;
import com.umc.devine.domain.project.repository.ProjectRequirementMemberRepository;
import com.umc.devine.domain.report.dto.ReportReqDTO;
import com.umc.devine.domain.report.entity.DevReport;
import com.umc.devine.domain.report.enums.ReportType;
import com.umc.devine.domain.report.repository.DevReportRepository;
import com.umc.devine.domain.report.service.command.ReportCommandService;
import com.umc.devine.domain.techstack.entity.mapping.DevTechstack;
import com.umc.devine.domain.techstack.enums.TechName;
import com.umc.devine.domain.techstack.enums.TechstackSource;
import com.umc.devine.domain.techstack.repository.DevTechstackRepository;
import com.umc.devine.domain.techstack.repository.TechstackRepository;
import com.umc.devine.domain.ticket.entity.MemberReportCredit;
import com.umc.devine.domain.ticket.entity.PaymentTicket;
import com.umc.devine.domain.ticket.entity.TicketProduct;
import com.umc.devine.domain.ticket.repository.MemberReportCreditRepository;
import com.umc.devine.domain.ticket.repository.TicketProductRepository;
import com.umc.devine.global.scheduler.MemberWithdrawalFinalizeScheduler;
import com.umc.devine.support.IntegrationTestSupport;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 01-1 회원 탈퇴 FUNC-MEM-003(탈퇴 실행)·FUNC-MEM-004 1번(환불 신청 기록)과 기능명세서 5장 테이블별 처리.
 * 시나리오 T1·T2·T4·T6-3·T6-4·T13.
 */
class MemberWithdrawalExecuteTest extends IntegrationTestSupport {

    @Autowired private MemberWithdrawalCommandService withdrawalService;
    @Autowired private MemberRepository memberRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private ContactRepository contactRepository;
    @Autowired private TermsRepository termsRepository;
    @Autowired private MemberAgreementRepository memberAgreementRepository;
    @Autowired private TechstackRepository techstackRepository;
    @Autowired private DevTechstackRepository devTechstackRepository;
    @Autowired private GitRepoUrlRepository gitRepoUrlRepository;
    @Autowired private DevReportRepository devReportRepository;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private ProjectRequirementMemberRepository requirementMemberRepository;
    @Autowired private MatchingRepository matchingRepository;
    @Autowired private BookmarkRepository bookmarkRepository;
    @Autowired private ImageRepository imageRepository;
    @Autowired private ChatRoomRepository chatRoomRepository;
    @Autowired private ChatMessageRepository chatMessageRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private ComplaintRepository complaintRepository;
    @Autowired private ComplaintQueryService complaintQueryService;
    @Autowired private MemberStatusHistoryRepository memberStatusHistoryRepository;
    @Autowired private MemberReportCreditRepository memberReportCreditRepository;
    @Autowired private TicketProductRepository ticketProductRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private PaymentRefundRepository paymentRefundRepository;
    @Autowired private OutboxTaskRepository outboxTaskRepository;
    @Autowired private WithdrawalReasonStatRepository withdrawalReasonStatRepository;
    @Autowired private RefundTxService refundTxService;
    @Autowired private AdminWithdrawalRefundService adminWithdrawalRefundService;
    @Autowired private ReportCommandService reportCommandService;
    @Autowired private MemberWithdrawalFinalizeScheduler finalizeScheduler;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private EntityManager em;

    private Member me;
    private Member other;
    private Category category;

    @BeforeEach
    void setUp() {
        me = saveMember("clerk_wx_me", "wxme", MemberStatus.ACTIVE);
        other = saveMember("clerk_wx_other", "wxother", MemberStatus.ACTIVE);
        category = categoryRepository.findByGenre(CategoryGenre.ECOMMERCE).orElseThrow();
    }

    private Member saveMember(String clerkId, String nickname, MemberStatus status) {
        return memberRepository.save(Member.builder()
                .clerkId(clerkId).name("홍길동").nickname(nickname)
                .address("서울").body("소개").image("https://cdn/profile.png").githubUsername("gh-" + nickname)
                .mainType(MemberMainType.DEVELOPER).disclosure(true).used(status)
                .build());
    }

    private static WithdrawalReqDTO.WithdrawReq confirmed() {
        return WithdrawalReqDTO.WithdrawReq.builder().confirmed(true).build();
    }

    private static WithdrawalReqDTO.WithdrawReq refundRequest(String email) {
        return WithdrawalReqDTO.WithdrawReq.builder()
                .confirmed(true).refundRequested(true).refundContactEmail(email)
                .build();
    }

    @Test
    @DisplayName("T1: 회원 행은 익명화해 남기고, 프로필·GitHub 데이터·약관 동의·기술스택·연락처를 지운다")
    void t1_anonymizesMemberAndDeletesProfileData() {
        me.addCategories(List.of(category));
        contactRepository.save(Contact.builder().member(me).contactType(ContactType.EMAIL).value("me@example.com").build());
        memberAgreementRepository.save(MemberAgreement.builder().member(me).terms(termsRepository.findAll().get(0)).agreed(true).build());
        devTechstackRepository.save(DevTechstack.builder().member(me)
                .techstack(techstackRepository.findByName(TechName.JAVA).orElseThrow()).source(TechstackSource.AUTO).build());
        devTechstackRepository.save(DevTechstack.builder().member(me)
                .techstack(techstackRepository.findByName(TechName.PYTHON).orElseThrow()).source(TechstackSource.MANUAL).build());
        GitRepoUrl repo = gitRepoUrlRepository.save(GitRepoUrl.builder().gitUrl("https://github.com/wx/repo").member(me).build());
        devReportRepository.save(DevReport.builder().gitRepoUrl(repo).reportType(ReportType.MAIN).completedAt(LocalDateTime.now()).build());
        devReportRepository.save(DevReport.builder().gitRepoUrl(repo).reportType(ReportType.DETAIL).completedAt(LocalDateTime.now()).build());
        memberReportCreditRepository.save(MemberReportCredit.of(me, 2));
        em.flush();

        WithdrawalResDTO.WithdrawResultDTO result = withdrawalService.withdraw(me, WithdrawalReqDTO.WithdrawReq.builder()
                .confirmed(true).reason(WithdrawalReason.OTHER).build());
        em.flush();
        em.clear();

        assertThat(result.withdrawn()).isTrue();
        assertThat(result.refundRequested()).isFalse();

        Member withdrawn = memberRepository.findById(me.getId()).orElseThrow();
        assertThat(withdrawn.getUsed()).isEqualTo(MemberStatus.DELETED);
        assertThat(withdrawn.getNickname()).isEqualTo("deleted#" + me.getId());
        assertThat(withdrawn.getName()).isNull();
        assertThat(withdrawn.getClerkId()).isNull();
        assertThat(withdrawn.getAddress()).isNull();
        assertThat(withdrawn.getBody()).isNull();
        assertThat(withdrawn.getImage()).isNull();
        assertThat(withdrawn.getGithubUsername()).isNull();
        assertThat(withdrawn.getDisclosure()).isFalse();
        assertThat(withdrawn.getProposalAlarm()).isFalse();
        assertThat(withdrawn.getMemberCategories()).isEmpty();
        assertThat(memberRepository.findByClerkId("clerk_wx_me")).isEmpty();

        assertThat(contactRepository.findAllByMember(withdrawn)).isEmpty();
        assertThat(memberAgreementRepository.findAllByMember(withdrawn)).isEmpty();
        assertThat(devTechstackRepository.findAllByMember(withdrawn)).isEmpty();
        assertThat(gitRepoUrlRepository.findAllByMember(withdrawn)).isEmpty();
        assertThat(devReportRepository.findIdsByMember(withdrawn)).isEmpty();
        assertThat(memberReportCreditRepository.findByMember(withdrawn).orElseThrow().getRemainingCount()).isZero();
        assertThat(withdrawalReasonStatRepository.findAll())
                .anySatisfy(stat -> assertThat(stat.getReason()).isEqualTo(WithdrawalReason.OTHER));
    }

    @Test
    @DisplayName("감사 칼럼(created_by, updated_by)에 남은 clerk_id를 회원 행에서 비운다")
    void clearsAuditColumnsOnMemberRow() {
        em.createNativeQuery("UPDATE member SET created_by = 'clerk_wx_me', updated_by = 'clerk_wx_me' WHERE member_id = :id")
                .setParameter("id", me.getId()).executeUpdate();

        withdrawalService.withdraw(me, confirmed());
        em.flush();

        Object[] audit = (Object[]) em.createNativeQuery("SELECT created_by, updated_by FROM member WHERE member_id = :id")
                .setParameter("id", me.getId()).getSingleResult();
        assertThat(audit[0]).isNull();
        assertThat(audit[1]).isNull();
    }

    @Test
    @DisplayName("상태 변경 이력은 기존 행을 지우고 사유 없는 자진 탈퇴 행 1개만 남긴다")
    void statusHistory_replacedWithSelfWithdraw() {
        memberStatusHistoryRepository.save(MemberStatusHistory.builder()
                .member(me).action(MemberStatusAction.SUSPEND).status(MemberStatus.SUSPENDED).reason("과거 정지").build());

        withdrawalService.withdraw(me, confirmed());
        em.flush();

        List<MemberStatusHistory> histories = memberStatusHistoryRepository.findAll().stream()
                .filter(h -> h.getMember().getId().equals(me.getId())).toList();
        assertThat(histories).singleElement().satisfies(h -> {
            assertThat(h.getAction()).isEqualTo(MemberStatusAction.SELF_WITHDRAW);
            assertThat(h.getStatus()).isEqualTo(MemberStatus.DELETED);
            assertThat(h.getReason()).isNull();
        });
    }

    @Test
    @DisplayName("T2: 대기 지원·제안이 있으면 409와 대기 건 수를 돌려주고 아무것도 바꾸지 않는다")
    void t2_pendingMatchingsBlockWithdrawal() {
        Project project = saveProject(other, "남의 프로젝트");
        saveMatching(project, me, MatchingStatus.PENDING);

        assertThatThrownBy(() -> withdrawalService.withdraw(me, confirmed()))
                .isInstanceOf(MemberException.class)
                .satisfies(e -> {
                    MemberException ex = (MemberException) e;
                    assertThat(ex.getReason()).isEqualTo(MemberErrorReason.WITHDRAWAL_PENDING_MATCHINGS_EXIST);
                    assertThat(((WithdrawalResDTO.PendingMatchingsRemainDTO) ex.getResult()).pendingCount()).isEqualTo(1);
                });
        assertThat(me.getUsed()).isEqualTo(MemberStatus.ACTIVE);
        assertThat(me.getClerkId()).isEqualTo("clerk_wx_me");
    }

    @Test
    @DisplayName("T4: 환불 신청 시 결제 건별 신청 행(REQUESTED)을 만들고 운영자 메일을 아웃박스에 남기며 생성권은 0이 된다")
    void t4_refundRequestCreated() {
        memberReportCreditRepository.save(MemberReportCredit.of(me, 2));
        Payment payment = savePayment(me, "pay_wx_t4", 9_900L, LocalDateTime.now().minusDays(20));

        WithdrawalResDTO.WithdrawResultDTO result = withdrawalService.withdraw(me, refundRequest(" refund@example.com "));
        em.flush();

        assertThat(result.refundRequested()).isTrue();
        List<PaymentRefund> refunds = paymentRefundRepository.findAllByPaymentIdIn(List.of(payment.getId()));
        assertThat(refunds).singleElement().satisfies(r -> {
            assertThat(r.getStatus()).isEqualTo(RefundStatus.REQUESTED);
            assertThat(r.getSource()).isEqualTo(RefundSource.WITHDRAWAL);
            assertThat(r.getReason()).isEqualTo("회원 탈퇴");
            assertThat(r.getRefundAmount()).isEqualTo(6_600L);
            assertThat(r.getRefundCreditCount()).isEqualTo(2);
            assertThat(r.getContactEmail()).isEqualTo("refund@example.com");
        });
        assertThat(memberReportCreditRepository.findByMember(me).orElseThrow().getRemainingCount()).isZero();
        assertThat(outboxOf(OutboxTaskType.WITHDRAWAL_REFUND_MAIL)).singleElement()
                .satisfies(t -> assertThat(t.getPayload()).contains(String.valueOf(refunds.get(0).getId())));

        // T13: 결제 기록은 그대로 남는다
        assertThat(paymentRepository.findById(payment.getId())).isPresent();
    }

    @Test
    @DisplayName("환불을 신청했는데 연락 이메일이 없으면 400이고, 환불 가능 생성권이 없으면 신청은 무시된다")
    void refundRequest_validation() {
        memberReportCreditRepository.save(MemberReportCredit.of(me, 1));
        savePayment(me, "pay_wx_email", 4_900L, LocalDateTime.now().minusDays(1));

        assertThatThrownBy(() -> withdrawalService.withdraw(me, refundRequest(null)))
                .isInstanceOf(MemberException.class)
                .extracting("reason").isEqualTo(MemberErrorReason.REFUND_CONTACT_EMAIL_REQUIRED);

        // 무상분만 있는 회원
        memberReportCreditRepository.save(MemberReportCredit.of(other, 3));
        WithdrawalResDTO.WithdrawResultDTO result = withdrawalService.withdraw(other, refundRequest(null));

        assertThat(result.refundRequested()).isFalse();
        assertThat(outboxOf(OutboxTaskType.WITHDRAWAL_REFUND_MAIL)).isEmpty();
    }

    @Test
    @DisplayName("외부 정리 작업(GitHub 권한 회수부터)을 clerk_id와 이미지 S3 키로 아웃박스에 남기고 이미지 행은 지운다")
    void externalCleanupEnqueued() throws Exception {
        imageRepository.save(Image.builder().imageType(ImageType.PROFILE).imageUrl("https://cdn/profile.png")
                .s3Key("profile/wx.png").uploaded(true).uploader(me).build());

        withdrawalService.withdraw(me, confirmed());
        em.flush();

        assertThat(imageRepository.findAllByUploader(me)).isEmpty();
        OutboxTask task = outboxOf(OutboxTaskType.GITHUB_GRANT_REVOKE).get(0);
        Map<?, ?> payload = objectMapper.readValue(task.getPayload(), Map.class);
        assertThat(payload.get("clerkId")).isEqualTo("clerk_wx_me");
        assertThat(payload.get("s3Keys")).isEqualTo(List.of("profile/wx.png"));
        assertThat(task.getPayload()).doesNotContainIgnoringCase("token");
    }

    @Test
    @DisplayName("PM이면 프로젝트 삭제 함수를 부른다: 매칭 이력이 있으면 이름만 남기고, 없으면 행까지 지운다. 북마크도 지운다")
    void ownedProjectsDeleted() {
        Project withHistory = saveProject(me, "이력 있는 프로젝트");
        saveMatching(withHistory, other, MatchingStatus.COMPLETED);
        Project withoutHistory = saveProject(me, "이력 없는 프로젝트");
        bookmarkRepository.save(Bookmark.builder().member(other).targetType(BookmarkType.PROJECT).targetId(withHistory.getId()).build());
        bookmarkRepository.save(Bookmark.builder().member(other).targetType(BookmarkType.DEVELOPER).targetId(me.getId()).build());
        bookmarkRepository.save(Bookmark.builder().member(me).targetType(BookmarkType.DEVELOPER).targetId(other.getId()).build());

        withdrawalService.withdraw(me, confirmed());
        em.flush();
        em.clear();

        Project kept = projectRepository.findById(withHistory.getId()).orElseThrow();
        assertThat(kept.getStatus()).isEqualTo(ProjectStatus.DELETED);
        assertThat(kept.getName()).isEqualTo("이력 있는 프로젝트");
        assertThat(projectRepository.findById(withoutHistory.getId())).isEmpty();
        assertThat(bookmarkRepository.findAll()).noneMatch(b ->
                b.getMember().getId().equals(me.getId())
                        || (b.getTargetType() == BookmarkType.DEVELOPER && b.getTargetId().equals(me.getId()))
                        || (b.getTargetType() == BookmarkType.PROJECT && b.getTargetId().equals(withHistory.getId())));
        // 매칭 이력은 보관한다
        assertThat(matchingRepository.existsByProject(kept)).isTrue();
    }

    @Test
    @DisplayName("채팅: 내가 보낸 메시지만 지우고 상대 메시지와 방은 남기며 내 쪽은 나간 상태가 된다. 받은 알림만 지운다")
    void chatAndNotifications() {
        ChatRoom room = chatRoomRepository.save(ChatRoom.builder().member1(me).member2(other).build());
        ChatMessage mine = chatMessageRepository.save(ChatMessage.builder().chatRoom(room).sender(me).content("내 메시지").build());
        ChatMessage theirs = chatMessageRepository.save(ChatMessage.builder().chatRoom(room).sender(other).content("상대 메시지").build());
        Notification received = saveNotification(me, other);
        Notification sent = saveNotification(other, me);

        withdrawalService.withdraw(me, confirmed());
        em.flush();
        em.clear();

        assertThat(chatMessageRepository.findById(mine.getId())).isEmpty();
        assertThat(chatMessageRepository.findById(theirs.getId())).isPresent();
        ChatRoom kept = chatRoomRepository.findById(room.getId()).orElseThrow();
        assertThat(kept.getMember1Left()).isTrue();
        assertThat(kept.getMember2Left()).isFalse();
        assertThat(notificationRepository.findById(received.getId())).isEmpty();
        assertThat(notificationRepository.findById(sent.getId())).isPresent()
                .get().satisfies(n -> assertThat(n.getSender().getId()).isEqualTo(me.getId()));
    }

    @Test
    @DisplayName("신고 기록은 행을 남기고 신고자·피신고자 칸을 비우며, 관리자 화면에는 '탈퇴한 회원'으로 보인다")
    void complaintsAnonymized() {
        Complaint asComplainant = complaintRepository.save(Complaint.builder().complainant(me).respondentMember(other)
                .targetType(ComplaintTargetType.DEVELOPER).targetId(other.getId()).reason("신고").build());
        Complaint asRespondent = complaintRepository.save(Complaint.builder().complainant(other).respondentMember(me)
                .targetType(ComplaintTargetType.DEVELOPER).targetId(me.getId()).reason("신고").build());

        withdrawalService.withdraw(me, confirmed());
        em.flush();
        em.clear();

        assertThat(complaintRepository.findById(asComplainant.getId()).orElseThrow().getComplainant()).isNull();
        assertThat(complaintRepository.findById(asRespondent.getId()).orElseThrow().getRespondentMember()).isNull();
        assertThat(complaintQueryService.getComplaintDetail(asRespondent.getId()))
                .satisfies(detail -> {
                    assertThat(detail.respondentNickname()).isEqualTo("탈퇴한 회원");
                    assertThat(detail.complainantNickname()).isEqualTo("wxother");
                    assertThat(detail.respondentComplaintCount()).isZero();
                });
    }

    @Test
    @DisplayName("T6-4: 자격상실 예정 회원도 탈퇴할 수 있고, 예정일이 지나도 관리자 확정 작업이 실행되지 않는다")
    void t6_4_pendingWithdrawalMemberCanWithdraw() {
        Member pending = saveMember("clerk_wx_pending", "wxpending", MemberStatus.ACTIVE);
        pending.scheduleForceWithdrawal(LocalDateTime.now().minusDays(1));
        em.flush();

        withdrawalService.withdraw(pending, confirmed());
        em.flush();

        assertThat(pending.getUsed()).isEqualTo(MemberStatus.DELETED);
        assertThat(pending.getScheduledWithdrawalAt()).isNull();
        finalizeScheduler.finalizeExpiredWithdrawals();
        em.flush();
        assertThat(memberStatusHistoryRepository.findAll()).noneMatch(h ->
                h.getMember().getId().equals(pending.getId()) && h.getAction() == MemberStatusAction.WITHDRAWAL_FINALIZED);
    }

    @Test
    @DisplayName("이미 탈퇴한 회원은 다시 탈퇴할 수 없다")
    void alreadyWithdrawn() {
        withdrawalService.withdraw(me, confirmed());
        em.flush();

        assertThatThrownBy(() -> withdrawalService.withdraw(me, confirmed()))
                .isInstanceOf(com.umc.devine.domain.auth.exception.AuthException.class);
    }

    @Test
    @DisplayName("탈퇴 환불 신청이 있는 결제는 관리자 전액 환불을 막고, 완료 처리하면 COMPLETED가 되며 연락 이메일을 지운다")
    void adminRefundBlockedAndCompletion() {
        memberReportCreditRepository.save(MemberReportCredit.of(me, 1));
        Payment payment = savePayment(me, "pay_wx_admin", 4_900L, LocalDateTime.now().minusDays(2));
        withdrawalService.withdraw(me, refundRequest("refund@example.com"));
        em.flush();

        assertThatThrownBy(() -> refundTxService.claim(payment.getId(), "고객 요청"))
                .isInstanceOf(PaymentException.class)
                .extracting("reason").isEqualTo(PaymentErrorReason.WITHDRAWAL_REFUND_REQUESTED);

        PaymentRefund request = paymentRefundRepository.findAllByPaymentIdIn(List.of(payment.getId())).get(0);
        adminWithdrawalRefundService.complete(request.getId());
        em.flush();
        em.clear();

        PaymentRefund completed = paymentRefundRepository.findById(request.getId()).orElseThrow();
        assertThat(completed.getStatus()).isEqualTo(RefundStatus.COMPLETED);
        assertThat(completed.getContactEmail()).isNull();
        assertThatThrownBy(() -> adminWithdrawalRefundService.complete(request.getId()))
                .isInstanceOf(PaymentException.class)
                .extracting("reason").isEqualTo(PaymentErrorReason.NOT_WITHDRAWAL_REFUND_REQUEST);
    }

    @Test
    @DisplayName("T6-3: 탈퇴한 회원의 리포트 실패 콜백이 오면 결과를 버리고 생성권을 돌려주지 않는다")
    void t6_3_callbackForWithdrawnMember() {
        GitRepoUrl repo = gitRepoUrlRepository.save(GitRepoUrl.builder().gitUrl("https://github.com/wx/gen").member(other).build());
        DevReport main = devReportRepository.save(DevReport.builder().gitRepoUrl(repo).reportType(ReportType.MAIN).build());
        DevReport detail = devReportRepository.save(DevReport.builder().gitRepoUrl(repo).reportType(ReportType.DETAIL).build());
        memberReportCreditRepository.save(MemberReportCredit.of(other, 0));
        other.withdraw();
        em.flush();

        reportCommandService.processCallback(ReportReqDTO.CallbackReq.builder()
                .mainReportId(main.getId()).detailReportId(detail.getId())
                .status(ReportReqDTO.CallbackStatus.FAILED).errorMessage("AI 서버 장애")
                .build());

        assertThat(memberReportCreditRepository.findByMember(other).orElseThrow().getRemainingCount()).isZero();
    }

    private List<OutboxTask> outboxOf(OutboxTaskType type) {
        em.flush();
        return outboxTaskRepository.findAll().stream().filter(t -> t.getTaskType() == type).toList();
    }

    private Project saveProject(Member pm, String name) {
        Project project = projectRepository.save(Project.builder()
                .name(name).content("소개").status(ProjectStatus.RECRUITING)
                .projectField(ProjectField.WEB).mode(ProjectMode.ONLINE)
                .durationRange(DurationRange.ONE_TO_THREE).location("온라인")
                .recruitmentDeadline(LocalDate.now().plusDays(10)).category(category).member(pm)
                .build());
        project.addRequirement(requirementMemberRepository.save(ProjectRequirementMember.builder()
                .project(project).part(ProjectPart.BACKEND).requirementNum(2).currentCount(0)
                .build()));
        return project;
    }

    private Matching saveMatching(Project project, Member developer, MatchingStatus status) {
        return matchingRepository.save(Matching.builder()
                .project(project).member(developer).matchingType(MatchingType.APPLY).status(status).part(ProjectPart.BACKEND)
                .build());
    }

    private Notification saveNotification(Member receiver, Member sender) {
        return notificationRepository.save(Notification.builder()
                .receiver(receiver).sender(sender).type(NotificationType.MATCHING_APPLIED)
                .title("새로운 지원자가 있습니다").content("알림").referenceId(1L)
                .build());
    }

    private Payment savePayment(Member member, String portonePaymentId, long amount, LocalDateTime paidAt) {
        TicketProduct product = ticketProductRepository.save(TicketProduct.builder()
                .name("리포트 생성권 " + portonePaymentId).price(amount).creditAmount(amount == 9_900L ? 3 : 1).active(true)
                .build());
        Payment payment = Payment.builder()
                .portonePaymentId(portonePaymentId).member(member).orderName(product.getName())
                .amount(amount).currency("KRW")
                .build();
        payment.addTransaction(Transaction.builder()
                .portoneTransactionId("tx_" + portonePaymentId).payment(payment)
                .type(TransactionType.PAYMENT).status(TransactionStatus.PAID)
                .method(PaymentMethod.CARD).pgProvider("TOSSPAYMENTS")
                .amount(amount).paidAt(paidAt)
                .build());
        payment.addPaymentTicket(PaymentTicket.builder()
                .payment(payment).ticketProduct(product).quantity(1)
                .unitPrice(product.getPrice()).unitCreditAmount(product.getCreditAmount())
                .build());
        return paymentRepository.saveAndFlush(payment);
    }
}
