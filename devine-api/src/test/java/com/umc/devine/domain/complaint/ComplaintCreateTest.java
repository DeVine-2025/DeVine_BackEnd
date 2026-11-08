package com.umc.devine.domain.complaint;

import com.umc.devine.domain.bookmark.entity.Bookmark;
import com.umc.devine.domain.bookmark.enums.BookmarkType;
import com.umc.devine.domain.bookmark.repository.BookmarkRepository;
import com.umc.devine.domain.bookmark.service.query.BookmarkQueryService;
import com.umc.devine.domain.category.entity.Category;
import com.umc.devine.domain.category.enums.CategoryGenre;
import com.umc.devine.domain.category.repository.CategoryRepository;
import com.umc.devine.domain.chat.entity.ChatMessage;
import com.umc.devine.domain.chat.entity.ChatRoom;
import com.umc.devine.domain.chat.repository.ChatMessageRepository;
import com.umc.devine.domain.chat.repository.ChatRoomRepository;
import com.umc.devine.domain.complaint.dto.ComplaintReqDTO;
import com.umc.devine.domain.complaint.dto.ComplaintResDTO;
import com.umc.devine.domain.complaint.entity.Complaint;
import com.umc.devine.domain.complaint.enums.ComplaintAction;
import com.umc.devine.domain.complaint.enums.ComplaintReasonCode;
import com.umc.devine.domain.complaint.enums.ComplaintStatus;
import com.umc.devine.domain.complaint.enums.ComplaintTargetType;
import com.umc.devine.domain.complaint.exception.ComplaintException;
import com.umc.devine.domain.complaint.exception.code.ComplaintErrorReason;
import com.umc.devine.domain.complaint.repository.ComplaintRepository;
import com.umc.devine.domain.complaint.service.command.ComplaintCommandService;
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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("FUNC-REP-001 신고 접수")
class ComplaintCreateTest extends IntegrationTestSupport {

    @Autowired
    private ComplaintCommandService complaintCommandService;
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
    @Autowired
    private BookmarkRepository bookmarkRepository;
    @Autowired
    private BookmarkQueryService bookmarkQueryService;

    private Member complainant;
    private Member respondent;

    @BeforeEach
    void setUp() {
        complainant = saveMember("clerk_c", "complainant");
        respondent = saveMember("clerk_r", "respondent");
    }

    @Nested
    @DisplayName("프로젝트 신고")
    class ProjectComplaint {

        @Test
        @DisplayName("접수되면 즉시 가려지고 PM에게 알림이 간다")
        void create_blindsProjectAndNotifiesPm() {
            Project project = saveProject(respondent, ProjectStatus.RECRUITING);

            ComplaintResDTO.CreateRes result = create(ComplaintTargetType.PROJECT, project.getId(), null,
                    ComplaintReasonCode.FALSE_INFORMATION, null);

            Project blinded = projectRepository.findById(project.getId()).orElseThrow();
            assertThat(blinded.isReportBlinded()).isTrue();
            assertThat(blinded.isHidden()).isTrue();
            assertThat(blinded.getStatus()).isEqualTo(ProjectStatus.RECRUITING);

            Complaint saved = complaintRepository.findById(result.complaintId()).orElseThrow();
            assertThat(saved.getStatus()).isEqualTo(ComplaintStatus.PENDING);
            assertThat(saved.getRespondentMember().getId()).isEqualTo(respondent.getId());
            assertThat(result.blockTargetNickname()).isEqualTo("respondent");

            assertThat(notificationRepository.findAll())
                    .anyMatch(n -> n.getType() == NotificationType.COMPLAINT_PROJECT_BLINDED
                            && n.getReceiver().getId().equals(respondent.getId()));
        }

        @Test
        @DisplayName("가려지면 북마크 목록에서도 빠진다")
        void create_hidesFromBookmarkList() {
            Project project = saveProject(respondent, ProjectStatus.RECRUITING);
            bookmarkRepository.save(Bookmark.builder()
                    .member(complainant)
                    .targetType(BookmarkType.PROJECT)
                    .targetId(project.getId())
                    .build());
            assertThat(bookmarkQueryService.findAllBookmarks(complainant).bookmarks()).hasSize(1);

            create(ComplaintTargetType.PROJECT, project.getId(), null, ComplaintReasonCode.SPAM, null);

            assertThat(bookmarkQueryService.findAllBookmarks(complainant).bookmarks()).isEmpty();
        }

        @Test
        @DisplayName("본인 프로젝트는 신고할 수 없다")
        void create_selfProject() {
            Project mine = saveProject(complainant, ProjectStatus.RECRUITING);

            assertReason(() -> create(ComplaintTargetType.PROJECT, mine.getId(), null, ComplaintReasonCode.SPAM, null),
                    ComplaintErrorReason.SELF_COMPLAINT);
        }

