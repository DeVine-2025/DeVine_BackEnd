package com.umc.devine.domain.project.service.command;

import com.umc.devine.domain.category.entity.Category;
import com.umc.devine.domain.category.enums.CategoryGenre;
import com.umc.devine.domain.category.repository.CategoryRepository;
import com.umc.devine.domain.image.entity.Image;
import com.umc.devine.domain.image.enums.ImageType;
import com.umc.devine.domain.image.exception.ImageException;
import com.umc.devine.domain.image.repository.ImageRepository;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.enums.MemberMainType;
import com.umc.devine.domain.member.enums.MemberStatus;
import com.umc.devine.domain.member.repository.MemberRepository;
import com.umc.devine.domain.project.dto.ProjectReqDTO;
import com.umc.devine.domain.project.dto.ProjectResDTO;
import com.umc.devine.domain.project.entity.Project;
import com.umc.devine.domain.project.enums.*;
import com.umc.devine.domain.project.exception.ProjectException;
import com.umc.devine.domain.project.repository.ProjectRepository;
import com.umc.devine.domain.techstack.entity.Techstack;
import com.umc.devine.domain.techstack.enums.TechGenre;
import com.umc.devine.domain.techstack.enums.TechName;
import com.umc.devine.domain.techstack.repository.ProjectRequirementTechstackRepository;
import com.umc.devine.domain.techstack.repository.TechstackRepository;
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

class ProjectCommandServiceTest extends IntegrationTestSupport {

    @Autowired
    private ProjectCommandService projectCommandService;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private TechstackRepository techstackRepository;

    @Autowired
    private ProjectRequirementTechstackRepository projectRequirementTechstackRepository;

    private Member pmMember;
    private Member devMember;
    private Category testCategory;
    private Techstack backendParent;
    private Techstack javaTechstack;

    @BeforeEach
    void setUp() {
        pmMember = memberRepository.save(Member.builder()
                .clerkId("clerk_pm_123")
                .name("PM유저")
                .nickname("pmuser")
                .mainType(MemberMainType.PM)
                .disclosure(true)
                .used(MemberStatus.ACTIVE)
                .build());

        devMember = memberRepository.save(Member.builder()
                .clerkId("clerk_dev_123")
                .name("개발자유저")
                .nickname("devuser")
                .mainType(MemberMainType.DEVELOPER)
                .disclosure(true)
                .used(MemberStatus.ACTIVE)
                .build());

        testCategory = categoryRepository.save(Category.builder()
                .genre(CategoryGenre.ECOMMERCE)
                .build());

        backendParent = techstackRepository.save(Techstack.builder()
                .name(TechName.BACKEND)
                .genre(null)
                .build());

        javaTechstack = techstackRepository.save(Techstack.builder()
                .name(TechName.JAVA)
                .genre(TechGenre.LANGUAGE)
                .parentStack(backendParent)
                .build());
    }

    private ProjectReqDTO.CreateProjectReq createValidRequest() {
        return createValidRequest(null);
    }

    private ProjectReqDTO.CreateProjectReq createValidRequest(List<Long> imageIds) {
        return ProjectReqDTO.CreateProjectReq.builder()
                .projectField(ProjectField.WEB)
                .category(CategoryGenre.ECOMMERCE)
                .mode(ProjectMode.ONLINE)
                .durationRange(DurationRange.ONE_TO_THREE)
                .location("온라인")
                .recruitmentDeadline(LocalDate.now().plusDays(30))
                .recruitments(List.of(
                        ProjectReqDTO.RecruitmentDTO.builder()
                                .position(ProjectPart.BACKEND)
                                .count(2)
                                .techStacks(List.of(TechName.JAVA))
                                .build()
                ))
                .title("테스트 프로젝트")
                .content("테스트 프로젝트 내용입니다.")
                .imageIds(imageIds)
                .build();
    }

    private Image createUploadedProjectImage(Member owner) {
        return imageRepository.save(Image.builder()
                .imageType(ImageType.PROJECT)
                .imageUrl("https://cdn.test.com/test.jpg")
                .s3Key("projects/2026/01/01/test.jpg")
                .uploaded(true)
                .clerkId(owner.getClerkId())
                .uploader(owner)
                .build());
    }

    @Nested
    @DisplayName("프로젝트 생성")
    class CreateProjectTest {

