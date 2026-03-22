package com.umc.devine.domain.member.repository;

import com.umc.devine.domain.member.entity.GitRepoUrl;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.enums.MemberMainType;
import com.umc.devine.domain.member.enums.MemberStatus;
import com.umc.devine.support.CoreIntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class GitRepoUrlRepositoryTest extends CoreIntegrationTestSupport {

    @Autowired
    private GitRepoUrlRepository gitRepoUrlRepository;

    @Autowired
    private MemberRepository memberRepository;

    private Member testMember;

    @BeforeEach
    void setUp() {
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
    @DisplayName("findAllByMember")
    class FindAllByMemberTest {

        @Test
        @DisplayName("회원의 모든 깃 레포 URL을 조회한다")
        void findAllByMember_success() {
            // given
            GitRepoUrl repo1 = gitRepoUrlRepository.save(GitRepoUrl.builder()
                    .member(testMember)
                    .gitUrl("https://github.com/user/repo1")
                    .build());

            GitRepoUrl repo2 = gitRepoUrlRepository.save(GitRepoUrl.builder()
                    .member(testMember)
                    .gitUrl("https://github.com/user/repo2")
                    .build());

            // when
            List<GitRepoUrl> result = gitRepoUrlRepository.findAllByMember(testMember);

            // then
            assertThat(result).hasSize(2);
            assertThat(result).extracting(GitRepoUrl::getGitUrl)
                    .containsExactlyInAnyOrder(
                            "https://github.com/user/repo1",
                            "https://github.com/user/repo2"
                    );
        }

        @Test
        @DisplayName("깃 레포가 없으면 빈 리스트를 반환한다")
        void findAllByMember_empty() {
            // when
            List<GitRepoUrl> result = gitRepoUrlRepository.findAllByMember(testMember);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByIdWithMember")
    class FindByIdWithMemberTest {

        @Test
        @DisplayName("ID로 깃 레포를 조회하면서 Member를 페치 조인한다")
        void findByIdWithMember_success() {
            // given
            GitRepoUrl repo = gitRepoUrlRepository.save(GitRepoUrl.builder()
                    .member(testMember)
                    .gitUrl("https://github.com/user/repo")
                    .build());

            // when
            Optional<GitRepoUrl> result = gitRepoUrlRepository.findByIdWithMember(repo.getId());

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getGitUrl()).isEqualTo("https://github.com/user/repo");
            assertThat(result.get().getMember().getNickname()).isEqualTo("testuser");
        }

        @Test
        @DisplayName("존재하지 않는 ID로 조회하면 빈 Optional을 반환한다")
        void findByIdWithMember_notFound() {
            // when
            Optional<GitRepoUrl> result = gitRepoUrlRepository.findByIdWithMember(999L);

            // then
            assertThat(result).isEmpty();
        }
    }
}
