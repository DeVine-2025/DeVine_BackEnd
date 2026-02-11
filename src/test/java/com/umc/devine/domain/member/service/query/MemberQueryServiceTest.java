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
import com.umc.devine.domain.project.enums.DurationRange;
import com.umc.devine.domain.project.enums.ProjectField;
import com.umc.devine.domain.project.enums.ProjectMode;
import com.umc.devine.domain.project.enums.ProjectStatus;
import com.umc.devine.domain.project.repository.ProjectRepository;
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
            MemberResDTO.UserProfileDTO result = memberQueryService.findMemberByNickname("testuser");

            // then
            assertThat(result.nickname()).isEqualTo("testuser");
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
    class FindAllDevelopersTest {

        @Test
        @DisplayName("프로젝트 기반 추천 목록 페이지네이션 조회 성공")
        void findAllDevelopers_success() {
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
            PagedResponse<MemberResDTO.DeveloperDTO> result = memberQueryService.findAllDevelopers(testMember, dto);

            // then
            assertThat(result.getContent()).isNotEmpty();
            assertThat(result.getContent().get(0).nickname()).isEqualTo("testuser");
        }
    }

    @Nested
    @DisplayName("개발자 추천 프리뷰 조회")
    class FindAllDevelopersPreviewTest {

        @Test
        @DisplayName("프리뷰 목록 조회 성공")
        void findAllDevelopersPreview_success() {
            // given
            DevTechstack devTechstack = DevTechstack.builder()
                    .member(testMember)
                    .techstack(testTechstack)
                    .source(TechstackSource.MANUAL)
                    .build();
            devTechstackRepository.save(devTechstack);

            // when
            List<MemberResDTO.DeveloperDTO> result = memberQueryService.findAllDevelopersPreview(testMember, 4);

            // then
            assertThat(result).isNotEmpty();
            assertThat(result.size()).isLessThanOrEqualTo(4);
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
            PagedResponse<MemberResDTO.UserProfileDTO> result = memberQueryService.searchDevelopers(dto);

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

}