        @Test
        @DisplayName("같은 프로젝트는 기각된 뒤에도 다시 신고할 수 없다")
        void create_duplicateProject() {
            Project project = saveProject(respondent, ProjectStatus.RECRUITING);
            Long complaintId = create(ComplaintTargetType.PROJECT, project.getId(), null,
                    ComplaintReasonCode.SPAM, null).complaintId();
            Complaint complaint = complaintRepository.findById(complaintId).orElseThrow();
            Project blinded = projectRepository.findById(project.getId()).orElseThrow();
            complaint.updateStatus(ComplaintStatus.COMPLETED, ComplaintAction.DISMISS, "위반 아님", "위반 아님",
                    null, LocalDateTime.now());
            complaintRepository.saveAndFlush(complaint);
            blinded.changeReportBlind(false, LocalDateTime.now());
            projectRepository.saveAndFlush(blinded);

            assertReason(() -> create(ComplaintTargetType.PROJECT, project.getId(), null, ComplaintReasonCode.SPAM, null),
                    ComplaintErrorReason.DUPLICATE_TARGET);
        }

        @Test
        @DisplayName("이미 가려진 프로젝트는 볼 수 없으므로 대상 없음이다")
        void create_alreadyBlindedProject() {
            Project project = saveProject(respondent, ProjectStatus.RECRUITING);
            project.changeReportBlind(true, LocalDateTime.now());
            projectRepository.saveAndFlush(project);

            assertReason(() -> create(ComplaintTargetType.PROJECT, project.getId(), null, ComplaintReasonCode.SPAM, null),
                    ComplaintErrorReason.TARGET_NOT_FOUND);
        }

