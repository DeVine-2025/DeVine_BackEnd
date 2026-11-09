package com.umc.devine.domain.complaint;

import com.umc.devine.admin.complaint.dto.ComplaintReqDTO;
import com.umc.devine.admin.complaint.service.command.AdminComplaintCommandService;
import com.umc.devine.domain.category.entity.Category;
import com.umc.devine.domain.category.enums.CategoryGenre;
import com.umc.devine.domain.category.repository.CategoryRepository;
import com.umc.devine.domain.chat.entity.ChatMessage;
import com.umc.devine.domain.chat.entity.ChatRoom;
import com.umc.devine.domain.chat.repository.ChatMessageRepository;
import com.umc.devine.domain.chat.repository.ChatRoomRepository;
import com.umc.devine.domain.complaint.entity.Complaint;
import com.umc.devine.domain.complaint.enums.ComplaintAction;
import com.umc.devine.domain.complaint.enums.ComplaintReasonCode;
import com.umc.devine.domain.complaint.enums.ComplaintStatus;
import com.umc.devine.domain.complaint.enums.ComplaintTargetType;
import com.umc.devine.domain.complaint.exception.ComplaintException;
import com.umc.devine.domain.complaint.exception.code.ComplaintErrorReason;
import com.umc.devine.domain.complaint.repository.ComplaintRepository;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.enums.MemberMainType;
import com.umc.devine.domain.member.enums.MemberStatus;
import com.umc.devine.domain.member.repository.MemberRepository;
import com.umc.devine.domain.notification.enums.NotificationType;
import com.umc.devine.domain.notification.repository.NotificationRepository;
import com.umc.devine.domain.project.entity.Project;
import com.umc.devine.domain.project.enums.DurationRange;
import com.umc.devine.domain.project.enums.ProjectField;
import com.umc.devine.domain.project.enums.ProjectMode;
import com.umc.devine.domain.project.enums.ProjectStatus;
import com.umc.devine.domain.project.repository.ProjectRepository;
import com.umc.devine.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("FUNC-REP-002~004 신고 처리 결과 연동")
class ComplaintResolutionTest extends IntegrationTestSupport {

    @Autowired
    private AdminComplaintCommandService adminComplaintCommandService;
    @Autowired
    private ComplaintRepository complaintRepository;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private ChatRoomRepository chatRoomRepository;
    @Autowired
    private ChatMessageRepository chatMessageRepository;
    @Autowired
    private NotificationRepository notificationRepository;

    private Member complainant;
    private Member respondent;
    private Member admin;

    @BeforeEach
    void setUp() {
        complainant = saveMember("clerk_c2", "complainant2", MemberStatus.ACTIVE);
        respondent = saveMember("clerk_r2", "respondent2", MemberStatus.ACTIVE);
        admin = saveMember("clerk_a2", "admin2", MemberStatus.ACTIVE);
    }

    @Test
    @DisplayName("정지로 처리하면 피신고자 계정이 실제로 정지된다")
    void suspendAction_suspendsAccount() {
        Complaint complaint = saveComplaint(ComplaintTargetType.DEVELOPER, null, ComplaintStatus.IN_REVIEW);

        adminComplaintCommandService.updateStatus(complaint.getId(), admin.getClerkId(),
                completeWith(ComplaintAction.SUSPEND));

        assertThat(memberRepository.findById(respondent.getId()).orElseThrow().getUsed())
                .isEqualTo(MemberStatus.SUSPENDED);
        assertThat(complaintRepository.findById(complaint.getId()).orElseThrow().isLinkedActionCompleted()).isTrue();
    }

