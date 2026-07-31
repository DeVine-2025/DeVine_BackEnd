package com.umc.devine.domain.project.repository;

import com.umc.devine.domain.category.entity.Category;
import com.umc.devine.domain.category.enums.CategoryGenre;
import com.umc.devine.domain.category.repository.CategoryRepository;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.enums.MemberMainType;
import com.umc.devine.domain.member.enums.MemberStatus;
import com.umc.devine.domain.member.repository.MemberRepository;
import com.umc.devine.domain.project.entity.Project;
import com.umc.devine.domain.project.entity.mapping.Matching;
import com.umc.devine.domain.project.enums.*;
import com.umc.devine.domain.project.enums.mapping.MatchingStatus;
import com.umc.devine.domain.project.enums.mapping.MatchingType;
import com.umc.devine.support.CoreIntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class MatchingHistoryPurgeTest extends CoreIntegrationTestSupport {

    @Autowired
    private MatchingRepository matchingRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProjectRepository projectRepository;

    private Project project;

    @BeforeEach
    void setUp() {
        Member pm = memberRepository.save(Member.builder()
                .clerkId("clerk_matching_pm")
                .nickname("matchingpm")
                .mainType(MemberMainType.PM)
                .disclosure(true)
                .used(MemberStatus.ACTIVE)
                .build());
        Category category = categoryRepository.save(Category.builder().genre(CategoryGenre.ECOMMERCE).build());
        project = projectRepository.save(Project.builder()
                .name("프로젝트")
                .content("내용")
                .status(ProjectStatus.RECRUITING)
                .projectField(ProjectField.WEB)
                .mode(ProjectMode.ONLINE)
                .durationRange(DurationRange.ONE_TO_THREE)
                .location("온라인")
                .recruitmentDeadline(LocalDate.now().plusDays(30))
                .category(category)
                .member(pm)
                .build());
    }

    private Member withdrawnMember(String clerkId, String nickname, LocalDateTime deletedAt) {
        Member member = memberRepository.save(Member.builder()
                .clerkId(clerkId)
                .nickname(nickname)
                .mainType(MemberMainType.DEVELOPER)
                .disclosure(true)
                .used(MemberStatus.DELETED)
                .build());
        member.finalizeWithdrawal();
        setDeletedAt(member, deletedAt);
        return memberRepository.save(member);
    }

    private void setDeletedAt(Member member, LocalDateTime deletedAt) {
        try {
            var field = Member.class.getDeclaredField("deletedAt");
            field.setAccessible(true);
            field.set(member, deletedAt);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private Matching createMatching(Member member) {
        return matchingRepository.save(Matching.builder()
                .status(MatchingStatus.PENDING)
                .matchingType(MatchingType.APPLY)
                .project(project)
                .member(member)
                .build());
    }

    @Test
    @DisplayName("탈퇴 후 1년이 지난 회원의 매칭 이력은 파기된다")
    void bulkDeleteByWithdrawnMemberDeletedAtBefore_deletesExpired() {
        // given
        Member expired = withdrawnMember("clerk_matching_expired", "matchexpired", LocalDateTime.now().minusYears(1).minusDays(1));
        Matching expiredMatching = createMatching(expired);

        Member recentlyWithdrawn = withdrawnMember("clerk_matching_recent", "matchrecent", LocalDateTime.now().minusDays(1));
        Matching recentMatching = createMatching(recentlyWithdrawn);

        // when
        int deleted = matchingRepository.bulkDeleteByWithdrawnMemberDeletedAtBefore(LocalDateTime.now().minusYears(1));

        // then
        assertThat(deleted).isEqualTo(1);
        assertThat(matchingRepository.findById(expiredMatching.getId())).isEmpty();
        assertThat(matchingRepository.findById(recentMatching.getId())).isPresent();
    }

    @Test
    @DisplayName("탈퇴하지 않은 회원의 매칭 이력은 대상이 아니다")
    void bulkDeleteByWithdrawnMemberDeletedAtBefore_ignoresActiveMembers() {
        // given
        Member active = memberRepository.save(Member.builder()
                .clerkId("clerk_matching_active")
                .nickname("matchingactive")
                .mainType(MemberMainType.DEVELOPER)
                .disclosure(true)
                .used(MemberStatus.ACTIVE)
                .build());
        Matching matching = createMatching(active);

        // when
        int deleted = matchingRepository.bulkDeleteByWithdrawnMemberDeletedAtBefore(LocalDateTime.now().plusYears(10));

        // then
        assertThat(deleted).isZero();
        assertThat(matchingRepository.findById(matching.getId())).isPresent();
    }
}