        @Test
        @DisplayName("없는 프로젝트는 대상 없음이다")
        void create_projectNotFound() {
            assertReason(() -> create(ComplaintTargetType.PROJECT, 999999L, null, ComplaintReasonCode.SPAM, null),
                    ComplaintErrorReason.TARGET_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("회원 신고")
    class DeveloperComplaint {

        @Test
        @DisplayName("닉네임으로 접수되고 프로필은 가리지 않는다")
        void create_success() {
            ComplaintResDTO.CreateRes result = create(ComplaintTargetType.DEVELOPER, null, "respondent",
                    ComplaintReasonCode.ABUSE, "욕설을 했어요");

            Complaint saved = complaintRepository.findById(result.complaintId()).orElseThrow();
            assertThat(saved.getTargetId()).isNull();
            assertThat(saved.getRespondentMember().getId()).isEqualTo(respondent.getId());
            assertThat(result.targetName()).isEqualTo("respondent");
        }

        @Test
        @DisplayName("처리 중인 신고가 있으면 같은 회원을 다시 신고할 수 없다")
        void create_duplicateWhileInProgress() {
            create(ComplaintTargetType.DEVELOPER, null, "respondent", ComplaintReasonCode.ABUSE, null);

            assertReason(() -> create(ComplaintTargetType.DEVELOPER, null, "respondent", ComplaintReasonCode.ABUSE, null),
                    ComplaintErrorReason.DUPLICATE_RESPONDENT);
        }

        @Test
        @DisplayName("앞선 신고가 처리완료되면 같은 회원을 다시 신고할 수 있다")
        void create_afterCompleted() {
            Long first = create(ComplaintTargetType.DEVELOPER, null, "respondent",
                    ComplaintReasonCode.ABUSE, null).complaintId();
            Complaint complaint = complaintRepository.findById(first).orElseThrow();
            complaint.updateStatus(ComplaintStatus.COMPLETED, ComplaintAction.WARNING, "경고", "경고",
                    null, LocalDateTime.now());
            complaintRepository.saveAndFlush(complaint);

            ComplaintResDTO.CreateRes second = create(ComplaintTargetType.DEVELOPER, null, "respondent",
                    ComplaintReasonCode.ABUSE, null);

            assertThat(second.complaintId()).isNotEqualTo(first);
        }

        @Test
        @DisplayName("탈퇴한 회원은 대상 없음이다")
        void create_withdrawnMember() {
            respondent.withdraw();
            memberRepository.saveAndFlush(respondent);

            assertReason(() -> create(ComplaintTargetType.DEVELOPER, null, "respondent", ComplaintReasonCode.ABUSE, null),
                    ComplaintErrorReason.TARGET_NOT_FOUND);
        }

        @Test
        @DisplayName("본인은 신고할 수 없다")
        void create_self() {
            assertReason(() -> create(ComplaintTargetType.DEVELOPER, null, "complainant", ComplaintReasonCode.ABUSE, null),
                    ComplaintErrorReason.SELF_COMPLAINT);
        }
    }

    @Nested
    @DisplayName("채팅 메시지 신고")
    class ChatComplaint {

        @Test
        @DisplayName("상대가 보낸 메시지를 신고하면 접수된다")
        void create_success() {
            ChatMessage message = saveMessage(complainant, respondent, respondent, "욕설 메시지");

            ComplaintResDTO.CreateRes result = create(ComplaintTargetType.CHAT, message.getId(), null,
                    ComplaintReasonCode.ABUSE, null);

            Complaint saved = complaintRepository.findById(result.complaintId()).orElseThrow();
            assertThat(saved.getRespondentMember().getId()).isEqualTo(respondent.getId());
            assertThat(result.targetName()).isEqualTo("채팅 메시지");
            assertThat(chatMessageRepository.findById(message.getId()).orElseThrow().isHiddenByPolicy()).isFalse();
        }

        @Test
        @DisplayName("내가 보낸 메시지는 신고할 수 없다")
        void create_ownMessage() {
            ChatMessage message = saveMessage(complainant, respondent, complainant, "내 메시지");

            assertReason(() -> create(ComplaintTargetType.CHAT, message.getId(), null, ComplaintReasonCode.ABUSE, null),
                    ComplaintErrorReason.SELF_COMPLAINT);
        }

        @Test
        @DisplayName("참여하지 않은 방의 메시지는 존재 여부를 알려주지 않는다")
        void create_otherRoomMessage() {
            Member other = saveMember("clerk_o", "other");
            ChatMessage message = saveMessage(other, respondent, respondent, "남의 방 메시지");

            assertReason(() -> create(ComplaintTargetType.CHAT, message.getId(), null, ComplaintReasonCode.ABUSE, null),
                    ComplaintErrorReason.TARGET_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("입력값과 악용 방지")
    class Validation {

        @Test
        @DisplayName("사유가 기타면 상세 내용이 필수다")
        void create_etcWithoutDetail() {
            assertReason(() -> create(ComplaintTargetType.DEVELOPER, null, "respondent", ComplaintReasonCode.ETC, " "),
                    ComplaintErrorReason.DETAIL_REQUIRED);
        }

        @Test
        @DisplayName("하루 5건을 넘기면 접수되지 않는다")
        void create_dailyLimit() {
            for (int i = 0; i < 5; i++) {
                Project project = saveProject(respondent, ProjectStatus.RECRUITING);
                create(ComplaintTargetType.PROJECT, project.getId(), null, ComplaintReasonCode.SPAM, null);
            }
            Project sixth = saveProject(respondent, ProjectStatus.RECRUITING);

            assertReason(() -> create(ComplaintTargetType.PROJECT, sixth.getId(), null, ComplaintReasonCode.SPAM, null),
                    ComplaintErrorReason.DAILY_LIMIT_EXCEEDED);
        }
    }

    private ComplaintResDTO.CreateRes create(ComplaintTargetType targetType, Long targetId, String nickname,
                                             ComplaintReasonCode reasonCode, String detail) {
        return complaintCommandService.create(complainant, ComplaintReqDTO.CreateReq.builder()
                .targetType(targetType)
                .targetId(targetId)
                .targetNickname(nickname)
                .reasonCode(reasonCode)
                .detail(detail)
                .build());
    }

    private void assertReason(Runnable call, ComplaintErrorReason expected) {
        assertThatThrownBy(call::run)
                .isInstanceOf(ComplaintException.class)
                .extracting("reason").isEqualTo(expected);
    }

    private Member saveMember(String clerkId, String nickname) {
        return memberRepository.save(Member.builder()
                .clerkId(clerkId)
                .name("테스트")
                .nickname(nickname)
                .mainType(MemberMainType.DEVELOPER)
                .disclosure(true)
                .used(MemberStatus.ACTIVE)
                .build());
    }

    private Project saveProject(Member owner, ProjectStatus status) {
        Category category = categoryRepository.save(Category.builder().genre(CategoryGenre.ECOMMERCE).build());
        return projectRepository.save(Project.builder()
                .name("프로젝트")
                .content("프로젝트 원문")
                .status(status)
                .projectField(ProjectField.WEB)
                .mode(ProjectMode.ONLINE)
                .durationRange(DurationRange.ONE_TO_THREE)
                .location("온라인")
                .recruitmentDeadline(LocalDate.now().plusDays(30))
                .category(category)
                .member(owner)
                .build());
    }

    private ChatMessage saveMessage(Member member1, Member member2, Member sender, String content) {
        Member first = member1.getId() < member2.getId() ? member1 : member2;
        Member second = member1.getId() < member2.getId() ? member2 : member1;
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