        @Test
        @DisplayName("회원이 프로젝트 생성에 성공한다")
        void createProject_success() {
            // given
            ProjectReqDTO.CreateProjectReq request = createValidRequest();

            // when
            ProjectResDTO.CreateProjectRes result = projectCommandService.createProject(pmMember, request);

            // then
            assertThat(result.projectId()).isNotNull();
            assertThat(result.title()).isEqualTo("테스트 프로젝트");
            assertThat(result.projectField()).isEqualTo(ProjectField.WEB);
            assertThat(result.category()).isEqualTo(CategoryGenre.ECOMMERCE);
            assertThat(result.status()).isEqualTo(ProjectStatus.RECRUITING);
            assertThat(result.recruitments()).hasSize(1);
            assertThat(result.recruitments().get(0).position()).isEqualTo(ProjectPart.BACKEND);
            assertThat(result.recruitments().get(0).count()).isEqualTo(2);
        }

        @Test
        @DisplayName("회원이 이미지를 포함하여 프로젝트를 생성한다")
        void createProject_withImages() {
            // given
            Image image = createUploadedProjectImage(pmMember);
            ProjectReqDTO.CreateProjectReq request = createValidRequest(List.of(image.getId()));

            // when
            ProjectResDTO.CreateProjectRes result = projectCommandService.createProject(pmMember, request);

            // then
            assertThat(result.images()).hasSize(1);
            assertThat(result.images().get(0).imageUrl()).isEqualTo("https://cdn.test.com/test.jpg");
        }

        @Test
        @DisplayName("개발자 회원도 프로젝트 생성에 성공한다")
        void createProject_developerSuccess() {
            // given
            ProjectReqDTO.CreateProjectReq request = createValidRequest();

            // when
            ProjectResDTO.CreateProjectRes result = projectCommandService.createProject(devMember, request);

            // then
            assertThat(result.projectId()).isNotNull();
            assertThat(result.title()).isEqualTo("테스트 프로젝트");
        }

        @Test
        @DisplayName("모집 마감일이 과거이면 예외가 발생한다")
        void createProject_pastDeadline() {
            // given
            ProjectReqDTO.CreateProjectReq request = ProjectReqDTO.CreateProjectReq.builder()
                    .projectField(ProjectField.WEB)
                    .category(CategoryGenre.ECOMMERCE)
                    .mode(ProjectMode.ONLINE)
                    .durationRange(DurationRange.ONE_TO_THREE)
                    .location("온라인")
                    .recruitmentDeadline(LocalDate.now().minusDays(1))
                    .recruitments(List.of(
                            ProjectReqDTO.RecruitmentDTO.builder()
                                    .position(ProjectPart.BACKEND)
                                    .count(2)
                                    .build()
                    ))
                    .title("테스트 프로젝트")
                    .content("테스트 프로젝트 내용입니다.")
                    .build();

            // when & then
            assertThatThrownBy(() -> projectCommandService.createProject(pmMember, request))
                    .isInstanceOf(ProjectException.class);
        }

        @Test
        @DisplayName("업로드되지 않은 이미지를 첨부하면 예외가 발생한다")
        void createProject_unuploadedImage() {
            // given
            Image unuploadedImage = imageRepository.save(Image.builder()
                    .imageType(ImageType.PROJECT)
                    .imageUrl("https://cdn.test.com/unuploaded.jpg")
                    .s3Key("projects/2026/01/01/unuploaded.jpg")
                    .uploaded(false)
                    .clerkId(pmMember.getClerkId())
                    .uploader(pmMember)
                    .build());

            ProjectReqDTO.CreateProjectReq request = createValidRequest(List.of(unuploadedImage.getId()));

            // when & then
            assertThatThrownBy(() -> projectCommandService.createProject(pmMember, request))
                    .isInstanceOf(ImageException.class);
        }

        @Test
        @DisplayName("다른 사용자의 이미지를 첨부하면 예외가 발생한다")
        void createProject_otherUserImage() {
            // given
            Image otherImage = createUploadedProjectImage(devMember);
            ProjectReqDTO.CreateProjectReq request = createValidRequest(List.of(otherImage.getId()));

            // when & then
            assertThatThrownBy(() -> projectCommandService.createProject(pmMember, request))
                    .isInstanceOf(ImageException.class);
        }

        @Test
        @DisplayName("PROJECT 타입이 아닌 이미지를 첨부하면 예외가 발생한다")
        void createProject_wrongImageType() {
            // given
            Image profileImage = imageRepository.save(Image.builder()
                    .imageType(ImageType.PROFILE)
                    .imageUrl("https://cdn.test.com/profile.jpg")
                    .s3Key("profiles/1/profile.jpg")
                    .uploaded(true)
                    .clerkId(pmMember.getClerkId())
                    .uploader(pmMember)
                    .build());

            ProjectReqDTO.CreateProjectReq request = createValidRequest(List.of(profileImage.getId()));

            // when & then
            assertThatThrownBy(() -> projectCommandService.createProject(pmMember, request))
                    .isInstanceOf(ImageException.class);
        }
    }