    @Test
    @DisplayName("이미 정지된 피신고자를 다시 정지 처리해도 신고 상태 변경이 롤백되지 않는다")
    void suspendAction_idempotentForAlreadySuspended() {
        respondent.suspend();
        memberRepository.saveAndFlush(respondent);
        Complaint complaint = saveComplaint(ComplaintTargetType.DEVELOPER, null, ComplaintStatus.IN_REVIEW);

        adminComplaintCommandService.updateStatus(complaint.getId(), admin.getClerkId(),
                completeWith(ComplaintAction.SUSPEND));

        Complaint updated = complaintRepository.findById(complaint.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(ComplaintStatus.COMPLETED);
        assertThat(updated.getAction()).isEqualTo(ComplaintAction.SUSPEND);
        assertThat(updated.isLinkedActionCompleted()).isFalse();
    }

    @Test
    @DisplayName("회원 신고에는 삭제 액션을 고를 수 없다")
    void developerComplaint_rejectsDeleteAction() {
        Complaint complaint = saveComplaint(ComplaintTargetType.DEVELOPER, null, ComplaintStatus.IN_REVIEW);

        assertThatThrownBy(() -> adminComplaintCommandService.updateStatus(complaint.getId(), admin.getClerkId(),
                completeWith(ComplaintAction.DELETE)))
                .isInstanceOf(ComplaintException.class)
                .extracting("reason").isEqualTo(ComplaintErrorReason.ACTION_NOT_ALLOWED_FOR_TARGET);
    }

    @Test
    @DisplayName("처리완료 시 통보용 사유 요약이 없으면 처리할 수 없다")
    void complete_requiresNotifySummary() {
        Complaint complaint = saveComplaint(ComplaintTargetType.DEVELOPER, null, ComplaintStatus.IN_REVIEW);

        assertThatThrownBy(() -> adminComplaintCommandService.updateStatus(complaint.getId(), admin.getClerkId(),
                ComplaintReqDTO.UpdateStatusReq.builder()
                        .status(ComplaintStatus.COMPLETED)
                        .action(ComplaintAction.WARNING)
                        .reason("내부 메모")
                        .build()))
                .isInstanceOf(ComplaintException.class)
                .extracting("reason").isEqualTo(ComplaintErrorReason.NOTIFY_SUMMARY_REQUIRED);
    }

    @Test
    @DisplayName("채팅 메시지를 삭제로 처리하면 양쪽 화면에서 가려진다")
    void chatComplaint_deleteHidesForBothSides() {
        ChatMessage message = saveMessage(respondent, "가려질 메시지");
        Complaint complaint = saveComplaint(ComplaintTargetType.CHAT, message.getId(), ComplaintStatus.IN_REVIEW);

        adminComplaintCommandService.updateStatus(complaint.getId(), admin.getClerkId(),
                completeWith(ComplaintAction.DELETE));

        assertThat(chatMessageRepository.findById(message.getId()).orElseThrow().isHiddenByPolicy()).isTrue();
    }

    @Test
    @DisplayName("채팅 메시지를 경고로 처리하면 메시지 행은 가려지지 않는다")
    void chatComplaint_warningKeepsMessageRow() {
        ChatMessage message = saveMessage(respondent, "경고로 끝나는 메시지");
        Complaint complaint = saveComplaint(ComplaintTargetType.CHAT, message.getId(), ComplaintStatus.IN_REVIEW);

        adminComplaintCommandService.updateStatus(complaint.getId(), admin.getClerkId(),
                completeWith(ComplaintAction.WARNING));

        assertThat(chatMessageRepository.findById(message.getId()).orElseThrow().isHiddenByPolicy()).isFalse();
    }

    @Test
    @DisplayName("삭제로 가린 채팅 메시지를 기각으로 재처리하면 다시 보인다")
    void chatComplaint_dismissAfterDeleteRestoresMessage() {
        ChatMessage message = saveMessage(respondent, "되돌릴 메시지");
        Complaint complaint = saveComplaint(ComplaintTargetType.CHAT, message.getId(), ComplaintStatus.IN_REVIEW);
        adminComplaintCommandService.updateStatus(complaint.getId(), admin.getClerkId(),
                completeWith(ComplaintAction.DELETE));

        adminComplaintCommandService.updateStatus(complaint.getId(), admin.getClerkId(),
                completeWith(ComplaintAction.DISMISS));

        assertThat(chatMessageRepository.findById(message.getId()).orElseThrow().isHiddenByPolicy()).isFalse();
    }

    @Test
    @DisplayName("다른 신고가 삭제로 확정돼 있으면 기각해도 채팅 메시지는 가려진 채 유지된다")
    void chatComplaint_dismissKeepsHiddenWhenAnotherComplaintDeleted() {
        ChatMessage message = saveMessage(respondent, "유지될 메시지");
        Complaint first = saveComplaint(ComplaintTargetType.CHAT, message.getId(), ComplaintStatus.IN_REVIEW);
        Complaint second = saveComplaintBy(saveMember("clerk_c4", "complainant4", MemberStatus.ACTIVE),
                ComplaintTargetType.CHAT, message.getId(), ComplaintStatus.IN_REVIEW);
        adminComplaintCommandService.updateStatus(second.getId(), admin.getClerkId(),
                completeWith(ComplaintAction.DELETE));

        adminComplaintCommandService.updateStatus(first.getId(), admin.getClerkId(),
                completeWith(ComplaintAction.DISMISS));

        assertThat(chatMessageRepository.findById(message.getId()).orElseThrow().isHiddenByPolicy()).isTrue();
    }

    @Test
    @DisplayName("다른 신고가 처리 중이면 기각해도 프로젝트 가림을 되돌리지 않는다")
    void dismiss_keepsBlindWhenAnotherComplaintIsInProgress() {
        Project project = saveProject();
        project.changeReportBlind(true, LocalDateTime.now());
        projectRepository.saveAndFlush(project);

        Complaint first = saveComplaint(ComplaintTargetType.PROJECT, project.getId(), ComplaintStatus.IN_REVIEW);
        saveComplaintBy(saveMember("clerk_c3", "complainant3", MemberStatus.ACTIVE),
                ComplaintTargetType.PROJECT, project.getId(), ComplaintStatus.PENDING);

        adminComplaintCommandService.updateStatus(first.getId(), admin.getClerkId(),
                completeWith(ComplaintAction.DISMISS));

        assertThat(projectRepository.findById(project.getId()).orElseThrow().isReportBlinded()).isTrue();
    }

    @Test
    @DisplayName("처리가 끝나면 신고자와 피신고자 모두에게 결과를 알린다")
    void complete_notifiesBothSides() {
        Complaint complaint = saveComplaint(ComplaintTargetType.DEVELOPER, null, ComplaintStatus.IN_REVIEW);

        adminComplaintCommandService.updateStatus(complaint.getId(), admin.getClerkId(),
                completeWith(ComplaintAction.WARNING));

        assertThat(notificationRepository.findAll())
                .anyMatch(n -> n.getType() == NotificationType.COMPLAINT_RESOLVED_COMPLAINANT
                        && n.getReceiver().getId().equals(complainant.getId()))
                .anyMatch(n -> n.getType() == NotificationType.COMPLAINT_RESOLVED_RESPONDENT
                        && n.getReceiver().getId().equals(respondent.getId()));
    }

    private ComplaintReqDTO.UpdateStatusReq completeWith(ComplaintAction action) {
        return ComplaintReqDTO.UpdateStatusReq.builder()
                .status(ComplaintStatus.COMPLETED)
                .action(action)
                .reason("내부 처리 메모")
                .notifySummary("운영 정책 위반을 확인했어요")
                .build();
    }

    private Complaint saveComplaint(ComplaintTargetType targetType, Long targetId, ComplaintStatus status) {
        return saveComplaintBy(complainant, targetType, targetId, status);
    }

    private Complaint saveComplaintBy(Member who, ComplaintTargetType targetType, Long targetId, ComplaintStatus status) {
        return complaintRepository.save(Complaint.builder()
                .complainant(who)
                .respondentMember(respondent)
                .targetType(targetType)
                .targetId(targetId)
                .reasonCode(ComplaintReasonCode.ABUSE)
                .detail("상세 내용")
                .status(status)
                .build());
    }

    private Member saveMember(String clerkId, String nickname, MemberStatus status) {
        return memberRepository.save(Member.builder()
                .clerkId(clerkId)
                .name("테스트")
                .nickname(nickname)
                .mainType(MemberMainType.DEVELOPER)
                .disclosure(true)
                .used(status)
                .build());
    }

    private Project saveProject() {
        Category category = categoryRepository.save(Category.builder().genre(CategoryGenre.ECOMMERCE).build());
        return projectRepository.save(Project.builder()
                .name("프로젝트")
                .content("프로젝트 원문")
                .status(ProjectStatus.RECRUITING)
                .projectField(ProjectField.WEB)
                .mode(ProjectMode.ONLINE)
                .durationRange(DurationRange.ONE_TO_THREE)
                .location("온라인")
                .recruitmentDeadline(LocalDate.now().plusDays(30))
                .category(category)
                .member(respondent)
                .build());
    }

    private ChatMessage saveMessage(Member sender, String content) {
        Member first = complainant.getId() < respondent.getId() ? complainant : respondent;
        Member second = complainant.getId() < respondent.getId() ? respondent : complainant;
        ChatRoom room = chatRoomRepository.save(ChatRoom.builder()
                .member1(first)
                .member2(second)
                .build());
        return chatMessageRepository.save(ChatMessage.builder()
                .chatRoom(room)
                .sender(sender)
                .content(content)
                .build());
    }
}
