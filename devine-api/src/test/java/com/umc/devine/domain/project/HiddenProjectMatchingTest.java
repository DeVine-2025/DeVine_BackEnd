package com.umc.devine.domain.project;

import com.umc.devine.domain.category.entity.Category;
import com.umc.devine.domain.category.enums.CategoryGenre;
import com.umc.devine.domain.category.repository.CategoryRepository;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.enums.MemberMainType;
import com.umc.devine.domain.member.enums.MemberStatus;
import com.umc.devine.domain.member.repository.MemberRepository;
import com.umc.devine.domain.project.entity.Project;
import com.umc.devine.domain.project.entity.ProjectRequirementMember;
import com.umc.devine.domain.project.entity.mapping.Matching;
import com.umc.devine.domain.project.enums.*;
import com.umc.devine.domain.project.enums.mapping.MatchingDecision;
import com.umc.devine.domain.project.enums.mapping.MatchingStatus;
import com.umc.devine.domain.project.enums.mapping.MatchingType;
import com.umc.devine.domain.project.exception.MatchingException;
import com.umc.devine.domain.project.exception.code.MatchingErrorReason;
import com.umc.devine.domain.project.repository.MatchingRepository;
import com.umc.devine.domain.project.repository.ProjectRepository;
import com.umc.devine.domain.project.repository.ProjectRequirementMemberRepository;
import com.umc.devine.domain.project.service.command.MatchingCommandService;
import com.umc.devine.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HiddenProjectMatchingTest extends IntegrationTestSupport {

    @Autowired private MatchingCommandService matchingCommandService;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private ProjectRequirementMemberRepository requirementMemberRepository;
    @Autowired private MatchingRepository matchingRepository;
    @Autowired private MemberRepository memberRepository;
    @Autowired private CategoryRepository categoryRepository;

    private Member pm;
    private Matching apply;

    @BeforeEach
    void setUp() {
        pm = saveMember("clerk_hid_pm", "hidpm", MemberMainType.PM);
        Member dev = saveMember("clerk_hid_dev", "hiddev", MemberMainType.DEVELOPER);
        Category category = categoryRepository.findByGenre(CategoryGenre.ECOMMERCE).orElseThrow();

        Project project = projectRepository.save(Project.builder()
                .name("비노출 테스트").content("소개").status(ProjectStatus.RECRUITING)
                .projectField(ProjectField.WEB).mode(ProjectMode.ONLINE)
                .durationRange(DurationRange.ONE_TO_THREE).location("온라인")
                .recruitmentDeadline(LocalDate.now().plusDays(10)).category(category).member(pm)
                .build());
        project.addRequirement(requirementMemberRepository.save(ProjectRequirementMember.builder()
                .project(project).part(ProjectPart.BACKEND).requirementNum(2).currentCount(0)
                .build()));
        apply = matchingRepository.save(Matching.builder()
                .project(project).member(dev).matchingType(MatchingType.APPLY)
                .status(MatchingStatus.PENDING).part(ProjectPart.BACKEND)
                .build());
        project.changeReportBlind(true, LocalDateTime.now());
        projectRepository.save(project);
    }

    private Member saveMember(String clerkId, String nickname, MemberMainType type) {
        return memberRepository.save(Member.builder()
                .clerkId(clerkId).name(nickname).nickname(nickname)
                .mainType(type).disclosure(true).used(MemberStatus.ACTIVE)
                .build());
    }

    @Test
    @DisplayName("비노출 프로젝트의 대기 지원은 수락할 수 없다")
    void hiddenProject_acceptRejected() {
        assertThatThrownBy(() -> matchingCommandService.respondToApplication(pm, apply.getId(), MatchingDecision.ACCEPT))
                .isInstanceOf(MatchingException.class)
                .extracting("reason").isEqualTo(MatchingErrorReason.CANNOT_ACCEPT_WHILE_HIDDEN);
        assertThat(apply.getStatus()).isEqualTo(MatchingStatus.PENDING);
    }

    @Test
    @DisplayName("비노출 프로젝트의 대기 지원은 거절할 수 있다")
    void hiddenProject_rejectAllowed() {
        matchingCommandService.respondToApplication(pm, apply.getId(), MatchingDecision.REJECT);

        assertThat(apply.getStatus()).isEqualTo(MatchingStatus.CANCELLED);
    }
}