    @Nested
    @DisplayName("프로젝트 수정")
    class UpdateProjectTest {

        private Project savedProject;

        @BeforeEach
        void setUp() {
            savedProject = projectRepository.save(Project.builder()
                    .name("원본 프로젝트")
                    .content("원본 내용")
                    .status(ProjectStatus.RECRUITING)
                    .projectField(ProjectField.WEB)
                    .mode(ProjectMode.ONLINE)
                    .durationRange(DurationRange.ONE_TO_THREE)
                    .location("온라인")
                    .recruitmentDeadline(LocalDate.now().plusDays(30))
                    .category(testCategory)
                    .member(pmMember)
                    .build());
        }

        @Test
        @DisplayName("프로젝트 수정에 성공한다")
        void updateProject_success() {
            // given
            ProjectReqDTO.UpdateProjectReq request = ProjectReqDTO.UpdateProjectReq.builder()
                    .projectField(ProjectField.MOBILE)
                    .category(CategoryGenre.ECOMMERCE)
                    .mode(ProjectMode.OFFLINE)
                    .durationRange(DurationRange.THREE_TO_SIX)
                    .location("서울 강남구")
                    .recruitmentDeadline(LocalDate.now().plusDays(60))
                    .recruitments(List.of(
                            ProjectReqDTO.RecruitmentDTO.builder()
                                    .position(ProjectPart.BACKEND)
                                    .count(3)
                                    .techStacks(List.of(TechName.JAVA))
                                    .build()
                    ))
                    .title("수정된 프로젝트")
                    .content("수정된 내용")
                    .build();

            // when
            ProjectResDTO.UpdateProjectRes result = projectCommandService.updateProject(
                    pmMember, savedProject.getId(), request);

            // then
            assertThat(result.title()).isEqualTo("수정된 프로젝트");
            assertThat(result.projectField()).isEqualTo(ProjectField.MOBILE);
            assertThat(result.location()).isEqualTo("서울 강남구");
        }

        @Test
        @DisplayName("프로젝트 소유자가 아닌 회원이 수정하면 예외가 발생한다")
        void updateProject_notOwner() {
            // given
            Member otherPm = memberRepository.save(Member.builder()
                    .clerkId("clerk_other_pm")
                    .name("다른PM")
                    .nickname("otherpm")
                    .mainType(MemberMainType.PM)
                    .disclosure(true)
                    .used(MemberStatus.ACTIVE)
                    .build());

            ProjectReqDTO.UpdateProjectReq request = ProjectReqDTO.UpdateProjectReq.builder()
                    .projectField(ProjectField.WEB)
                    .category(CategoryGenre.ECOMMERCE)
                    .mode(ProjectMode.ONLINE)
                    .durationRange(DurationRange.ONE_TO_THREE)
                    .location("온라인")
                    .recruitmentDeadline(LocalDate.now().plusDays(30))
                    .recruitments(List.of(
                            ProjectReqDTO.RecruitmentDTO.builder()
                                    .position(ProjectPart.BACKEND)
                                    .count(2)
                                    .build()
                    ))
                    .title("수정")
                    .content("수정 내용")
                    .build();

            // when & then
            assertThatThrownBy(() -> projectCommandService.updateProject(
                    otherPm, savedProject.getId(), request))
                    .isInstanceOf(ProjectException.class);
        }

        @Test
        @DisplayName("삭제된 프로젝트를 수정하면 예외가 발생한다")
        void updateProject_deletedProject() {
            // given
            savedProject.delete();
            projectRepository.saveAndFlush(savedProject);

            ProjectReqDTO.UpdateProjectReq request = ProjectReqDTO.UpdateProjectReq.builder()
                    .projectField(ProjectField.WEB)
                    .category(CategoryGenre.ECOMMERCE)
                    .mode(ProjectMode.ONLINE)
                    .durationRange(DurationRange.ONE_TO_THREE)
                    .location("온라인")
                    .recruitmentDeadline(LocalDate.now().plusDays(30))
                    .recruitments(List.of(
                            ProjectReqDTO.RecruitmentDTO.builder()
                                    .position(ProjectPart.BACKEND)
                                    .count(2)
                                    .build()
                    ))
                    .title("수정")
                    .content("수정 내용")
                    .build();

            // when & then
            assertThatThrownBy(() -> projectCommandService.updateProject(
                    pmMember, savedProject.getId(), request))
                    .isInstanceOf(ProjectException.class);
        }
    }

    @Nested
    @DisplayName("프로젝트 삭제")
    class DeleteProjectTest {

