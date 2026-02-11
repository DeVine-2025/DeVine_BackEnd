package com.umc.devine.domain.member.service.query;

import com.umc.devine.domain.category.entity.Category;
import com.umc.devine.domain.category.entity.mapping.MemberCategory;
import com.umc.devine.domain.category.enums.CategoryGenre;
import com.umc.devine.domain.category.repository.CategoryRepository;
import com.umc.devine.domain.category.repository.MemberCategoryRepository;
import com.umc.devine.domain.member.dto.MemberReqDTO;
import com.umc.devine.domain.member.dto.MemberResDTO;
import com.umc.devine.domain.member.entity.Contact;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.entity.Terms;
import com.umc.devine.domain.member.enums.ContactType;
import com.umc.devine.domain.member.enums.MemberMainType;
import com.umc.devine.domain.member.enums.MemberStatus;
import com.umc.devine.domain.member.exception.MemberException;
import com.umc.devine.domain.member.repository.ContactRepository;
import com.umc.devine.domain.member.repository.MemberRepository;
import com.umc.devine.domain.member.repository.TermsRepository;
import com.umc.devine.domain.project.entity.Project;
import com.umc.devine.domain.project.entity.ProjectRequirementMember;
import com.umc.devine.domain.project.enums.DurationRange;
import com.umc.devine.domain.project.enums.ProjectField;
import com.umc.devine.domain.project.enums.ProjectMode;
import com.umc.devine.domain.project.enums.ProjectPart;
import com.umc.devine.domain.project.enums.ProjectStatus;
import com.umc.devine.domain.project.repository.ProjectRepository;
import com.umc.devine.domain.project.repository.ProjectRequirementMemberRepository;
import com.umc.devine.domain.techstack.entity.mapping.ProjectRequirementTechstack;
import com.umc.devine.domain.techstack.repository.ProjectRequirementTechstackRepository;
import com.umc.devine.domain.techstack.dto.TechstackResDTO;
import com.umc.devine.domain.techstack.entity.Techstack;
import com.umc.devine.domain.techstack.entity.mapping.DevTechstack;
import com.umc.devine.domain.techstack.enums.TechGenre;
import com.umc.devine.domain.techstack.enums.TechName;
import com.umc.devine.domain.techstack.enums.TechstackSource;
import com.umc.devine.domain.techstack.repository.DevTechstackRepository;
import com.umc.devine.domain.techstack.repository.TechstackRepository;
import com.umc.devine.global.dto.PagedResponse;
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

    private Member testMember;
    private Category testCategory;
    private Techstack testTechstack;

    @BeforeEach
    void setUp() {
        testCategory = categoryRepository.save(Category.builder()
                .genre(CategoryGenre.HEALTHCARE)
                .build());

        testTechstack = techstackRepository.save(Techstack.builder()
                .name(TechName.JAVA)
                .genre(TechGenre.LANGUAGE)
                .build());

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
            // given
            termsRepository.save(Terms.builder()
                    .title("서비스 이용약관")
                    .content("서비스 약관 내용")
                    .required(true)
                    .build());

            termsRepository.save(Terms.builder()
                    .title("개인정보 처리방침")
                    .content("개인정보 약관 내용")
                    .required(true)
                    .build());

            // when
            MemberResDTO.TermsListDTO result = memberQueryService.findAllTerms();

            // then
            assertThat(result.terms()).hasSize(2);
        }

        @Test
        @DisplayName("약관이 없으면 빈 리스트를 반환한다")
        void findAllTerms_empty() {
            // when
            MemberResDTO.TermsListDTO result = memberQueryService.findAllTerms();

            // then
            assertThat(result.terms()).isEmpty();
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
        @DisplayName("프로젝트 기반 추천 목록 페이지네이션 조회 성공")
        void findRecommendedDevelopers_success() {
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
                    .projectField(ProjectField.ALL)
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

            // then
            assertThat(result.getContent()).isNotEmpty();
            assertThat(result.getContent().get(0).member().nickname()).isEqualTo("testuser");
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
        @DisplayName("여러 개발자 조회 시 N+1 없이 정상 조회")
        void findRecommendedDevelopers_multipleDevs_noN1() {
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
                    .projectField(ProjectField.ALL)
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

            // then
            assertThat(result.getContent()).hasSize(3);
            assertThat(result.getContent()).allSatisfy(dev -> {
                assertThat(dev.domains()).isNotNull();
                assertThat(dev.techstacks()).isNotNull();
            });
        }

        @Test
        @DisplayName("카테고리/기술스택 없는 개발자도 NPE 없이 정상 조회")
        void findRecommendedDevelopers_noCategoryOrTechstack_noNPE() {
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
                    .projectField(ProjectField.ALL)
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

            // then - NPE 발생하지 않고 정상 조회
            assertThat(result.getContent()).isNotEmpty();
            MemberResDTO.RecommendedDeveloperDTO devWithCatResult = result.getContent().stream()
                    .filter(d -> d.member().nickname().equals("devWithCat"))
                    .findFirst()
                    .orElseThrow();
            assertThat(devWithCatResult.domains()).hasSize(1);
            assertThat(devWithCatResult.techstacks()).isEmpty();
        }

        @Test
        @DisplayName("프로젝트 요구 기술스택 없을 때도 정상 동작")
        void findRecommendedDevelopers_noProjectTechstack_success() {
            // given
            memberCategoryRepository.save(MemberCategory.builder().member(testMember).category(testCategory).build());
            devTechstackRepository.save(DevTechstack.builder().member(testMember).techstack(testTechstack).source(TechstackSource.MANUAL).build());

            // 프로젝트에 요구 기술스택 없음
            Project project = projectRepository.save(Project.builder()
                    .name("기술스택 없는 프로젝트")
                    .content("내용")
                    .status(ProjectStatus.RECRUITING)
                    .projectField(ProjectField.ALL)
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

            // then
            assertThat(result.getContent()).isNotEmpty();
            assertThat(result.getContent().get(0).matchedTechstacks()).isEmpty();
        }

        @Test
        @DisplayName("domainMatch 정상 계산 - 일치하는 경우")
        void findRecommendedDevelopers_domainMatch_true() {
            // given
            memberCategoryRepository.save(MemberCategory.builder().member(testMember).category(testCategory).build());

            Project project = projectRepository.save(Project.builder()
                    .name("테스트 프로젝트")
                    .content("내용")
                    .status(ProjectStatus.RECRUITING)
                    .projectField(ProjectField.ALL)
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

            // then
            assertThat(result.getContent().get(0).domainMatch()).isTrue();
        }

        @Test
        @DisplayName("domainMatch 정상 계산 - 일치하지 않는 경우")
        void findRecommendedDevelopers_domainMatch_false() {
            // given
            Category otherCategory = categoryRepository.save(Category.builder()
                    .genre(CategoryGenre.EDUCATION)
                    .build());
            memberCategoryRepository.save(MemberCategory.builder().member(testMember).category(otherCategory).build());

            Project project = projectRepository.save(Project.builder()
                    .name("테스트 프로젝트")
                    .content("내용")
                    .status(ProjectStatus.RECRUITING)
                    .projectField(ProjectField.ALL)
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
        @DisplayName("matchedTechstacks 정상 계산 - 일치하는 기술스택이 있는 경우")
        void findRecommendedDevelopers_matchedTechstacks_success() {
            // given
            Techstack springTechstack = techstackRepository.save(Techstack.builder()
                    .name(TechName.SPRINGBOOT)
                    .genre(TechGenre.FRAMEWORK)
                    .build());

            memberCategoryRepository.save(MemberCategory.builder().member(testMember).category(testCategory).build());
            devTechstackRepository.save(DevTechstack.builder().member(testMember).techstack(testTechstack).source(TechstackSource.MANUAL).build());
            devTechstackRepository.save(DevTechstack.builder().member(testMember).techstack(springTechstack).source(TechstackSource.MANUAL).build());

            Project project = projectRepository.save(Project.builder()
                    .name("테스트 프로젝트")
                    .content("내용")
                    .status(ProjectStatus.RECRUITING)
                    .projectField(ProjectField.ALL)
                    .mode(ProjectMode.ONLINE)
                    .durationRange(DurationRange.ONE_TO_THREE)
                    .location("서울")
                    .recruitmentDeadline(LocalDate.now().plusDays(7))
                    .category(testCategory)
                    .member(testMember)
                    .build());

            // 프로젝트 요구사항에 기술스택 추가
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

            // then
            assertThat(result.getContent()).isNotEmpty();
            MemberResDTO.RecommendedDeveloperDTO developer = result.getContent().get(0);
            assertThat(developer.matchedTechstacks()).hasSize(2);
            assertThat(developer.matchedTechstacks()).containsExactlyInAnyOrder("JAVA", "SPRINGBOOT");
        }

        @Test
        @DisplayName("matchedTechstacks 정상 계산 - 일부만 일치하는 경우")
        void findRecommendedDevelopers_matchedTechstacks_partial() {
            // given
            Techstack springTechstack = techstackRepository.save(Techstack.builder()
                    .name(TechName.SPRINGBOOT)
                    .genre(TechGenre.FRAMEWORK)
                    .build());

            Techstack kotlinTechstack = techstackRepository.save(Techstack.builder()
                    .name(TechName.KOTLIN)
                    .genre(TechGenre.LANGUAGE)
                    .build());

            memberCategoryRepository.save(MemberCategory.builder().member(testMember).category(testCategory).build());
            // 개발자는 JAVA만 보유
            devTechstackRepository.save(DevTechstack.builder().member(testMember).techstack(testTechstack).source(TechstackSource.MANUAL).build());

            Project project = projectRepository.save(Project.builder()
                    .name("테스트 프로젝트")
                    .content("내용")
                    .status(ProjectStatus.RECRUITING)
                    .projectField(ProjectField.ALL)
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

            // 프로젝트는 JAVA, SPRING, KOTLIN 요구
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

            // then
            assertThat(result.getContent()).isNotEmpty();
            MemberResDTO.RecommendedDeveloperDTO developer = result.getContent().get(0);
            assertThat(developer.matchedTechstacks()).hasSize(1);
            assertThat(developer.matchedTechstacks()).containsExactly("JAVA");
        }
    }

    @Nested
    @DisplayName("개발자 추천 프리뷰 조회")
    class FindRecommendedDevelopersPreviewTest {

        @Test
        @DisplayName("프리뷰 목록 조회 성공")
        void findRecommendedDevelopersPreview_success() {
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
                    .projectField(ProjectField.ALL)
                    .mode(ProjectMode.ONLINE)
                    .durationRange(DurationRange.ONE_TO_THREE)
                    .location("서울")
                    .recruitmentDeadline(LocalDate.now().plusDays(7))
                    .category(testCategory)
                    .member(testMember)
                    .build());

            // when
            List<MemberResDTO.RecommendedDeveloperDTO> result = memberQueryService.findRecommendedDevelopersPreview(testMember, project.getId(), 4);

            // then
            assertThat(result).isNotEmpty();
            assertThat(result.size()).isLessThanOrEqualTo(4);
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

            Techstack springTechstack = techstackRepository.save(Techstack.builder()
                    .name(TechName.SPRINGBOOT)
                    .genre(TechGenre.FRAMEWORK)
                    .build());

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

}
