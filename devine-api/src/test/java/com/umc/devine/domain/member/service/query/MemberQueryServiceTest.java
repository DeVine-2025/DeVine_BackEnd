package com.umc.devine.domain.member.service.query;

import com.umc.devine.domain.category.entity.Category;
import com.umc.devine.domain.category.entity.mapping.MemberCategory;
import com.umc.devine.domain.category.enums.CategoryGenre;
import com.umc.devine.domain.category.repository.CategoryRepository;
import com.umc.devine.domain.category.repository.MemberCategoryRepository;
import com.umc.devine.domain.member.dto.MemberReqDTO;
import com.umc.devine.domain.member.dto.MemberResDTO;
import com.umc.devine.domain.member.entity.Contact;
import com.umc.devine.domain.member.entity.GitRepoUrl;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.entity.Terms;
import com.umc.devine.domain.member.enums.ContactType;
import com.umc.devine.domain.member.enums.MemberMainType;
import com.umc.devine.domain.member.enums.MemberStatus;
import com.umc.devine.domain.member.exception.MemberException;
import com.umc.devine.domain.member.repository.ContactRepository;
import com.umc.devine.domain.member.repository.GitRepoUrlRepository;
import com.umc.devine.domain.member.repository.MemberRepository;
import com.umc.devine.domain.member.repository.TermsRepository;
import com.umc.devine.domain.project.entity.Project;
import com.umc.devine.domain.project.entity.ProjectEmbedding;
import com.umc.devine.domain.project.entity.ProjectRequirementMember;
import com.umc.devine.domain.project.enums.DurationRange;
import com.umc.devine.domain.project.enums.ProjectField;
import com.umc.devine.domain.project.enums.ProjectMode;
import com.umc.devine.domain.project.enums.ProjectPart;
import com.umc.devine.domain.project.enums.ProjectStatus;
import com.umc.devine.domain.project.repository.ProjectEmbeddingRepository;
import com.umc.devine.domain.project.repository.ProjectRepository;
import com.umc.devine.domain.project.repository.ProjectRequirementMemberRepository;
import com.umc.devine.domain.report.entity.DevReport;
import com.umc.devine.domain.report.entity.ReportEmbedding;
import com.umc.devine.domain.report.enums.ReportType;
import com.umc.devine.domain.report.repository.DevReportRepository;
import com.umc.devine.domain.report.repository.ReportEmbeddingRepository;
import com.umc.devine.domain.techstack.entity.mapping.ProjectRequirementTechstack;
import com.umc.devine.domain.techstack.repository.ProjectRequirementTechstackRepository;
import com.umc.devine.domain.techstack.dto.TechstackResDTO;
import com.umc.devine.domain.techstack.entity.Techstack;
import com.umc.devine.domain.techstack.entity.mapping.DevTechstack;
import com.umc.devine.domain.techstack.enums.TechName;
import com.umc.devine.domain.techstack.enums.TechstackSource;
import com.umc.devine.domain.techstack.repository.DevTechstackRepository;
import com.umc.devine.domain.techstack.repository.TechstackRepository;
import com.umc.devine.global.dto.PagedResponse;
import com.umc.devine.global.enums.EmbeddingStatus;
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

class MemberQueryServiceTest extends IntegrationTestSupport {

    @Autowired
    private MemberQueryService memberQueryService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private TermsRepository termsRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private MemberCategoryRepository memberCategoryRepository;

    @Autowired
    private ContactRepository contactRepository;

    @Autowired
    private TechstackRepository techstackRepository;

    @Autowired
    private DevTechstackRepository devTechstackRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectRequirementMemberRepository projectRequirementMemberRepository;

    @Autowired
    private ProjectRequirementTechstackRepository projectRequirementTechstackRepository;

    @Autowired
    private GitRepoUrlRepository gitRepoUrlRepository;

    @Autowired
    private DevReportRepository devReportRepository;

    @Autowired
    private ReportEmbeddingRepository reportEmbeddingRepository;

    @Autowired
    private ProjectEmbeddingRepository projectEmbeddingRepository;

    private Member testMember;
    private Category testCategory;
    private Techstack testTechstack;

    @BeforeEach
    void setUp() {
        testCategory = categoryRepository.findByGenre(CategoryGenre.HEALTHCARE).orElseThrow();

        testTechstack = techstackRepository.findByName(TechName.JAVA).orElseThrow();

        testMember = memberRepository.save(Member.builder()
                .clerkId("clerk_test_123")
                .name("테스트")
                .nickname("testuser")
                .mainType(MemberMainType.DEVELOPER)
                .disclosure(true)
                .used(MemberStatus.ACTIVE)
                .build());
    }

    @Nested
    @DisplayName("이용약관 조회")
    class FindAllTermsTest {

