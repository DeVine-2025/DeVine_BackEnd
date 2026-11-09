package com.umc.devine.domain.member.service;

import com.umc.devine.domain.category.enums.CategoryGenre;
import com.umc.devine.domain.category.repository.CategoryRepository;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.enums.*;
import com.umc.devine.domain.member.repository.*;
import com.umc.devine.domain.project.entity.Project;
import com.umc.devine.domain.project.entity.mapping.Matching;
import com.umc.devine.domain.project.enums.*;
import com.umc.devine.domain.project.enums.mapping.*;
import com.umc.devine.domain.project.repository.*;
import com.umc.devine.domain.project.service.command.MatchingCommandService;
import com.umc.devine.support.IntegrationTestSupport;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;

@Transactional(propagation = Propagation.NOT_SUPPORTED)
class MemberBlockTransactionTest extends IntegrationTestSupport {
    @Autowired MemberBlockService blocks;
    @Autowired MatchingCommandService matchingCommands;
    @Autowired MemberRepository members;
    @Autowired MemberBlockRepository blockRepository;
    @Autowired ProjectRepository projects;
    @Autowired MatchingRepository matchings;
    @Autowired CategoryRepository categories;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired EntityManager em;

    record Fixture(Member pm, Member dev, Project project) {}

    private Fixture fixture() {
        return new TransactionTemplate(transactionManager).execute(status -> {
            String suffix = UUID.randomUUID().toString().substring(0, 6);
            Member pm = members.save(Member.builder().clerkId("pm" + suffix).name("PM").nickname("pm" + suffix)
                    .used(MemberStatus.ACTIVE).mainType(MemberMainType.PM).build());
            Member dev = members.save(Member.builder().clerkId("dev" + suffix).name("개발자").nickname("dev" + suffix)
                    .used(MemberStatus.ACTIVE).mainType(MemberMainType.DEVELOPER).build());
            Project project = projects.save(Project.builder().name("경합").content("내용").member(pm)
                    .category(categories.findByGenre(CategoryGenre.ECOMMERCE).orElseThrow())
                    .status(ProjectStatus.RECRUITING).projectField(ProjectField.WEB).mode(ProjectMode.ONLINE)
                    .durationRange(DurationRange.ONE_TO_THREE).location("온라인")
                    .recruitmentDeadline(LocalDate.now().plusDays(1)).build());
            return new Fixture(pm, dev, project);
        });
    }

    private void cleanup(Fixture f) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            List<Long> ids = List.of(f.pm().getId(), f.dev().getId());
            em.createQuery("DELETE FROM Notification n WHERE n.receiver.id IN :ids OR n.sender.id IN :ids").setParameter("ids", ids).executeUpdate();
            em.createQuery("DELETE FROM MemberBlock b WHERE b.blocker.id IN :ids OR b.target.id IN :ids").setParameter("ids", ids).executeUpdate();
            em.createQuery("DELETE FROM Matching m WHERE m.project.id = :id").setParameter("id", f.project().getId()).executeUpdate();
            projects.deleteById(f.project().getId());
            members.deleteById(f.pm().getId());
            members.deleteById(f.dev().getId());
        });
    }

    @Test
    void concurrentApplicationWaitsForBlockCommitAndCannotCreatePendingMatching() throws Exception {
        Fixture f = fixture();
        CountDownLatch blocked = new CountDownLatch(1), release = new CountDownLatch(1), applying = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var block = executor.submit(() -> new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                blocks.block(f.pm(), f.dev().getNickname());
                blocked.countDown();
                await(release);
            }));
            assertThat(blocked.await(10, TimeUnit.SECONDS)).isTrue();
            var apply = executor.submit(() -> {
                applying.countDown();
                assertThatThrownBy(() -> matchingCommands.applyToProject(f.dev(), f.project().getId(), ProjectPart.BACKEND))
                        .isInstanceOf(com.umc.devine.domain.project.exception.MatchingException.class);
            });
            try {
                assertThat(applying.await(10, TimeUnit.SECONDS)).isTrue();
                assertThatThrownBy(() -> apply.get(300, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
            } finally {
                release.countDown();
            }
            block.get(10, TimeUnit.SECONDS);
            apply.get(10, TimeUnit.SECONDS);
            assertThat(matchings.countPendingBetween(f.pm().getId(), f.dev().getId())).isZero();
        } finally {
            release.countDown();
            cleanup(f);
        }
    }

    @Test
    void rollbackRestoresBlockPendingMatchingProjectAndNotificationsTogether() {
        Fixture f = fixture();
        try {
            new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                Project project = projects.findById(f.project().getId()).orElseThrow();
                project.markDeadlinePassed();
                matchings.save(Matching.builder().project(project).member(f.dev()).matchingType(MatchingType.APPLY)
                        .status(MatchingStatus.PENDING).part(ProjectPart.BACKEND).build());
            });
            assertThatThrownBy(() -> new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                blocks.block(f.pm(), f.dev().getNickname());
                throw new IllegalStateException("rollback");
            })).isInstanceOf(IllegalStateException.class);
            assertThat(blockRepository.existsBetween(f.pm().getId(), f.dev().getId())).isFalse();
            assertThat(matchings.countPendingBetween(f.pm().getId(), f.dev().getId())).isEqualTo(1);
            assertThat(projects.findById(f.project().getId()).orElseThrow().getStatus()).isEqualTo(ProjectStatus.DEADLINE_PASSED);
        } finally {
            cleanup(f);
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) throw new AssertionError("작업 대기 시간 초과");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        }
    }
}
