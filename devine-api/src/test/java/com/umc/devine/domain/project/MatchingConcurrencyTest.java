package com.umc.devine.domain.project;

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
import com.umc.devine.domain.project.repository.MatchingRepository;
import com.umc.devine.domain.project.repository.ProjectRepository;
import com.umc.devine.domain.project.repository.ProjectRequirementMemberRepository;
import com.umc.devine.domain.project.service.command.MatchingCommandService;
import com.umc.devine.support.IntegrationTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/** 두 요청이 실제로 겹치도록 테스트 트랜잭션 없이 커밋된 데이터로 확인한다. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class MatchingConcurrencyTest extends IntegrationTestSupport {

    @Autowired private MatchingCommandService matchingCommandService;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private ProjectRequirementMemberRepository requirementMemberRepository;
    @Autowired private MatchingRepository matchingRepository;
    @Autowired private MemberRepository memberRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    private Member pm;
    private Member dev;
    private Project project;
    private ProjectRequirementMember requirement;

    @BeforeEach
    void setUp() {
        pm = saveMember("clerk_race_pm", "racepm", MemberMainType.PM);
        dev = saveMember("clerk_race_dev", "racedev", MemberMainType.DEVELOPER);
        project = projectRepository.save(Project.builder()
                .name("경합 테스트 프로젝트").content("소개").status(ProjectStatus.RECRUITING)
                .projectField(ProjectField.WEB).mode(ProjectMode.ONLINE)
                .durationRange(DurationRange.ONE_TO_THREE).location("온라인")
                .recruitmentDeadline(LocalDate.now().plusDays(10))
                .category(categoryRepository.findByGenre(CategoryGenre.ECOMMERCE).orElseThrow())
                .member(pm)
                .build());
        requirement = requirementMemberRepository.save(ProjectRequirementMember.builder()
                .project(project).part(ProjectPart.BACKEND).requirementNum(2).currentCount(0)
                .build());
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM notification WHERE receiver_id IN (?, ?)", pm.getId(), dev.getId());
        jdbcTemplate.update("DELETE FROM matching WHERE project_id = ?", project.getId());
        jdbcTemplate.update("DELETE FROM project_requirement_member WHERE project_id = ?", project.getId());
        jdbcTemplate.update("DELETE FROM project WHERE project_id = ?", project.getId());
        jdbcTemplate.update("DELETE FROM member WHERE member_id IN (?, ?)", pm.getId(), dev.getId());
    }

    private Member saveMember(String clerkId, String nickname, MemberMainType type) {
        return memberRepository.save(Member.builder()
                .clerkId(clerkId).name(nickname).nickname(nickname)
                .mainType(type).disclosure(true).used(MemberStatus.ACTIVE)
                .build());
    }

    @Test
    @DisplayName("PM의 제안 취소와 개발자의 수락이 동시에 들어오면 하나만 성공하고 모집 인원과 매칭 상태가 맞는다")
    void cancelProposalAndAccept_concurrently_onlyOneWins() throws Exception {
        Matching proposal = matchingRepository.save(Matching.builder()
                .project(project).member(dev).matchingType(MatchingType.PROPOSE)
                .status(MatchingStatus.PENDING).part(ProjectPart.BACKEND)
                .build());

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        List<Callable<Throwable>> tasks = List.of(
                () -> { ready.countDown(); start.await();
                    return catchThrowable(() -> matchingCommandService.cancelProposal(pm, proposal.getId())); },
                () -> { ready.countDown(); start.await();
                    return catchThrowable(() -> matchingCommandService.respondToProposal(dev, proposal.getId(), MatchingDecision.ACCEPT)); });

        ExecutorService executor = Executors.newFixedThreadPool(2);
        List<Future<Throwable>> futures = tasks.stream().map(executor::submit).toList();
        ready.await();
        start.countDown();
        long failures = 0;
        for (Future<Throwable> future : futures) {
            if (future.get() != null) {
                failures++;
            }
        }
        executor.shutdown();

        assertThat(failures).isEqualTo(1);
        MatchingStatus status = matchingRepository.findById(proposal.getId()).orElseThrow().getStatus();
        int currentCount = requirementMemberRepository.findById(requirement.getId()).orElseThrow().getCurrentCount();
        assertThat(currentCount).isEqualTo(status == MatchingStatus.COMPLETED ? 1 : 0);
    }
}