        @Test
        @DisplayName("프로젝트 삭제(소프트 삭제)에 성공한다")
        void deleteProject_success() {
            // given
            Project project = projectRepository.save(Project.builder()
                    .name("삭제할 프로젝트")
                    .content("삭제할 내용")
                    .status(ProjectStatus.RECRUITING)
                    .projectField(ProjectField.WEB)
                    .mode(ProjectMode.ONLINE)
                    .durationRange(DurationRange.ONE_TO_THREE)
                    .location("온라인")
                    .recruitmentDeadline(LocalDate.now().plusDays(30))
                    .category(testCategory)
                    .member(pmMember)
                    .build());

            // when
            projectCommandService.deleteProject(pmMember, project.getId());

            // then
            Project deleted = projectRepository.findById(project.getId()).orElseThrow();
            assertThat(deleted.getStatus()).isEqualTo(ProjectStatus.DELETED);
        }

        @Test
        @DisplayName("소유자가 아닌 회원이 삭제하면 예외가 발생한다")
        void deleteProject_notOwner() {
            // given
            Project project = projectRepository.save(Project.builder()
                    .name("프로젝트")
                    .content("내용")
                    .status(ProjectStatus.RECRUITING)
                    .projectField(ProjectField.WEB)
                    .mode(ProjectMode.ONLINE)
                    .durationRange(DurationRange.ONE_TO_THREE)
                    .location("온라인")
                    .recruitmentDeadline(LocalDate.now().plusDays(30))
                    .category(testCategory)
                    .member(pmMember)
                    .build());

            // when & then
            assertThatThrownBy(() -> projectCommandService.deleteProject(devMember, project.getId()))
                    .isInstanceOf(ProjectException.class);
        }
    }

    @Nested
    @DisplayName("프로젝트 상태 변경")
    class ChangeStatusTest {

        private Project recruitingProject;

        @BeforeEach
        void setUp() {
            recruitingProject = projectRepository.save(Project.builder()
                    .name("모집 중 프로젝트")
                    .content("내용")
                    .status(ProjectStatus.RECRUITING)
                    .projectField(ProjectField.WEB)
                    .mode(ProjectMode.ONLINE)
                    .durationRange(DurationRange.ONE_TO_THREE)
                    .location("온라인")
                    .recruitmentDeadline(LocalDate.now().plusDays(30))
                    .category(testCategory)
                    .member(pmMember)
                    .build());
        }

        @Test
        @DisplayName("모집 중 -> 진행 중으로 상태 변경에 성공한다")
        void changeStatus_recruitingToInProgress() {
            // when
            projectCommandService.changeProjectStatus(
                    pmMember, recruitingProject.getId(), ProjectStatus.IN_PROGRESS);

            // then
            Project updated = projectRepository.findById(recruitingProject.getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(ProjectStatus.IN_PROGRESS);
        }

        @Test
        @DisplayName("진행 중 -> 완료로 상태 변경에 성공한다")
        void changeStatus_inProgressToCompleted() {
            // given
            recruitingProject.startProgress();
            projectRepository.saveAndFlush(recruitingProject);

            // when
            projectCommandService.changeProjectStatus(
                    pmMember, recruitingProject.getId(), ProjectStatus.COMPLETED);

            // then
            Project updated = projectRepository.findById(recruitingProject.getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(ProjectStatus.COMPLETED);
        }

        @Test
        @DisplayName("모집 중에서 완료로 직접 변경에 성공한다")
        void changeStatus_recruitingToCompleted() {
            // when
            projectCommandService.changeProjectStatus(
                    pmMember, recruitingProject.getId(), ProjectStatus.COMPLETED);

            // then
            Project updated = projectRepository.findById(recruitingProject.getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(ProjectStatus.COMPLETED);
        }

        @Test
        @DisplayName("완료 상태에서 진행 중으로 변경에 성공한다")
        void changeStatus_completedToInProgress() {
            // given
            recruitingProject.startProgress();
            recruitingProject.complete();
            projectRepository.saveAndFlush(recruitingProject);

            // when
            projectCommandService.changeProjectStatus(
                    pmMember, recruitingProject.getId(), ProjectStatus.IN_PROGRESS);

            // then
            Project updated = projectRepository.findById(recruitingProject.getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(ProjectStatus.IN_PROGRESS);
        }

        @Test
        @DisplayName("소유자가 아닌 회원이 상태 변경하면 예외가 발생한다")
        void changeStatus_notOwner() {
            // when & then
            assertThatThrownBy(() -> projectCommandService.changeProjectStatus(
                    devMember, recruitingProject.getId(), ProjectStatus.IN_PROGRESS))
                    .isInstanceOf(ProjectException.class);
        }
    }
}
