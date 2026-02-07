package com.umc.devine.domain.member.repository;

import com.umc.devine.domain.category.entity.Category;
import com.umc.devine.domain.category.entity.mapping.MemberCategory;
import com.umc.devine.domain.category.enums.CategoryGenre;
import com.umc.devine.domain.category.repository.CategoryRepository;
import com.umc.devine.domain.category.repository.MemberCategoryRepository;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.enums.MemberMainType;
import com.umc.devine.domain.member.enums.MemberStatus;
import com.umc.devine.domain.techstack.entity.Techstack;
import com.umc.devine.domain.techstack.entity.mapping.DevTechstack;
import com.umc.devine.domain.techstack.enums.TechGenre;
import com.umc.devine.domain.techstack.enums.TechName;
import com.umc.devine.domain.techstack.enums.TechstackSource;
import com.umc.devine.domain.techstack.repository.DevTechstackRepository;
import com.umc.devine.domain.techstack.repository.TechstackRepository;
import com.umc.devine.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class MemberRepositoryTest extends IntegrationTestSupport {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private MemberCategoryRepository memberCategoryRepository;

    @Autowired
    private TechstackRepository techstackRepository;

    @Autowired
    private DevTechstackRepository devTechstackRepository;

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
    @DisplayName("findByNickname")
    class FindByNicknameTest {

        @Test
        @DisplayName("닉네임으로 활성 회원을 조회한다")
        void findByNickname_success() {
            // when
            Optional<Member> result = memberRepository.findByNickname("testuser");

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getNickname()).isEqualTo("testuser");
        }

        @Test
        @DisplayName("탈퇴한 회원은 닉네임으로 조회되지 않는다")
        void findByNickname_deletedMember() {
            // given
            testMember.withdraw();
            memberRepository.save(testMember);

            // when
            Optional<Member> result = memberRepository.findByNickname("testuser");

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("존재하지 않는 닉네임은 빈 Optional을 반환한다")
        void findByNickname_notFound() {
            // when
            Optional<Member> result = memberRepository.findByNickname("nonexistent");

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("existsByNickname")
    class ExistsByNicknameTest {

        @Test
        @DisplayName("존재하는 닉네임이면 true를 반환한다")
        void existsByNickname_true() {
            // when
            boolean result = memberRepository.existsByNickname("testuser");

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("존재하지 않는 닉네임이면 false를 반환한다")
        void existsByNickname_false() {
            // when
            boolean result = memberRepository.existsByNickname("nonexistent");

            // then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("findByClerkId")
    class FindByClerkIdTest {

        @Test
        @DisplayName("ClerkId로 회원을 조회한다")
        void findByClerkId_success() {
            // when
            Optional<Member> result = memberRepository.findByClerkId("clerk_test_123");

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getClerkId()).isEqualTo("clerk_test_123");
        }
    }

    @Nested
    @DisplayName("findDevelopersByFilters")
    class FindDevelopersByFiltersTest {

        @Test
        @DisplayName("필터 없이 모든 개발자를 조회한다")
        void findDevelopersByFilters_noFilter() {
            // when
            Page<Member> result = memberRepository.findDevelopersByFilters(
                    MemberMainType.DEVELOPER,
                    null,
                    null,
                    null,
                    PageRequest.of(0, 10)
            );

            // then
            assertThat(result.getContent()).isNotEmpty();
        }

        @Test
        @DisplayName("카테고리로 필터링하여 개발자를 조회한다")
        void findDevelopersByFilters_withCategory() {
            // given
            MemberCategory memberCategory = MemberCategory.builder()
                    .member(testMember)
                    .category(testCategory)
                    .build();
            memberCategoryRepository.save(memberCategory);

            // when
            Page<Member> result = memberRepository.findDevelopersByFilters(
                    MemberMainType.DEVELOPER,
                    CategoryGenre.HEALTHCARE,
                    null,
                    null,
                    PageRequest.of(0, 10)
            );

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getNickname()).isEqualTo("testuser");
        }

        @Test
        @DisplayName("기술 장르로 필터링하여 개발자를 조회한다")
        void findDevelopersByFilters_withTechGenre() {
            // given
            DevTechstack devTechstack = DevTechstack.builder()
                    .id(new DevTechstack.DevTechstackId(testMember.getId(), testTechstack.getId()))
                    .member(testMember)
                    .techstack(testTechstack)
                    .source(TechstackSource.MANUAL)
                    .build();
            devTechstackRepository.save(devTechstack);

            // when
            Page<Member> result = memberRepository.findDevelopersByFilters(
                    MemberMainType.DEVELOPER,
                    null,
                    TechGenre.LANGUAGE,
                    null,
                    PageRequest.of(0, 10)
            );

            // then
            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("기술 스택 이름으로 필터링하여 개발자를 조회한다")
        void findDevelopersByFilters_withTechstackName() {
            // given
            DevTechstack devTechstack = DevTechstack.builder()
                    .id(new DevTechstack.DevTechstackId(testMember.getId(), testTechstack.getId()))
                    .member(testMember)
                    .techstack(testTechstack)
                    .source(TechstackSource.MANUAL)
                    .build();
            devTechstackRepository.save(devTechstack);

            // when
            Page<Member> result = memberRepository.findDevelopersByFilters(
                    MemberMainType.DEVELOPER,
                    null,
                    null,
                    TechName.JAVA,
                    PageRequest.of(0, 10)
            );

            // then
            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("비공개 프로필은 조회되지 않는다")
        void findDevelopersByFilters_excludePrivate() {
            // given
            Member privateMember = memberRepository.save(Member.builder()
                    .clerkId("clerk_private_123")
                    .name("비공개")
                    .nickname("privateuser")
                    .mainType(MemberMainType.DEVELOPER)
                    .disclosure(false)
                    .used(MemberStatus.ACTIVE)
                    .build());

            // when
            Page<Member> result = memberRepository.findDevelopersByFilters(
                    MemberMainType.DEVELOPER,
                    null,
                    null,
                    null,
                    PageRequest.of(0, 10)
            );

            // then
            assertThat(result.getContent())
                    .extracting(Member::getNickname)
                    .doesNotContain("privateuser");
        }
    }

    @Nested
    @DisplayName("findAllByMainType")
    class FindAllByMainTypeTest {

        @Test
        @DisplayName("역할별로 회원을 조회한다")
        void findAllByMainType_success() {
            // when
            List<Member> result = memberRepository.findAllByMainType(
                    MemberMainType.DEVELOPER,
                    PageRequest.of(0, 10)
            );

            // then
            assertThat(result).isNotEmpty();
            assertThat(result).allMatch(m -> m.getMainType() == MemberMainType.DEVELOPER);
        }

        @Test
        @DisplayName("limit 만큼만 조회한다")
        void findAllByMainType_limit() {
            // given
            for (int i = 0; i < 5; i++) {
                memberRepository.save(Member.builder()
                        .clerkId("clerk_" + i)
                        .name("테스트" + i)
                        .nickname("user" + i)
                        .mainType(MemberMainType.DEVELOPER)
                        .disclosure(true)
                        .used(MemberStatus.ACTIVE)
                        .build());
            }

            // when
            List<Member> result = memberRepository.findAllByMainType(
                    MemberMainType.DEVELOPER,
                    PageRequest.of(0, 3)
            );

            // then
            assertThat(result).hasSize(3);
        }
    }

    @Nested
    @DisplayName("existsByClerkId")
    class ExistsByClerkIdTest {

        @Test
        @DisplayName("존재하는 ClerkId면 true를 반환한다")
        void existsByClerkId_true() {
            // when
            boolean result = memberRepository.existsByClerkId("clerk_test_123");

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("존재하지 않는 ClerkId면 false를 반환한다")
        void existsByClerkId_false() {
            // when
            boolean result = memberRepository.existsByClerkId("nonexistent_clerk_id");

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("탈퇴한 회원의 ClerkId는 false를 반환한다")
        void existsByClerkId_deletedMember() {
            // given
            testMember.withdraw();
            memberRepository.save(testMember);

            // when
            boolean result = memberRepository.existsByClerkId("clerk_test_123");

            // then
            assertThat(result).isFalse();
        }
    }
}
