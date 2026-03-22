package com.umc.devine.domain.member.converter;

import com.umc.devine.domain.member.dto.MemberResDTO;
import com.umc.devine.domain.member.entity.GitRepoUrl;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.enums.MemberMainType;
import com.umc.devine.domain.member.enums.MemberStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MemberConverterTest {

    private Member testMember;

    @BeforeEach
    void setUp() {
        testMember = Member.builder()
                .clerkId("clerk_test_123")
                .name("테스트")
                .nickname("testuser")
                .mainType(MemberMainType.DEVELOPER)
                .disclosure(true)
                .used(MemberStatus.ACTIVE)
                .build();
    }

    @Nested
    @DisplayName("toGitRepoDTO")
    class ToGitRepoDTOTest {

        @Test
        @DisplayName("리포트가 있는 레포는 hasReport가 true이다")
        void toGitRepoDTO_withReport() {
            // given
            GitRepoUrl gitRepoUrl = GitRepoUrl.builder()
                    .member(testMember)
                    .gitUrl("https://github.com/user/repo")
                    .gitDescription("테스트 레포")
                    .build();
            ReflectionTestUtils.setField(gitRepoUrl, "id", 1L);

            // when
            MemberResDTO.GitRepoDTO result = MemberConverter.toGitRepoDTO(gitRepoUrl, true);

            // then
            assertThat(result.hasReport()).isTrue();
            assertThat(result.gitRepoId()).isEqualTo(1L);
            assertThat(result.name()).isEqualTo("repo");
            assertThat(result.gitUrl()).isEqualTo("https://github.com/user/repo");
            assertThat(result.description()).isEqualTo("테스트 레포");
        }

        @Test
        @DisplayName("리포트가 없는 레포는 hasReport가 false이다")
        void toGitRepoDTO_withoutReport() {
            // given
            GitRepoUrl gitRepoUrl = GitRepoUrl.builder()
                    .member(testMember)
                    .gitUrl("https://github.com/user/another-repo")
                    .gitDescription("다른 레포")
                    .build();
            ReflectionTestUtils.setField(gitRepoUrl, "id", 2L);

            // when
            MemberResDTO.GitRepoDTO result = MemberConverter.toGitRepoDTO(gitRepoUrl, false);

            // then
            assertThat(result.hasReport()).isFalse();
            assertThat(result.gitRepoId()).isEqualTo(2L);
            assertThat(result.name()).isEqualTo("another-repo");
        }
    }

    @Nested
    @DisplayName("toGitRepoListDTO")
    class ToGitRepoListDTOTest {

        @Test
        @DisplayName("리포트 존재 여부에 따라 각 레포의 hasReport가 올바르게 설정된다")
        void toGitRepoListDTO_setsHasReportCorrectly() {
            // given
            GitRepoUrl repo1 = GitRepoUrl.builder()
                    .member(testMember)
                    .gitUrl("https://github.com/user/repo1")
                    .build();
            ReflectionTestUtils.setField(repo1, "id", 1L);

            GitRepoUrl repo2 = GitRepoUrl.builder()
                    .member(testMember)
                    .gitUrl("https://github.com/user/repo2")
                    .build();
            ReflectionTestUtils.setField(repo2, "id", 2L);

            GitRepoUrl repo3 = GitRepoUrl.builder()
                    .member(testMember)
                    .gitUrl("https://github.com/user/repo3")
                    .build();
            ReflectionTestUtils.setField(repo3, "id", 3L);

            List<GitRepoUrl> repos = List.of(repo1, repo2, repo3);
            List<Long> repoIdsWithReport = List.of(1L, 3L); // repo1, repo3만 리포트 있음

            // when
            MemberResDTO.GitRepoListDTO result = MemberConverter.toGitRepoListDTO(repos, repoIdsWithReport);

            // then
            assertThat(result.repos()).hasSize(3);

            MemberResDTO.GitRepoDTO dto1 = result.repos().stream()
                    .filter(r -> r.gitRepoId().equals(1L))
                    .findFirst().orElseThrow();
            assertThat(dto1.hasReport()).isTrue();

            MemberResDTO.GitRepoDTO dto2 = result.repos().stream()
                    .filter(r -> r.gitRepoId().equals(2L))
                    .findFirst().orElseThrow();
            assertThat(dto2.hasReport()).isFalse();

            MemberResDTO.GitRepoDTO dto3 = result.repos().stream()
                    .filter(r -> r.gitRepoId().equals(3L))
                    .findFirst().orElseThrow();
            assertThat(dto3.hasReport()).isTrue();
        }

        @Test
        @DisplayName("리포트가 하나도 없으면 모든 레포의 hasReport가 false이다")
        void toGitRepoListDTO_noReports() {
            // given
            GitRepoUrl repo1 = GitRepoUrl.builder()
                    .member(testMember)
                    .gitUrl("https://github.com/user/repo1")
                    .build();
            ReflectionTestUtils.setField(repo1, "id", 1L);

            List<GitRepoUrl> repos = List.of(repo1);
            List<Long> repoIdsWithReport = List.of(); // 리포트 없음

            // when
            MemberResDTO.GitRepoListDTO result = MemberConverter.toGitRepoListDTO(repos, repoIdsWithReport);

            // then
            assertThat(result.repos()).hasSize(1);
            assertThat(result.repos().get(0).hasReport()).isFalse();
        }

        @Test
        @DisplayName("빈 레포 목록이면 빈 결과를 반환한다")
        void toGitRepoListDTO_emptyRepos() {
            // given
            List<GitRepoUrl> repos = List.of();
            List<Long> repoIdsWithReport = List.of();

            // when
            MemberResDTO.GitRepoListDTO result = MemberConverter.toGitRepoListDTO(repos, repoIdsWithReport);

            // then
            assertThat(result.repos()).isEmpty();
        }
    }
}