        @Test
        @DisplayName("모든 이용약관을 조회한다")
        void findAllTerms_success() {
            // given - V3 시드 데이터로 이미 약관이 존재함

            // when
            MemberResDTO.TermsListDTO result = memberQueryService.findAllTerms();

            // then
            assertThat(result.terms()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("내 프로필 조회")
    class FindMemberProfileTest {

        @Test
        @DisplayName("내 프로필을 조회한다")
        void findMemberProfile_success() {
            // when
            MemberResDTO.MemberProfileDTO result = memberQueryService.findMemberProfile(testMember);

            // then
            assertThat(result.member().nickname()).isEqualTo("testuser");
            assertThat(result.member().mainType()).isEqualTo(MemberMainType.DEVELOPER);
        }

        @Test
        @DisplayName("카테고리와 연락처 정보가 포함된 프로필을 조회한다")
        void findMemberProfile_withCategoriesAndContacts() {
            // given
            MemberCategory memberCategory = MemberCategory.builder()
                    .member(testMember)
                    .category(testCategory)
                    .build();
            memberCategoryRepository.save(memberCategory);

            Contact contact = Contact.builder()
                    .member(testMember)
                    .contactType(ContactType.EMAIL)
                    .value("test@example.com")
                    .build();
            contactRepository.save(contact);

            // when
            MemberResDTO.MemberProfileDTO result = memberQueryService.findMemberProfile(testMember);

            // then
            assertThat(result.domains()).hasSize(1);
            assertThat(result.contacts()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("닉네임으로 회원 조회")
    class FindMemberByNicknameTest {

        @Test
        @DisplayName("닉네임으로 공개 프로필 회원을 조회한다")
        void findMemberByNickname_success() {
            // when
            MemberResDTO.MemberProfileDTO result = memberQueryService.findMemberByNickname("testuser");

            // then
            assertThat(result.member().nickname()).isEqualTo("testuser");
        }

        @Test
        @DisplayName("존재하지 않는 닉네임 조회 시 예외 발생")
        void findMemberByNickname_notFound() {
            // when & then
            assertThatThrownBy(() -> memberQueryService.findMemberByNickname("nonexistent"))
                    .isInstanceOf(MemberException.class);
        }

        @Test
        @DisplayName("비공개 프로필 회원 조회 시 예외 발생")
        void findMemberByNickname_notPublic() {
            // given
            memberRepository.save(Member.builder()
                    .clerkId("clerk_private_123")
                    .name("비공개")
                    .nickname("privateuser")
                    .mainType(MemberMainType.DEVELOPER)
                    .disclosure(false)
                    .used(MemberStatus.ACTIVE)
                    .build());

            // when & then
            assertThatThrownBy(() -> memberQueryService.findMemberByNickname("privateuser"))
                    .isInstanceOf(MemberException.class);
        }
    }

    @Nested
    @DisplayName("닉네임 중복 체크")
    class CheckNicknameDuplicateTest {

        @Test
        @DisplayName("중복 닉네임 확인")
        void checkNicknameDuplicate_duplicate() {
            // when
            MemberResDTO.NicknameDuplicateDTO result = memberQueryService.checkNicknameDuplicate("testuser");

            // then
            assertThat(result.isDuplicate()).isTrue();
            assertThat(result.nickname()).isEqualTo("testuser");
        }

        @Test
        @DisplayName("사용 가능한 닉네임 확인")
        void checkNicknameDuplicate_notDuplicate() {
            // when
            MemberResDTO.NicknameDuplicateDTO result = memberQueryService.checkNicknameDuplicate("newuser");

            // then
            assertThat(result.isDuplicate()).isFalse();
        }
    }

    @Nested
    @DisplayName("개발자 추천 목록 조회")
    class FindRecommendedDevelopersTest {

        @Test
        @DisplayName("임베딩이 없으면 빈 배열 반환")
        void findRecommendedDevelopers_noEmbedding_returnsEmpty() {
            // given - 임베딩 없이 프로젝트만 생성
            MemberCategory memberCategory = MemberCategory.builder()
                    .member(testMember)
                    .category(testCategory)
                    .build();
            memberCategoryRepository.save(memberCategory);

            DevTechstack devTechstack = DevTechstack.builder()
                    .member(testMember)
                    .techstack(testTechstack)
                    .source(TechstackSource.MANUAL)
                    .build();
            devTechstackRepository.save(devTechstack);

            Project project = projectRepository.save(Project.builder()
                    .name("테스트 프로젝트")
                    .content("내용")
                    .status(ProjectStatus.RECRUITING)
                    .projectField(ProjectField.WEB)
                    .mode(ProjectMode.ONLINE)
                    .durationRange(DurationRange.ONE_TO_THREE)
                    .location("서울")
                    .recruitmentDeadline(LocalDate.now().plusDays(7))
                    .category(testCategory)
                    .member(testMember)
                    .build());

            MemberReqDTO.RecommendDeveloperDTO dto = MemberReqDTO.RecommendDeveloperDTO.builder()
                    .projectId(project.getId())
                    .page(1)
                    .size(10)
                    .build();

            // when
            PagedResponse<MemberResDTO.RecommendedDeveloperDTO> result = memberQueryService.findRecommendedDevelopers(testMember, dto);

            // then - 임베딩이 없으면 빈 배열 반환
            assertThat(result.getContent()).isEmpty();
        }

        @Test
        @DisplayName("projectId가 없으면 빈 PagedResponse 반환")
        void findRecommendedDevelopers_emptyWhenNoProjectId() {
            // given
            MemberReqDTO.RecommendDeveloperDTO dto = MemberReqDTO.RecommendDeveloperDTO.builder()
                    .projectId(null)
                    .page(1)
                    .size(10)
                    .build();

            // when
            PagedResponse<MemberResDTO.RecommendedDeveloperDTO> result = memberQueryService.findRecommendedDevelopers(testMember, dto);

            // then
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }

        @Test
        @DisplayName("여러 개발자 조회 시 임베딩이 없으면 빈 배열 반환")
        void findRecommendedDevelopers_multipleDevs_noEmbedding_returnsEmpty() {
            // given
            Member dev1 = memberRepository.save(Member.builder()
                    .clerkId("clerk_dev1")
                    .name("개발자1")
                    .nickname("dev1")
                    .mainType(MemberMainType.DEVELOPER)
                    .disclosure(true)
                    .used(MemberStatus.ACTIVE)
                    .build());

            Member dev2 = memberRepository.save(Member.builder()
                    .clerkId("clerk_dev2")
                    .name("개발자2")
                    .nickname("dev2")
                    .mainType(MemberMainType.DEVELOPER)
                    .disclosure(true)
                    .used(MemberStatus.ACTIVE)
                    .build());

            // 각 개발자에게 카테고리, 기술스택 할당
            memberCategoryRepository.save(MemberCategory.builder().member(testMember).category(testCategory).build());
            memberCategoryRepository.save(MemberCategory.builder().member(dev1).category(testCategory).build());
            memberCategoryRepository.save(MemberCategory.builder().member(dev2).category(testCategory).build());

            devTechstackRepository.save(DevTechstack.builder().member(testMember).techstack(testTechstack).source(TechstackSource.MANUAL).build());
            devTechstackRepository.save(DevTechstack.builder().member(dev1).techstack(testTechstack).source(TechstackSource.MANUAL).build());
            devTechstackRepository.save(DevTechstack.builder().member(dev2).techstack(testTechstack).source(TechstackSource.MANUAL).build());

            Project project = projectRepository.save(Project.builder()
                    .name("테스트 프로젝트")
                    .content("내용")
                    .status(ProjectStatus.RECRUITING)
                    .projectField(ProjectField.WEB)
                    .mode(ProjectMode.ONLINE)
                    .durationRange(DurationRange.ONE_TO_THREE)
                    .location("서울")
                    .recruitmentDeadline(LocalDate.now().plusDays(7))
                    .category(testCategory)
                    .member(testMember)
                    .build());

            MemberReqDTO.RecommendDeveloperDTO dto = MemberReqDTO.RecommendDeveloperDTO.builder()
                    .projectId(project.getId())
                    .page(1)
                    .size(10)
                    .build();

            // when
            PagedResponse<MemberResDTO.RecommendedDeveloperDTO> result = memberQueryService.findRecommendedDevelopers(testMember, dto);

            // then - 임베딩이 없으면 빈 배열 반환
            assertThat(result.getContent()).isEmpty();
        }

        @Test
        @DisplayName("임베딩 없이 카테고리/기술스택 없는 개발자 - 빈 배열 반환")
        void findRecommendedDevelopers_noCategoryOrTechstack_noEmbedding_returnsEmpty() {
            // given - testMember는 카테고리/기술스택 없음
            // 다른 개발자만 카테고리 있음
            Member devWithCategory = memberRepository.save(Member.builder()
                    .clerkId("clerk_with_cat")
                    .name("카테고리있는개발자")
                    .nickname("devWithCat")
                    .mainType(MemberMainType.DEVELOPER)
                    .disclosure(true)
                    .used(MemberStatus.ACTIVE)
                    .build());

            memberCategoryRepository.save(MemberCategory.builder().member(devWithCategory).category(testCategory).build());

            Project project = projectRepository.save(Project.builder()
                    .name("테스트 프로젝트")
                    .content("내용")
                    .status(ProjectStatus.RECRUITING)
                    .projectField(ProjectField.WEB)
                    .mode(ProjectMode.ONLINE)
                    .durationRange(DurationRange.ONE_TO_THREE)
                    .location("서울")
                    .recruitmentDeadline(LocalDate.now().plusDays(7))
                    .category(testCategory)
                    .member(testMember)
                    .build());

            MemberReqDTO.RecommendDeveloperDTO dto = MemberReqDTO.RecommendDeveloperDTO.builder()
                    .projectId(project.getId())
                    .page(1)
                    .size(10)
                    .build();

            // when
            PagedResponse<MemberResDTO.RecommendedDeveloperDTO> result = memberQueryService.findRecommendedDevelopers(testMember, dto);

            // then - 임베딩 없이는 빈 배열 반환 (NPE도 없음)
            assertThat(result.getContent()).isEmpty();
        }

        @Test
        @DisplayName("프로젝트 요구 기술스택 없고 임베딩도 없으면 빈 배열 반환")
        void findRecommendedDevelopers_noProjectTechstack_noEmbedding_returnsEmpty() {
            // given
            memberCategoryRepository.save(MemberCategory.builder().member(testMember).category(testCategory).build());
            devTechstackRepository.save(DevTechstack.builder().member(testMember).techstack(testTechstack).source(TechstackSource.MANUAL).build());

            // 프로젝트에 요구 기술스택 없음 (임베딩도 없음)
            Project project = projectRepository.save(Project.builder()
                    .name("기술스택 없는 프로젝트")
                    .content("내용")
                    .status(ProjectStatus.RECRUITING)
                    .projectField(ProjectField.WEB)
                    .mode(ProjectMode.ONLINE)
                    .durationRange(DurationRange.ONE_TO_THREE)
                    .location("서울")
                    .recruitmentDeadline(LocalDate.now().plusDays(7))
                    .category(testCategory)
                    .member(testMember)
                    .build());

            MemberReqDTO.RecommendDeveloperDTO dto = MemberReqDTO.RecommendDeveloperDTO.builder()
                    .projectId(project.getId())
                    .page(1)
                    .size(10)
                    .build();

            // when
            PagedResponse<MemberResDTO.RecommendedDeveloperDTO> result = memberQueryService.findRecommendedDevelopers(testMember, dto);

            // then - 임베딩이 없으면 빈 배열 반환
            assertThat(result.getContent()).isEmpty();
        }

        @Test
        @DisplayName("domainMatch 정상 계산 - 임베딩 없으면 빈 배열 반환")
        void findRecommendedDevelopers_domainMatch_noEmbedding_returnsEmpty() {
            // given
            memberCategoryRepository.save(MemberCategory.builder().member(testMember).category(testCategory).build());

            Project project = projectRepository.save(Project.builder()
                    .name("테스트 프로젝트")
                    .content("내용")
                    .status(ProjectStatus.RECRUITING)
                    .projectField(ProjectField.WEB)
                    .mode(ProjectMode.ONLINE)
                    .durationRange(DurationRange.ONE_TO_THREE)
                    .location("서울")
                    .recruitmentDeadline(LocalDate.now().plusDays(7))
                    .category(testCategory)  // HEALTHCARE
                    .member(testMember)
                    .build());

            MemberReqDTO.RecommendDeveloperDTO dto = MemberReqDTO.RecommendDeveloperDTO.builder()
                    .projectId(project.getId())
                    .page(1)
                    .size(10)
                    .build();

            // when
            PagedResponse<MemberResDTO.RecommendedDeveloperDTO> result = memberQueryService.findRecommendedDevelopers(testMember, dto);

            // then - 임베딩이 없으면 빈 배열 반환 (domainMatch는 벡터 검색에서만 계산됨)
            assertThat(result.getContent()).isEmpty();
        }

        @Test
        @DisplayName("domainMatch 정상 계산 - 일치하지 않는 경우")
        void findRecommendedDevelopers_domainMatch_false() {
            // given
            Category otherCategory = categoryRepository.findByGenre(CategoryGenre.EDUCATION).orElseThrow();
            memberCategoryRepository.save(MemberCategory.builder().member(testMember).category(otherCategory).build());

            Project project = projectRepository.save(Project.builder()
                    .name("테스트 프로젝트")
                    .content("내용")
                    .status(ProjectStatus.RECRUITING)
                    .projectField(ProjectField.WEB)
                    .mode(ProjectMode.ONLINE)
                    .durationRange(DurationRange.ONE_TO_THREE)
                    .location("서울")
                    .recruitmentDeadline(LocalDate.now().plusDays(7))
                    .category(testCategory)  // HEALTHCARE - 개발자는 EDUCATION
                    .member(testMember)
                    .build());

            MemberReqDTO.RecommendDeveloperDTO dto = MemberReqDTO.RecommendDeveloperDTO.builder()
                    .projectId(project.getId())
                    .page(1)
                    .size(10)
                    .build();

            // when
            PagedResponse<MemberResDTO.RecommendedDeveloperDTO> result = memberQueryService.findRecommendedDevelopers(testMember, dto);

            // then - 카테고리 필터로 조회되지 않으므로 빈 결과
            assertThat(result.getContent()).isEmpty();
        }

        @Test
        @DisplayName("matchedTechstacks 정상 계산 - 임베딩 없으면 빈 배열 반환")
        void findRecommendedDevelopers_matchedTechstacks_noEmbedding_returnsEmpty() {
            // given
            Techstack springTechstack = techstackRepository.findByName(TechName.SPRINGBOOT).orElseThrow();

            memberCategoryRepository.save(MemberCategory.builder().member(testMember).category(testCategory).build());
            devTechstackRepository.save(DevTechstack.builder().member(testMember).techstack(testTechstack).source(TechstackSource.MANUAL).build());
            devTechstackRepository.save(DevTechstack.builder().member(testMember).techstack(springTechstack).source(TechstackSource.MANUAL).build());

            Project project = projectRepository.save(Project.builder()
                    .name("테스트 프로젝트")
                    .content("내용")
                    .status(ProjectStatus.RECRUITING)
                    .projectField(ProjectField.WEB)
                    .mode(ProjectMode.ONLINE)
                    .durationRange(DurationRange.ONE_TO_THREE)
                    .location("서울")
                    .recruitmentDeadline(LocalDate.now().plusDays(7))
                    .category(testCategory)
                    .member(testMember)
                    .build());

            // 프로젝트 요구사항에 기술스택 추가 (임베딩은 없음)
            ProjectRequirementMember requirement = projectRequirementMemberRepository.save(
                    ProjectRequirementMember.builder()
                            .project(project)
                            .part(ProjectPart.BACKEND)
                            .requirementNum(2)
                            .build());

            projectRequirementTechstackRepository.save(
                    ProjectRequirementTechstack.builder()
                            .requirement(requirement)
                            .techstack(testTechstack)  // JAVA
                            .build());

            projectRequirementTechstackRepository.save(
                    ProjectRequirementTechstack.builder()
                            .requirement(requirement)
                            .techstack(springTechstack)  // SPRING
                            .build());

            MemberReqDTO.RecommendDeveloperDTO dto = MemberReqDTO.RecommendDeveloperDTO.builder()
                    .projectId(project.getId())
                    .page(1)
                    .size(10)
                    .build();

            // when
            PagedResponse<MemberResDTO.RecommendedDeveloperDTO> result = memberQueryService.findRecommendedDevelopers(testMember, dto);

            // then - 임베딩이 없으면 빈 배열 반환 (matchedTechstacks는 벡터 검색에서만 계산됨)
            assertThat(result.getContent()).isEmpty();
        }

        @Test
        @DisplayName("matchedTechstacks 정상 계산 - 임베딩 없으면 빈 배열 반환 (일부 일치 시나리오)")
        void findRecommendedDevelopers_matchedTechstacks_partial_noEmbedding_returnsEmpty() {
            // given
            Techstack springTechstack = techstackRepository.findByName(TechName.SPRINGBOOT).orElseThrow();

            Techstack kotlinTechstack = techstackRepository.findByNameAndParentStackName(TechName.KOTLIN, TechName.BACKEND).orElseThrow();

            memberCategoryRepository.save(MemberCategory.builder().member(testMember).category(testCategory).build());
            // 개발자는 JAVA만 보유
            devTechstackRepository.save(DevTechstack.builder().member(testMember).techstack(testTechstack).source(TechstackSource.MANUAL).build());

            Project project = projectRepository.save(Project.builder()
                    .name("테스트 프로젝트")
                    .content("내용")
                    .status(ProjectStatus.RECRUITING)
                    .projectField(ProjectField.WEB)
                    .mode(ProjectMode.ONLINE)
                    .durationRange(DurationRange.ONE_TO_THREE)
                    .location("서울")
                    .recruitmentDeadline(LocalDate.now().plusDays(7))
                    .category(testCategory)
                    .member(testMember)
                    .build());

            ProjectRequirementMember requirement = projectRequirementMemberRepository.save(
                    ProjectRequirementMember.builder()
                            .project(project)
                            .part(ProjectPart.BACKEND)
                            .requirementNum(2)
                            .build());

            // 프로젝트는 JAVA, SPRING, KOTLIN 요구 (임베딩은 없음)
            projectRequirementTechstackRepository.save(
                    ProjectRequirementTechstack.builder()
                            .requirement(requirement)
                            .techstack(testTechstack)  // JAVA - 개발자 보유
                            .build());

            projectRequirementTechstackRepository.save(
                    ProjectRequirementTechstack.builder()
                            .requirement(requirement)
                            .techstack(springTechstack)  // SPRING - 개발자 미보유
                            .build());

            projectRequirementTechstackRepository.save(
                    ProjectRequirementTechstack.builder()
                            .requirement(requirement)
                            .techstack(kotlinTechstack)  // KOTLIN - 개발자 미보유
                            .build());

            MemberReqDTO.RecommendDeveloperDTO dto = MemberReqDTO.RecommendDeveloperDTO.builder()
                    .projectId(project.getId())
                    .page(1)
                    .size(10)
                    .build();

            // when
            PagedResponse<MemberResDTO.RecommendedDeveloperDTO> result = memberQueryService.findRecommendedDevelopers(testMember, dto);

            // then - 임베딩이 없으면 빈 배열 반환
            assertThat(result.getContent()).isEmpty();
        }
    }

    @Nested
    @DisplayName("개발자 추천 프리뷰 조회")
    class FindRecommendedDevelopersPreviewTest {

        @Test
        @DisplayName("프리뷰 목록 조회 - 임베딩 없으면 빈 배열 반환")
        void findRecommendedDevelopersPreview_noEmbedding_returnsEmpty() {
            // given
            MemberCategory memberCategory = MemberCategory.builder()
                    .member(testMember)
                    .category(testCategory)
                    .build();
            memberCategoryRepository.save(memberCategory);

            DevTechstack devTechstack = DevTechstack.builder()
                    .member(testMember)
                    .techstack(testTechstack)
                    .source(TechstackSource.MANUAL)
                    .build();
            devTechstackRepository.save(devTechstack);

            Project project = projectRepository.save(Project.builder()
                    .name("테스트 프로젝트")
                    .content("내용")
                    .status(ProjectStatus.RECRUITING)
                    .projectField(ProjectField.WEB)
                    .mode(ProjectMode.ONLINE)
                    .durationRange(DurationRange.ONE_TO_THREE)
                    .location("서울")
                    .recruitmentDeadline(LocalDate.now().plusDays(7))
                    .category(testCategory)
                    .member(testMember)
                    .build());

            // when
            List<MemberResDTO.RecommendedDeveloperDTO> result = memberQueryService.findRecommendedDevelopersPreview(testMember, project.getId(), 4);

            // then - 임베딩이 없으면 빈 배열 반환
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("projectId가 없으면 빈 배열 반환")
        void findRecommendedDevelopersPreview_emptyWhenNoProjectId() {
            // when
            List<MemberResDTO.RecommendedDeveloperDTO> result = memberQueryService.findRecommendedDevelopersPreview(testMember, null, 4);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("개발자 검색")
    class SearchDevelopersTest {

        @Test
        @DisplayName("필터 조건에 따른 검색 성공")
        void searchDevelopers_success() {
            // given
            MemberCategory memberCategory = MemberCategory.builder()
                    .member(testMember)
                    .category(testCategory)
                    .build();
            memberCategoryRepository.save(memberCategory);

            DevTechstack devTechstack = DevTechstack.builder()
                    .member(testMember)
                    .techstack(testTechstack)
                    .source(TechstackSource.MANUAL)
                    .build();
            devTechstackRepository.save(devTechstack);

            MemberReqDTO.SearchDeveloperDTO dto = MemberReqDTO.SearchDeveloperDTO.builder()
                    .categories(List.of(CategoryGenre.HEALTHCARE))
                    .page(1)
                    .size(10)
                    .build();

            // when
            PagedResponse<MemberResDTO.MemberListItemDTO> result = memberQueryService.searchDevelopers(dto);

            // then
            assertThat(result.getContent()).isNotEmpty();
        }

        @Test
        @DisplayName("여러 개발자 검색 시 N+1 없이 정상 조회")
        void searchDevelopers_multipleDevs_noN1() {
            // given
            Member dev1 = memberRepository.save(Member.builder()
                    .clerkId("clerk_search_dev1")
                    .name("검색개발자1")
                    .nickname("searchDev1")
                    .mainType(MemberMainType.DEVELOPER)
                    .disclosure(true)
                    .used(MemberStatus.ACTIVE)
                    .build());

            Member dev2 = memberRepository.save(Member.builder()
                    .clerkId("clerk_search_dev2")
                    .name("검색개발자2")
                    .nickname("searchDev2")
                    .mainType(MemberMainType.DEVELOPER)
                    .disclosure(true)
                    .used(MemberStatus.ACTIVE)
                    .build());

            // 각 개발자에게 여러 카테고리, 기술스택 할당
            memberCategoryRepository.save(MemberCategory.builder().member(testMember).category(testCategory).build());
            memberCategoryRepository.save(MemberCategory.builder().member(dev1).category(testCategory).build());
            memberCategoryRepository.save(MemberCategory.builder().member(dev2).category(testCategory).build());

            Techstack springTechstack = techstackRepository.findByName(TechName.SPRINGBOOT).orElseThrow();

            devTechstackRepository.save(DevTechstack.builder().member(testMember).techstack(testTechstack).source(TechstackSource.MANUAL).build());
            devTechstackRepository.save(DevTechstack.builder().member(testMember).techstack(springTechstack).source(TechstackSource.MANUAL).build());
            devTechstackRepository.save(DevTechstack.builder().member(dev1).techstack(testTechstack).source(TechstackSource.MANUAL).build());
            devTechstackRepository.save(DevTechstack.builder().member(dev2).techstack(springTechstack).source(TechstackSource.MANUAL).build());

            MemberReqDTO.SearchDeveloperDTO dto = MemberReqDTO.SearchDeveloperDTO.builder()
                    .categories(List.of(CategoryGenre.HEALTHCARE))
                    .page(1)
                    .size(10)
                    .build();

            // when
            PagedResponse<MemberResDTO.MemberListItemDTO> result = memberQueryService.searchDevelopers(dto);

            // then
            assertThat(result.getContent()).hasSize(3);
            assertThat(result.getContent()).allSatisfy(dev -> {
                assertThat(dev.domains()).isNotNull();
                assertThat(dev.techstacks()).isNotNull();
            });
        }

        @Test
        @DisplayName("카테고리/기술스택 없는 개발자도 NPE 없이 정상 조회")
        void searchDevelopers_noCategoryOrTechstack_noNPE() {
            // given
            Member devWithCategory = memberRepository.save(Member.builder()
                    .clerkId("clerk_search_cat")
                    .name("카테고리있는개발자")
                    .nickname("searchDevWithCat")
                    .mainType(MemberMainType.DEVELOPER)
                    .disclosure(true)
                    .used(MemberStatus.ACTIVE)
                    .build());

            Member devWithoutAnything = memberRepository.save(Member.builder()
                    .clerkId("clerk_search_empty")
                    .name("아무것도없는개발자")
                    .nickname("searchDevEmpty")
                    .mainType(MemberMainType.DEVELOPER)
                    .disclosure(true)
                    .used(MemberStatus.ACTIVE)
                    .build());

            memberCategoryRepository.save(MemberCategory.builder().member(devWithCategory).category(testCategory).build());

            MemberReqDTO.SearchDeveloperDTO dto = MemberReqDTO.SearchDeveloperDTO.builder()
                    .page(1)
                    .size(10)
                    .build();

            // when
            PagedResponse<MemberResDTO.MemberListItemDTO> result = memberQueryService.searchDevelopers(dto);

            // then - NPE 발생하지 않고 정상 조회
            assertThat(result.getContent()).isNotEmpty();

            MemberResDTO.MemberListItemDTO emptyDev = result.getContent().stream()
                    .filter(d -> d.member().nickname().equals("searchDevEmpty"))
                    .findFirst()
                    .orElseThrow();
            assertThat(emptyDev.domains()).isEmpty();
            assertThat(emptyDev.techstacks()).isEmpty();
        }

        @Test
        @DisplayName("필터 없이 전체 개발자 조회")
        void searchDevelopers_noFilter_success() {
            // given
            memberCategoryRepository.save(MemberCategory.builder().member(testMember).category(testCategory).build());

            MemberReqDTO.SearchDeveloperDTO dto = MemberReqDTO.SearchDeveloperDTO.builder()
                    .page(1)
                    .size(10)
                    .build();

            // when
            PagedResponse<MemberResDTO.MemberListItemDTO> result = memberQueryService.searchDevelopers(dto);

            // then
            assertThat(result.getContent()).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("내 기술 스택 조회")
    class FindMemberTechstacksTest {

        @Test
        @DisplayName("회원의 기술 스택 목록을 조회한다")
        void findMemberTechstacks_success() {
            // given
            DevTechstack devTechstack = DevTechstack.builder()
                    .member(testMember)
                    .techstack(testTechstack)
                    .source(TechstackSource.MANUAL)
                    .build();
            devTechstackRepository.save(devTechstack);

            // when
            TechstackResDTO.DevTechstackListDTO result = memberQueryService.findMemberTechstacks(testMember);

            // then
            assertThat(result.techstacks()).hasSize(1);
        }

        @Test
        @DisplayName("기술 스택이 없으면 빈 리스트를 반환한다")
        void findMemberTechstacks_empty() {
            // when
            TechstackResDTO.DevTechstackListDTO result = memberQueryService.findMemberTechstacks(testMember);

            // then
            assertThat(result.techstacks()).isEmpty();
        }
    }

    @Nested
    @DisplayName("닉네임으로 기술 스택 조회")
    class FindTechstacksByNicknameTest {

        @Test
        @DisplayName("닉네임으로 공개 프로필 회원의 기술 스택을 조회한다")
        void findTechstacksByNickname_success() {
            // given
            DevTechstack devTechstack = DevTechstack.builder()
                    .member(testMember)
                    .techstack(testTechstack)
                    .source(TechstackSource.MANUAL)
                    .build();
            devTechstackRepository.save(devTechstack);

            // when
            TechstackResDTO.DevTechstackListDTO result = memberQueryService.findTechstacksByNickname("testuser");

            // then
            assertThat(result.techstacks()).hasSize(1);
        }

        @Test
        @DisplayName("존재하지 않는 닉네임 조회 시 예외 발생")
        void findTechstacksByNickname_notFound() {
            // when & then
            assertThatThrownBy(() -> memberQueryService.findTechstacksByNickname("nonexistent"))
                    .isInstanceOf(MemberException.class);
        }

        @Test
        @DisplayName("비공개 프로필 회원의 기술 스택 조회 시 예외 발생")
        void findTechstacksByNickname_notPublic() {
            // given
            memberRepository.save(Member.builder()
                    .clerkId("clerk_private_456")
                    .name("비공개")
                    .nickname("privateuser2")
                    .mainType(MemberMainType.DEVELOPER)
                    .disclosure(false)
                    .used(MemberStatus.ACTIVE)
                    .build());

            // when & then
            assertThatThrownBy(() -> memberQueryService.findTechstacksByNickname("privateuser2"))
                    .isInstanceOf(MemberException.class);
        }
    }

    @Nested
    @DisplayName("벡터 검색 기반 개발자 추천")
    class VectorSearchRecommendationTest {

        private Techstack backendTechstack;
        private Techstack javaTechstack;
        private Techstack springTechstack;

        @BeforeEach
        void setUpVectorSearchData() {
            // 루트 포지션 (BACKEND)
            backendTechstack = techstackRepository.findByName(TechName.BACKEND).orElseThrow();

            // BACKEND 하위 기술스택
            javaTechstack = techstackRepository.findByName(TechName.JAVA).orElseThrow();

            springTechstack = techstackRepository.findByName(TechName.SPRINGBOOT).orElseThrow();
        }

        @Test
        @DisplayName("벡터 검색 시 점수가 정상적으로 계산된다")
        void vectorSearch_scoresCalculatedCorrectly() {
            // given - 개발자 설정
            Member developer = memberRepository.save(Member.builder()
                    .clerkId("clerk_vector_dev")
                    .name("벡터개발자")
                    .nickname("vectorDev")
                    .mainType(MemberMainType.DEVELOPER)
                    .disclosure(true)
                    .used(MemberStatus.ACTIVE)
                    .build());

            // 개발자 카테고리 (도메인 일치용)
            memberCategoryRepository.save(MemberCategory.builder()
                    .member(developer)
                    .category(testCategory)
                    .build());

            // 개발자 기술스택 (BACKEND 포지션 + JAVA, SPRINGBOOT)
            devTechstackRepository.save(DevTechstack.builder()
                    .member(developer)
                    .techstack(backendTechstack)
                    .source(TechstackSource.MANUAL)
                    .build());
            devTechstackRepository.save(DevTechstack.builder()
                    .member(developer)
                    .techstack(javaTechstack)
                    .source(TechstackSource.MANUAL)
                    .build());
            devTechstackRepository.save(DevTechstack.builder()
                    .member(developer)
                    .techstack(springTechstack)
                    .source(TechstackSource.MANUAL)
                    .build());

            // 개발자 리포트 및 임베딩
            GitRepoUrl gitRepoUrl = gitRepoUrlRepository.save(GitRepoUrl.builder()
                    .member(developer)
                    .gitUrl("https://github.com/vectordev/repo")
                    .gitDescription("Test repo")
                    .build());

            DevReport devReport = devReportRepository.save(DevReport.builder()
                    .gitRepoUrl(gitRepoUrl)
                    .content("{\"summary\": \"test\"}")
                    .reportType(ReportType.MAIN)
                    .build());

            // 임베딩 벡터 생성 (1536 차원, 유사도 테스트용)
            float[] devEmbedding = new float[1536];
            for (int i = 0; i < 1536; i++) {
                devEmbedding[i] = 0.5f;
            }
            ReportEmbedding reportEmbedding = reportEmbeddingRepository.save(ReportEmbedding.builder()
                    .devReport(devReport)
                    .embedding(devEmbedding)
                    .status(EmbeddingStatus.SUCCESS)
                    .build());

            // 프로젝트 설정
            Project project = projectRepository.save(Project.builder()
                    .name("벡터테스트 프로젝트")
                    .content("프로젝트 내용")
                    .status(ProjectStatus.RECRUITING)
                    .projectField(ProjectField.WEB)
                    .mode(ProjectMode.ONLINE)
                    .durationRange(DurationRange.ONE_TO_THREE)
                    .location("서울")
                    .recruitmentDeadline(LocalDate.now().plusDays(30))
                    .category(testCategory)  // 도메인 일치
                    .member(testMember)
                    .build());

            // 프로젝트 요구 포지션 (BACKEND, 모집 중)
            ProjectRequirementMember prm = projectRequirementMemberRepository.save(
                    ProjectRequirementMember.builder()
                            .project(project)
                            .part(ProjectPart.BACKEND)
                            .requirementNum(2)
                            .currentCount(0)
                            .build());

            // 프로젝트 요구 기술스택 (JAVA, SPRINGBOOT)
            projectRequirementTechstackRepository.save(ProjectRequirementTechstack.builder()
                    .requirement(prm)
                    .techstack(javaTechstack)
                    .build());
            projectRequirementTechstackRepository.save(ProjectRequirementTechstack.builder()
                    .requirement(prm)
                    .techstack(springTechstack)
                    .build());

            // 프로젝트 임베딩 (개발자와 유사한 벡터)
            float[] projectEmbedding = new float[1536];
            for (int i = 0; i < 1536; i++) {
                projectEmbedding[i] = 0.5f;  // 동일한 벡터 = 코사인 유사도 1.0
            }
            projectEmbeddingRepository.save(ProjectEmbedding.builder()
                    .project(project)
                    .embedding(projectEmbedding)
                    .status(EmbeddingStatus.SUCCESS)
                    .build());

            MemberReqDTO.RecommendDeveloperDTO dto = MemberReqDTO.RecommendDeveloperDTO.builder()
                    .projectId(project.getId())
                    .page(1)
                    .size(10)
                    .build();

            // when
            PagedResponse<MemberResDTO.RecommendedDeveloperDTO> result =
                    memberQueryService.findRecommendedDevelopers(testMember, dto);

            // then
            assertThat(result.getContent()).isNotEmpty();

            MemberResDTO.RecommendedDeveloperDTO recommendedDev = result.getContent().stream()
                    .filter(d -> d.member().nickname().equals("vectorDev"))
                    .findFirst()
                    .orElse(null);

            assertThat(recommendedDev).isNotNull();

            // 점수 필드 검증
            assertThat(recommendedDev.totalScore()).isNotNull();
            assertThat(recommendedDev.similarityScorePercent()).isNotNull();
            assertThat(recommendedDev.techstackScorePercent()).isNotNull();
            assertThat(recommendedDev.domainMatch()).isNotNull();
            assertThat(recommendedDev.matchedTechstacks()).isNotNull();

            // 점수 범위 검증 (0~100)
            assertThat(recommendedDev.totalScore()).isBetween(0.0, 100.0);
            assertThat(recommendedDev.similarityScorePercent()).isBetween(0.0, 100.0);
            assertThat(recommendedDev.techstackScorePercent()).isBetween(0.0, 100.0);

            // 동일한 벡터이므로 유사도는 100에 가까워야 함
            assertThat(recommendedDev.similarityScorePercent()).isGreaterThan(90.0);

            // 도메인 일치해야 함
            assertThat(recommendedDev.domainMatch()).isTrue();

            // 기술스택 매칭 (JAVA, SPRINGBOOT)
            assertThat(recommendedDev.matchedTechstacks()).containsAnyOf("JAVA", "SPRINGBOOT");

            // 기술스택 100% 매칭이므로 점수도 높아야 함
            assertThat(recommendedDev.techstackScorePercent()).isEqualTo(100.0);

            System.out.println("=== 벡터 검색 점수 결과 ===");
            System.out.println("totalScore: " + recommendedDev.totalScore());
            System.out.println("similarityScorePercent: " + recommendedDev.similarityScorePercent());
            System.out.println("techstackScorePercent: " + recommendedDev.techstackScorePercent());
            System.out.println("domainMatch: " + recommendedDev.domainMatch());
            System.out.println("matchedTechstacks: " + recommendedDev.matchedTechstacks());
        }

        @Test
        @DisplayName("프리뷰에서도 점수가 정상적으로 계산된다")
        void vectorSearchPreview_scoresCalculatedCorrectly() {
            // given - 간단한 설정
            Member developer = memberRepository.save(Member.builder()
                    .clerkId("clerk_preview_dev")
                    .name("프리뷰개발자")
                    .nickname("previewDev")
                    .mainType(MemberMainType.DEVELOPER)
                    .disclosure(true)
                    .used(MemberStatus.ACTIVE)
                    .build());

            memberCategoryRepository.save(MemberCategory.builder()
                    .member(developer)
                    .category(testCategory)
                    .build());

            devTechstackRepository.save(DevTechstack.builder()
                    .member(developer)
                    .techstack(backendTechstack)
                    .source(TechstackSource.MANUAL)
                    .build());

            GitRepoUrl gitRepoUrl = gitRepoUrlRepository.save(GitRepoUrl.builder()
                    .member(developer)
                    .gitUrl("https://github.com/previewdev/repo")
                    .gitDescription("Preview repo")
                    .build());

            DevReport devReport = devReportRepository.save(DevReport.builder()
                    .gitRepoUrl(gitRepoUrl)
                    .content("{\"summary\": \"preview test\"}")
                    .reportType(ReportType.MAIN)
                    .build());

            float[] devEmbedding = new float[1536];
            for (int i = 0; i < 1536; i++) {
                devEmbedding[i] = 0.3f;
            }
            reportEmbeddingRepository.save(ReportEmbedding.builder()
                    .devReport(devReport)
                    .embedding(devEmbedding)
                    .status(EmbeddingStatus.SUCCESS)
                    .build());

            Project project = projectRepository.save(Project.builder()
                    .name("프리뷰테스트 프로젝트")
                    .content("프리뷰 내용")
                    .status(ProjectStatus.RECRUITING)
                    .projectField(ProjectField.WEB)
                    .mode(ProjectMode.ONLINE)
                    .durationRange(DurationRange.ONE_TO_THREE)
                    .location("서울")
                    .recruitmentDeadline(LocalDate.now().plusDays(30))
                    .category(testCategory)
                    .member(testMember)
                    .build());

            projectRequirementMemberRepository.save(ProjectRequirementMember.builder()
                    .project(project)
                    .part(ProjectPart.BACKEND)
                    .requirementNum(1)
                    .currentCount(0)
                    .build());

            float[] projectEmbedding = new float[1536];
            for (int i = 0; i < 1536; i++) {
                projectEmbedding[i] = 0.3f;
            }
            projectEmbeddingRepository.save(ProjectEmbedding.builder()
                    .project(project)
                    .embedding(projectEmbedding)
                    .status(EmbeddingStatus.SUCCESS)
                    .build());

            // when
            List<MemberResDTO.RecommendedDeveloperDTO> result =
                    memberQueryService.findRecommendedDevelopersPreview(testMember, project.getId(), 5);

            // then
            assertThat(result).isNotEmpty();

            MemberResDTO.RecommendedDeveloperDTO dev = result.stream()
                    .filter(d -> d.member().nickname().equals("previewDev"))
                    .findFirst()
                    .orElse(null);

            assertThat(dev).isNotNull();
            assertThat(dev.totalScore()).isNotNull();
            assertThat(dev.similarityScorePercent()).isNotNull();
            assertThat(dev.domainMatch()).isTrue();

            System.out.println("=== 프리뷰 벡터 검색 점수 결과 ===");
            System.out.println("totalScore: " + dev.totalScore());
            System.out.println("similarityScorePercent: " + dev.similarityScorePercent());
            System.out.println("techstackScorePercent: " + dev.techstackScorePercent());
            System.out.println("domainMatch: " + dev.domainMatch());
        }
    }

}
