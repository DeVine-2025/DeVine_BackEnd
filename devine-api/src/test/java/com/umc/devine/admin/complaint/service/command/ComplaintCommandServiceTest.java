package com.umc.devine.admin.complaint.service.command;

import com.umc.devine.admin.complaint.dto.ComplaintReqDTO;
import com.umc.devine.admin.complaint.dto.ComplaintResDTO;
import com.umc.devine.admin.complaint.entity.Complaint;
import com.umc.devine.admin.complaint.entity.ComplaintHistory;
import com.umc.devine.admin.complaint.enums.ComplaintAction;
import com.umc.devine.admin.complaint.enums.ComplaintStatus;
import com.umc.devine.admin.complaint.enums.ComplaintTargetType;
import com.umc.devine.admin.complaint.exception.ComplaintException;
import com.umc.devine.admin.complaint.repository.ComplaintHistoryRepository;
import com.umc.devine.admin.complaint.repository.ComplaintRepository;
import com.umc.devine.domain.category.entity.Category;
import com.umc.devine.domain.category.enums.CategoryGenre;
import com.umc.devine.domain.category.repository.CategoryRepository;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ComplaintCommandServiceTest extends IntegrationTestSupport {

    @Autowired
    private ComplaintCommandService complaintCommandService;

    @Autowired
    private ComplaintRepository complaintRepository;

    @Autowired
    private ComplaintHistoryRepository complaintHistoryRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private Member complainant;
    private Member respondentMember;
    private Member admin;

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

        admin = memberRepository.save(Member.builder()
                .clerkId("clerk_admin")
                .name("관리자")
                .nickname("admin")
                .mainType(MemberMainType.PM)
                .disclosure(true)
                .used(MemberStatus.ACTIVE)
                .build());
    }

    private Complaint createComplaint(ComplaintStatus status) {
        return createComplaint(ComplaintTargetType.CHAT, 1L, status);
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

    private Project createProject(ProjectStatus status) {
        Category category = categoryRepository.save(Category.builder()
                .genre(CategoryGenre.ECOMMERCE)
                .build());
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
    @DisplayName("updateStatus")
    class UpdateStatusTest {

        @Test
        @DisplayName("존재하지 않는 신고ID면 예외가 발생한다")
        void updateStatus_notFound() {
            // given
            ComplaintReqDTO.UpdateStatusReq request = ComplaintReqDTO.UpdateStatusReq.builder()
                    .status(ComplaintStatus.IN_REVIEW)
                    .build();

            // when & then
            assertThatThrownBy(() -> complaintCommandService.updateStatus(999999L, admin.getId(), request))
                    .isInstanceOf(ComplaintException.class);
        }

        @Test
        @DisplayName("대기에서 검토중으로 상태를 변경한다")
        void updateStatus_toInReview() {
            // given
            Complaint complaint = createComplaint(ComplaintStatus.PENDING);
            ComplaintReqDTO.UpdateStatusReq request = ComplaintReqDTO.UpdateStatusReq.builder()
                    .status(ComplaintStatus.IN_REVIEW)
                    .build();

            // when
            ComplaintResDTO.UpdateStatusRes result = complaintCommandService.updateStatus(complaint.getId(), admin.getId(), request);

            // then
            assertThat(result.status()).isEqualTo(ComplaintStatus.IN_REVIEW);
            assertThat(result.reprocessWarning()).isFalse();
        }

        @Test
        @DisplayName("처리완료 시 세부 액션이 없으면 예외가 발생한다")
        void updateStatus_completedWithoutAction() {
            // given
            Complaint complaint = createComplaint(ComplaintStatus.IN_REVIEW);
            ComplaintReqDTO.UpdateStatusReq request = ComplaintReqDTO.UpdateStatusReq.builder()
                    .status(ComplaintStatus.COMPLETED)
                    .reason("확인 결과 규정 위반")
                    .build();

            // when & then
            assertThatThrownBy(() -> complaintCommandService.updateStatus(complaint.getId(), admin.getId(), request))
                    .isInstanceOf(ComplaintException.class);
        }

        @Test
        @DisplayName("처리완료 시 처리 사유가 없으면 예외가 발생한다")
        void updateStatus_completedWithoutReason() {
            // given
            Complaint complaint = createComplaint(ComplaintStatus.IN_REVIEW);
            ComplaintReqDTO.UpdateStatusReq request = ComplaintReqDTO.UpdateStatusReq.builder()
                    .status(ComplaintStatus.COMPLETED)
                    .action(ComplaintAction.WARNING)
                    .build();

            // when & then
            assertThatThrownBy(() -> complaintCommandService.updateStatus(complaint.getId(), admin.getId(), request))
                    .isInstanceOf(ComplaintException.class);
        }

        @Test
        @DisplayName("액션과 사유가 모두 있으면 처리완료로 변경된다")
        void updateStatus_completed_success() {
            // given
            Complaint complaint = createComplaint(ComplaintStatus.IN_REVIEW);
            ComplaintReqDTO.UpdateStatusReq request = ComplaintReqDTO.UpdateStatusReq.builder()
                    .status(ComplaintStatus.COMPLETED)
                    .action(ComplaintAction.WARNING)
                    .reason("확인 결과 규정 위반")
                    .build();

            // when
            ComplaintResDTO.UpdateStatusRes result = complaintCommandService.updateStatus(complaint.getId(), admin.getId(), request);

            // then
            assertThat(result.status()).isEqualTo(ComplaintStatus.COMPLETED);
            assertThat(result.action()).isEqualTo(ComplaintAction.WARNING);
            assertThat(result.resolutionReason()).isEqualTo("확인 결과 규정 위반");
            assertThat(result.resolvedAt()).isNotNull();
            assertThat(result.reprocessWarning()).isFalse();
        }

        @Test
        @DisplayName("이미 처리완료된 건을 다시 변경하면 경고 플래그가 true다")
        void updateStatus_reprocessWarning() {
            // given
            Complaint complaint = createComplaint(ComplaintStatus.COMPLETED);
            ComplaintReqDTO.UpdateStatusReq request = ComplaintReqDTO.UpdateStatusReq.builder()
                    .status(ComplaintStatus.COMPLETED)
                    .action(ComplaintAction.DISMISS)
                    .reason("재검토 결과 기각")
                    .build();

            // when
            ComplaintResDTO.UpdateStatusRes result = complaintCommandService.updateStatus(complaint.getId(), admin.getId(), request);

            // then
            assertThat(result.reprocessWarning()).isTrue();
        }

        @Test
        @DisplayName("상태 변경 시 처리 이력이 기록된다")
        void updateStatus_recordsHistory() {
            // given
            Complaint complaint = createComplaint(ComplaintStatus.PENDING);
            ComplaintReqDTO.UpdateStatusReq request = ComplaintReqDTO.UpdateStatusReq.builder()
                    .status(ComplaintStatus.IN_REVIEW)
                    .build();

            // when
            complaintCommandService.updateStatus(complaint.getId(), admin.getId(), request);

            // then
            List<ComplaintHistory> histories = complaintHistoryRepository.findByComplaintIdOrderByCreatedAtDesc(complaint.getId());
            assertThat(histories).hasSize(1);
            assertThat(histories.get(0).getStatus()).isEqualTo(ComplaintStatus.IN_REVIEW);
            assertThat(histories.get(0).getResolver().getId()).isEqualTo(admin.getId());
        }

        @Test
        @DisplayName("상태가 여러 번 바뀌면 이력이 누적된다")
        void updateStatus_accumulatesHistory() {
            // given
            Complaint complaint = createComplaint(ComplaintStatus.PENDING);

            // when
            complaintCommandService.updateStatus(complaint.getId(), admin.getId(),
                    ComplaintReqDTO.UpdateStatusReq.builder().status(ComplaintStatus.IN_REVIEW).build());
            complaintCommandService.updateStatus(complaint.getId(), admin.getId(),
                    ComplaintReqDTO.UpdateStatusReq.builder()
                            .status(ComplaintStatus.COMPLETED)
                            .action(ComplaintAction.WARNING)
                            .reason("확인 결과 규정 위반")
                            .build());

            // then
            List<ComplaintHistory> histories = complaintHistoryRepository.findByComplaintIdOrderByCreatedAtDesc(complaint.getId());
            assertThat(histories).hasSize(2);
            assertThat(histories.get(0).getStatus()).isEqualTo(ComplaintStatus.COMPLETED);
            assertThat(histories.get(1).getStatus()).isEqualTo(ComplaintStatus.IN_REVIEW);
        }

        @Test
        @DisplayName("PROJECT 유형 신고를 DELETE로 처리하면 신고된 프로젝트가 비노출(삭제) 처리된다")
        void updateStatus_deleteAction_hidesReportedProject() {
            // given
            Project project = createProject(ProjectStatus.RECRUITING);
            Complaint complaint = createComplaint(ComplaintTargetType.PROJECT, project.getId(), ComplaintStatus.IN_REVIEW);
            ComplaintReqDTO.UpdateStatusReq request = ComplaintReqDTO.UpdateStatusReq.builder()
                    .status(ComplaintStatus.COMPLETED)
                    .action(ComplaintAction.DELETE)
                    .reason("확인 결과 저작권 침해로 비노출 처리")
                    .build();

            // when
            complaintCommandService.updateStatus(complaint.getId(), admin.getId(), request);

            // then
            Project updated = projectRepository.findById(project.getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(ProjectStatus.DELETED);
        }

        @Test
        @DisplayName("이미 삭제된 프로젝트를 DELETE로 다시 처리해도 예외 없이 신고 상태만 변경된다")
        void updateStatus_deleteAction_alreadyDeletedProject() {
            // given
            Project project = createProject(ProjectStatus.DELETED);
            Complaint complaint = createComplaint(ComplaintTargetType.PROJECT, project.getId(), ComplaintStatus.IN_REVIEW);
            ComplaintReqDTO.UpdateStatusReq request = ComplaintReqDTO.UpdateStatusReq.builder()
                    .status(ComplaintStatus.COMPLETED)
                    .action(ComplaintAction.DELETE)
                    .reason("이미 삭제된 프로젝트지만 신고는 처리")
                    .build();

            // when
            ComplaintResDTO.UpdateStatusRes result = complaintCommandService.updateStatus(complaint.getId(), admin.getId(), request);

            // then
            assertThat(result.status()).isEqualTo(ComplaintStatus.COMPLETED);
        }
    }
}
