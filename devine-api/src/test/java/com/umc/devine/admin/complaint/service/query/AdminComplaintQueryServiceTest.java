package com.umc.devine.admin.complaint.service.query;

import com.umc.devine.admin.complaint.dto.ComplaintReqDTO;
import com.umc.devine.admin.complaint.dto.ComplaintResDTO;
import com.umc.devine.admin.complaint.entity.Complaint;
import com.umc.devine.admin.complaint.enums.ComplaintStatus;
import com.umc.devine.admin.complaint.enums.ComplaintTargetType;
import com.umc.devine.admin.complaint.exception.ComplaintException;
import com.umc.devine.admin.complaint.repository.ComplaintRepository;
import com.umc.devine.domain.category.entity.Category;
import com.umc.devine.domain.category.enums.CategoryGenre;
import com.umc.devine.domain.category.repository.CategoryRepository;
import com.umc.devine.domain.chat.entity.ChatMessage;
import com.umc.devine.domain.chat.entity.ChatRoom;
import com.umc.devine.domain.chat.repository.ChatMessageRepository;
import com.umc.devine.domain.chat.repository.ChatRoomRepository;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.enums.MemberMainType;
import com.umc.devine.domain.member.enums.MemberStatus;
import com.umc.devine.domain.member.repository.MemberRepository;
import com.umc.devine.domain.project.entity.Project;
import com.umc.devine.domain.project.enums.DurationRange;
import com.umc.devine.domain.project.enums.ProjectField;
import com.umc.devine.domain.project.enums.ProjectMode;
import com.umc.devine.domain.project.enums.ProjectStatus;
import com.umc.devine.domain.project.repository.ProjectRepository;
import com.umc.devine.support.IntegrationTestSupport;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ComplaintQueryServiceTest extends IntegrationTestSupport {

    @Autowired
    private ComplaintQueryService complaintQueryService;

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
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    private Member complainant;
    private Member respondentMember;
    private Category category;

    @BeforeEach
    void setUp() {
        complainant = memberRepository.save(Member.builder()
                .clerkId("clerk_complainant")
                .name("신고자")
                .nickname("complainant")
                .mainType(MemberMainType.DEVELOPER)
                .disclosure(true)
                .used(MemberStatus.ACTIVE)
                .build());

        respondentMember = memberRepository.save(Member.builder()
                .clerkId("clerk_respondent")
                .name("피신고자")
                .nickname("respondent")
                .mainType(MemberMainType.DEVELOPER)
                .disclosure(true)
                .used(MemberStatus.ACTIVE)
                .build());

        category = categoryRepository.save(Category.builder()
                .genre(CategoryGenre.ECOMMERCE)
                .build());
    }

    private Complaint createComplaint(ComplaintTargetType targetType, Long targetId, ComplaintStatus status) {
        return complaintRepository.save(Complaint.builder()
                .complainant(complainant)
                .respondentMember(respondentMember)
                .targetType(targetType)
                .targetId(targetId)
                .reason("부적절한 콘텐츠입니다.")
                .status(status)
                .build());
    }

    private void backdateCreatedAt(Long complaintId, LocalDateTime createdAt) {
        entityManager.flush();
        jdbcTemplate.update("UPDATE complaint SET created_at = ? WHERE complaint_id = ?", createdAt, complaintId);
        entityManager.clear();
    }

    private ChatMessage createChatMessage(String content) {
        ChatRoom chatRoom = chatRoomRepository.save(ChatRoom.builder()
                .member1(complainant)
                .member2(respondentMember)
                .build());
        return chatMessageRepository.save(ChatMessage.builder()
                .chatRoom(chatRoom)
                .sender(respondentMember)
                .content(content)
                .build());
    }

    private Project createProject(ProjectStatus status) {
        return projectRepository.save(Project.builder()
                .name("프로젝트")
                .content("프로젝트 원문 내용입니다.")
                .status(status)
                .projectField(ProjectField.WEB)
                .mode(ProjectMode.ONLINE)
                .durationRange(DurationRange.ONE_TO_THREE)
                .location("온라인")
                .recruitmentDeadline(LocalDate.now().plusDays(30))
                .category(category)
                .member(respondentMember)
                .build());
    }

    @Nested
    @DisplayName("getComplaintList")
    class GetComplaintListTest {

        @Test
        @DisplayName("필터 없이 전체 목록을 조회한다")
        void getComplaintList_noFilter() {
            // given
            createComplaint(ComplaintTargetType.CHAT, 1L, ComplaintStatus.PENDING);
            createComplaint(ComplaintTargetType.PROJECT, 2L, ComplaintStatus.PENDING);

            // when
            var result = complaintQueryService.getComplaintList(ComplaintReqDTO.SearchReq.builder().build());

            // then
            assertThat(result.getContent()).hasSize(2);
        }

        @Test
        @DisplayName("신고 유형으로 필터링한다")
        void getComplaintList_byTargetType() {
            // given
            createComplaint(ComplaintTargetType.CHAT, 1L, ComplaintStatus.PENDING);
            createComplaint(ComplaintTargetType.PROJECT, 2L, ComplaintStatus.PENDING);

            // when
            var result = complaintQueryService.getComplaintList(ComplaintReqDTO.SearchReq.builder()
                    .targetType(ComplaintTargetType.PROJECT)
                    .build());

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).targetType()).isEqualTo(ComplaintTargetType.PROJECT);
        }

        @Test
        @DisplayName("처리 상태로 필터링한다")
        void getComplaintList_byStatus() {
            // given
            createComplaint(ComplaintTargetType.CHAT, 1L, ComplaintStatus.PENDING);
            createComplaint(ComplaintTargetType.CHAT, 2L, ComplaintStatus.COMPLETED);

            // when
            var result = complaintQueryService.getComplaintList(ComplaintReqDTO.SearchReq.builder()
                    .status(ComplaintStatus.COMPLETED)
                    .build());

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).status()).isEqualTo(ComplaintStatus.COMPLETED);
        }

        @Test
        @DisplayName("페이지네이션이 동작한다")
        void getComplaintList_pagination() {
            // given
            createComplaint(ComplaintTargetType.CHAT, 1L, ComplaintStatus.PENDING);
            createComplaint(ComplaintTargetType.CHAT, 2L, ComplaintStatus.PENDING);
            createComplaint(ComplaintTargetType.CHAT, 3L, ComplaintStatus.PENDING);

            // when
            var result = complaintQueryService.getComplaintList(ComplaintReqDTO.SearchReq.builder()
                    .page(1).size(2).build());

            // then
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getTotalElements()).isEqualTo(3);
        }

        @Test
        @DisplayName("접수 후 48시간 초과 미처리 건은 SLA초과여부가 true다")
        void getComplaintList_slaExceeded() {
            // given
            Complaint overdue = createComplaint(ComplaintTargetType.CHAT, 1L, ComplaintStatus.PENDING);
            backdateCreatedAt(overdue.getId(), LocalDateTime.now().minusHours(49));
            createComplaint(ComplaintTargetType.CHAT, 2L, ComplaintStatus.PENDING);

            // when
            var result = complaintQueryService.getComplaintList(ComplaintReqDTO.SearchReq.builder().build());

            // then
            var overdueDto = result.getContent().stream().filter(r -> r.complaintId().equals(overdue.getId())).findFirst().orElseThrow();
            var recentDto = result.getContent().stream().filter(r -> !r.complaintId().equals(overdue.getId())).findFirst().orElseThrow();
            assertThat(overdueDto.slaExceeded()).isTrue();
            assertThat(recentDto.slaExceeded()).isFalse();
        }

        @Test
        @DisplayName("처리완료된 건은 48시간이 지나도 SLA초과여부가 false다")
        void getComplaintList_completedNeverExceeded() {
            // given
            Complaint completed = createComplaint(ComplaintTargetType.CHAT, 1L, ComplaintStatus.COMPLETED);
            backdateCreatedAt(completed.getId(), LocalDateTime.now().minusHours(72));

            // when
            var result = complaintQueryService.getComplaintList(ComplaintReqDTO.SearchReq.builder().build());

            // then
            assertThat(result.getContent().get(0).slaExceeded()).isFalse();
        }
    }

    @Nested
    @DisplayName("getComplaintDetail")
    class GetComplaintDetailTest {

        @Test
        @DisplayName("존재하지 않는 신고ID면 예외가 발생한다")
        void getComplaintDetail_notFound() {
            assertThatThrownBy(() -> complaintQueryService.getComplaintDetail(999999L))
                    .isInstanceOf(ComplaintException.class);
        }

        @Test
        @DisplayName("PROJECT 유형 신고는 프로젝트 원문을 반환한다")
        void getComplaintDetail_project() {
            // given
            Project project = createProject(ProjectStatus.RECRUITING);
            Complaint complaint = createComplaint(ComplaintTargetType.PROJECT, project.getId(), ComplaintStatus.PENDING);

            // when
            ComplaintResDTO.ComplaintDetailRes result = complaintQueryService.getComplaintDetail(complaint.getId());

            // then
            assertThat(result.content()).isEqualTo("프로젝트 원문 내용입니다.");
        }

        @Test
        @DisplayName("삭제된 프로젝트에 대한 신고는 삭제 안내 문구를 반환한다")
        void getComplaintDetail_deletedProject() {
            // given
            Project project = createProject(ProjectStatus.DELETED);
            Complaint complaint = createComplaint(ComplaintTargetType.PROJECT, project.getId(), ComplaintStatus.PENDING);

            // when
            ComplaintResDTO.ComplaintDetailRes result = complaintQueryService.getComplaintDetail(complaint.getId());

            // then
            assertThat(result.content()).isEqualTo("삭제된 콘텐츠입니다");
        }

        @Test
        @DisplayName("CHAT 유형 신고는 신고당한 그 메시지의 원문을 반환한다")
        void getComplaintDetail_chat() {
            // given
            ChatMessage message = createChatMessage("이 프로젝트 진짜 별로네");
            Complaint complaint = createComplaint(ComplaintTargetType.CHAT, message.getId(), ComplaintStatus.PENDING);

            // when
            ComplaintResDTO.ComplaintDetailRes result = complaintQueryService.getComplaintDetail(complaint.getId());

            // then
            assertThat(result.content()).isEqualTo("이 프로젝트 진짜 별로네");
        }

        @Test
        @DisplayName("삭제된(존재하지 않는) 메시지에 대한 신고는 삭제 안내 문구를 반환한다")
        void getComplaintDetail_deletedChatMessage() {
            // given
            Complaint complaint = createComplaint(ComplaintTargetType.CHAT, 999999L, ComplaintStatus.PENDING);

            // when
            ComplaintResDTO.ComplaintDetailRes result = complaintQueryService.getComplaintDetail(complaint.getId());

            // then
            assertThat(result.content()).isEqualTo("삭제된 콘텐츠입니다");
        }

        @Test
        @DisplayName("DEVELOPER 유형 신고는 콘텐츠가 없다")
        void getComplaintDetail_developer() {
            // given
            Complaint complaint = createComplaint(ComplaintTargetType.DEVELOPER, null, ComplaintStatus.PENDING);

            // when
            ComplaintResDTO.ComplaintDetailRes result = complaintQueryService.getComplaintDetail(complaint.getId());

            // then
            assertThat(result.content()).isNull();
        }

        @Test
        @DisplayName("피신고자의 누적 신고 건수와 이력을 함께 반환한다")
        void getComplaintDetail_respondentHistory() {
            // given
            createComplaint(ComplaintTargetType.CHAT, 1L, ComplaintStatus.COMPLETED);
            Complaint target = createComplaint(ComplaintTargetType.CHAT, 2L, ComplaintStatus.PENDING);

            // when
            ComplaintResDTO.ComplaintDetailRes result = complaintQueryService.getComplaintDetail(target.getId());

            // then
            assertThat(result.respondentComplaintCount()).isEqualTo(2);
            assertThat(result.respondentHistory()).hasSize(2);
        }
    }
}
