package com.umc.devine.domain.member.service;

import com.umc.devine.domain.category.enums.CategoryGenre;
import com.umc.devine.domain.category.repository.CategoryRepository;
import com.umc.devine.domain.member.dto.MemberReqDTO;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.enums.*;
import com.umc.devine.domain.member.repository.MemberRepository;
import com.umc.devine.domain.member.service.query.MemberQueryService;
import com.umc.devine.domain.notification.enums.NotificationType;
import com.umc.devine.domain.notification.repository.NotificationRepository;
import com.umc.devine.domain.notification.service.command.NotificationCommandService;
import com.umc.devine.domain.project.dto.ProjectReqDTO;
import com.umc.devine.domain.project.entity.Project;
import com.umc.devine.domain.project.entity.mapping.Matching;
import com.umc.devine.domain.project.enums.*;
import com.umc.devine.domain.project.enums.mapping.*;
import com.umc.devine.domain.project.repository.*;
import com.umc.devine.domain.project.service.command.MatchingCommandService;
import com.umc.devine.domain.project.service.query.ProjectQueryService;
import com.umc.devine.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.LocalDate;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

class MemberBlockEffectsTest extends IntegrationTestSupport {
    @Autowired MemberBlockService blocks;
    @Autowired MemberRepository members;
    @Autowired ProjectRepository projects;
    @Autowired MatchingRepository matchings;
    @Autowired CategoryRepository categories;
    @Autowired ProjectQueryService projectQueries;
    @Autowired MemberQueryService memberQueries;
    @Autowired MatchingCommandService matchingCommands;
    @Autowired com.umc.devine.domain.project.service.query.MatchingQueryService matchingQueries;
    @Autowired NotificationRepository notifications;
    @Autowired NotificationCommandService notificationCommands;
    @Autowired com.umc.devine.domain.bookmark.repository.BookmarkRepository bookmarks;
    @Autowired com.umc.devine.domain.bookmark.service.query.BookmarkQueryService bookmarkQueries;
    @Autowired com.umc.devine.domain.member.repository.GitRepoUrlRepository repos;
    @Autowired com.umc.devine.domain.report.repository.DevReportRepository reports;
    @Autowired com.umc.devine.domain.report.service.query.ReportQueryService reportQueries;
    @Autowired com.umc.devine.domain.complaint.service.command.ComplaintCommandService complaints;
    @Autowired com.umc.devine.domain.chat.repository.ChatRoomRepository rooms;
    @Autowired com.umc.devine.domain.chat.repository.ChatMessageRepository messages;
    @Autowired com.umc.devine.domain.project.repository.ProjectRecommendRepository projectRecommendations;
    @Autowired com.umc.devine.domain.member.repository.MemberRecommendRepository memberRecommendations;
    @Autowired com.umc.devine.domain.techstack.repository.TechstackRepository techstacks;
    @Autowired jakarta.persistence.EntityManager em;
    Member pm, dev;
    Project project;

    @BeforeEach
    void setup() {
        pm = member("effectpm", MemberMainType.PM);
        dev = member("effectdev", MemberMainType.DEVELOPER);
        project = projects.save(Project.builder().name("차단프로젝트").content("내용")
                .member(pm).category(categories.findByGenre(CategoryGenre.ECOMMERCE).orElseThrow())
                .status(ProjectStatus.RECRUITING).projectField(ProjectField.WEB)
                .mode(ProjectMode.ONLINE).durationRange(DurationRange.ONE_TO_THREE).location("온라인")
                .recruitmentDeadline(LocalDate.now().plusDays(30)).build());
    }

    private Member member(String nickname, MemberMainType type) {
        return members.save(Member.builder().clerkId(nickname).name(nickname).nickname(nickname)
                .mainType(type).used(MemberStatus.ACTIVE).build());
    }

    private Matching matching(MatchingType type) {
        return matchings.save(Matching.builder().project(project).member(dev).matchingType(type)
                .status(MatchingStatus.PENDING).part(ProjectPart.BACKEND).build());
    }

    @Test
    void cancelsBothPendingTypesAndClosesExpiredProjectWithoutNotifyingBlocker() {
        Matching apply = matching(MatchingType.APPLY);
        Matching proposal = matching(MatchingType.PROPOSE);
        assertThat(blocks.confirmation(dev, pm.getNickname()).pendingCount()).isEqualTo(2);
        project.markDeadlinePassed();
        blocks.block(dev, pm.getNickname());
        assertThat(apply.getStatus()).isEqualTo(MatchingStatus.CANCELLED);
        assertThat(proposal.getStatus()).isEqualTo(MatchingStatus.CANCELLED);
        assertThat(project.getStatus()).isEqualTo(ProjectStatus.CLOSED);
        assertThat(notifications.countByReceiverIdAndIsReadFalse(dev.getId())).isZero();
        assertThat(notifications.countByReceiverIdAndIsReadFalse(pm.getId())).isEqualTo(2);
        blocks.unblock(dev, pm.getNickname());
        assertThat(apply.getStatus()).isEqualTo(MatchingStatus.CANCELLED);
    }

    @Test
    void blocksBothDirectionsButAnonymousPublicLookupStillWorks() {
        blocks.block(dev, pm.getNickname());
        assertThatThrownBy(() -> projectQueries.getProjectDetail(dev, project.getId()))
                .isInstanceOf(com.umc.devine.domain.project.exception.ProjectException.class);
        assertThatCode(() -> projectQueries.getProjectDetail(null, project.getId())).doesNotThrowAnyException();
        assertThatThrownBy(() -> memberQueries.findMemberByNickname(dev, pm.getNickname()))
                .isInstanceOf(com.umc.devine.domain.member.exception.MemberException.class);
        assertThatThrownBy(() -> memberQueries.findMemberByNickname(pm, dev.getNickname()))
                .isInstanceOf(com.umc.devine.domain.member.exception.MemberException.class);
        assertThatCode(() -> memberQueries.findMemberByNickname(null, pm.getNickname())).doesNotThrowAnyException();
    }

    @Test
    void filtersBeforePaginationAndTotals() {
        blocks.block(dev, pm.getNickname());
        var projectResult = projectQueries.searchProjects(dev,
                new ProjectReqDTO.SearchProjectReq(null, null, null, null, null, 1, 1));
        assertThat(projectResult.projects().getTotalElements()).isZero();
        var memberResult = memberQueries.searchDevelopers(dev, new MemberReqDTO.SearchDeveloperDTO(null, null, 1, 100));
        assertThat(memberResult.getContent()).extracting(item -> item.member().nickname()).doesNotContain(pm.getNickname());
    }

    @Test
    void rejectsNewApplicationsAndProposals() {
        blocks.block(pm, dev.getNickname());
        assertThatThrownBy(() -> matchingCommands.applyToProject(dev, project.getId(), ProjectPart.BACKEND))
                .isInstanceOf(com.umc.devine.domain.project.exception.MatchingException.class);
        assertThatThrownBy(() -> matchingCommands.proposeToMember(pm, dev.getNickname(), project.getId(), ProjectPart.BACKEND, "제안"))
                .isInstanceOf(com.umc.devine.domain.project.exception.MatchingException.class);
    }

    @Test
    void suppressesOrdinaryNotificationsButKeepsModerationAndCancellation() {
        blocks.block(pm, dev.getNickname());
        long before = notifications.count();
        notificationCommands.create(NotificationType.MATCHING_APPLIED, pm.getId(), dev.getId(), "지원", 1L);
        notificationCommands.create(NotificationType.MATCHING_PROPOSED, dev.getId(), pm.getId(), "제안", 1L);
        assertThat(notifications.count()).isEqualTo(before);
        notificationCommands.create(NotificationType.COMPLAINT_RESOLVED_RESPONDENT, dev.getId(), pm.getId(), "결과", 1L);
        notificationCommands.create(NotificationType.MATCHING_PROPOSAL_CANCELLED, dev.getId(), pm.getId(), "취소", 1L);
        assertThat(notifications.count()).isEqualTo(before + 2);
    }

    @Test
    void keepsCompletedMatchingNamesButHidesProfileAndProjectDetails() {
        Matching accepted = matching(MatchingType.APPLY);
        accepted.applyDecision(MatchingDecision.ACCEPT);
        blocks.block(pm, dev.getNickname());
        var developers = matchingQueries.getDevelopers(pm, MatchingType.APPLY,
                org.springframework.data.domain.PageRequest.of(0, 10)).developers().getContent();
        assertThat(developers).hasSize(1);
        assertThat(developers.getFirst().developerNickname()).isEqualTo(dev.getNickname());
        assertThat(developers.getFirst().developerId()).isNull();
        assertThat(developers.getFirst().canViewDetail()).isFalse();
        var history = matchingQueries.getProjects(dev, MatchingType.APPLY,
                org.springframework.data.domain.PageRequest.of(0, 10)).projects().getContent();
        assertThat(history).hasSize(1);
        assertThat(history.getFirst().projectName()).isEqualTo(project.getName());
        assertThat(history.getFirst().projectId()).isNull();
        assertThat(history.getFirst().positions()).isEmpty();
        assertThat(history.getFirst().canViewDetail()).isFalse();
    }

    @Test
    void hidesBookmarksHomeAndMemberAncillaryEndpointsAndRestoresAfterUnblocking() {
        bookmarks.save(com.umc.devine.domain.bookmark.entity.Bookmark.builder().member(dev)
                .targetType(com.umc.devine.domain.bookmark.enums.BookmarkType.PROJECT).targetId(project.getId()).build());
        bookmarks.save(com.umc.devine.domain.bookmark.entity.Bookmark.builder().member(dev)
                .targetType(com.umc.devine.domain.bookmark.enums.BookmarkType.DEVELOPER).targetId(pm.getId()).build());
        blocks.block(pm, dev.getNickname());
        assertThat(bookmarkQueries.findAllBookmarks(dev).bookmarks()).isEmpty();
        assertThat(projectQueries.getWeeklyBestProjects(dev).projects()).isEmpty();
        assertThatThrownBy(() -> memberQueries.findTechstacksByNickname(dev, pm.getNickname()))
                .isInstanceOf(com.umc.devine.domain.member.exception.MemberException.class);
        assertThatThrownBy(() -> memberQueries.findContributionsByNickname(dev, pm.getNickname(), null, null))
                .isInstanceOf(com.umc.devine.domain.member.exception.MemberException.class);
        assertThatThrownBy(() -> memberQueries.findReposByNickname(dev, pm.getNickname(), org.springframework.data.domain.PageRequest.of(0, 10)))
                .isInstanceOf(com.umc.devine.domain.member.exception.MemberException.class);
        assertThatThrownBy(() -> memberQueries.findProjectsByNickname(dev, pm.getNickname(), List.of(ProjectStatus.RECRUITING), org.springframework.data.domain.PageRequest.of(0, 10)))
                .isInstanceOf(com.umc.devine.domain.member.exception.MemberException.class);
        blocks.unblock(pm, dev.getNickname());
        assertThat(bookmarkQueries.findAllBookmarks(dev).bookmarks()).hasSize(2);
        assertThat(projectQueries.getWeeklyBestProjects(dev).projects()).hasSize(1);
    }

    @Test
    void hidesPublicReportsFromBlockedViewerButKeepsAnonymousAccess() {
        var repo = repos.save(com.umc.devine.domain.member.entity.GitRepoUrl.builder().member(dev)
                .gitUrl("https://github.com/example/blocked").build());
        reports.save(com.umc.devine.domain.report.entity.DevReport.builder().gitRepoUrl(repo)
                .reportType(com.umc.devine.domain.report.enums.ReportType.MAIN).content("{}").build());
        blocks.block(pm, dev.getNickname());
        assertThatThrownBy(() -> reportQueries.getMainReport(pm.getId(), repo.getId()))
                .isInstanceOf(com.umc.devine.domain.report.exception.ReportException.class);
        assertThatThrownBy(() -> reportQueries.getReportsByNickname(pm, dev.getNickname(),
                com.umc.devine.domain.report.enums.ReportType.MAIN, org.springframework.data.domain.PageRequest.of(0, 10)))
                .isInstanceOf(com.umc.devine.domain.member.exception.MemberException.class);
        assertThatCode(() -> reportQueries.getMainReport(null, repo.getId())).doesNotThrowAnyException();
    }

    @Test
    void canReportOldChatAfterBlockingAndResponseReflectsOwnBlock() {
        var room = rooms.save(com.umc.devine.domain.chat.entity.ChatRoom.builder().member1(pm).member2(dev).build());
        var message = messages.save(com.umc.devine.domain.chat.entity.ChatMessage.builder()
                .chatRoom(room).sender(pm).content("신고 대상").build());
        blocks.block(dev, pm.getNickname());
        var result = complaints.create(dev, new com.umc.devine.domain.complaint.dto.ComplaintReqDTO.CreateReq(
                com.umc.devine.domain.complaint.enums.ComplaintTargetType.CHAT, message.getId(), null,
                com.umc.devine.domain.complaint.enums.ComplaintReasonCode.ETC, "상세"));
        assertThat(result.blockTargetAlreadyBlocked()).isTrue();
        assertThat(result.blockTargetNickname()).isEqualTo(pm.getNickname());
    }

    @Test
    void excludesBothDirectionsFromNativeVectorRecommendationsAndTheirCount() {
        var repo = repos.save(com.umc.devine.domain.member.entity.GitRepoUrl.builder().member(dev)
                .gitUrl("https://github.com/example/vector").build());
        var report = reports.save(com.umc.devine.domain.report.entity.DevReport.builder().gitRepoUrl(repo)
                .reportType(com.umc.devine.domain.report.enums.ReportType.MAIN).content("{}").build());
        float[] vector = new float[1536];
        vector[0] = 1;
        var reportEmbedding = com.umc.devine.domain.report.entity.ReportEmbedding.builder()
                .devReport(report).embedding(vector).status(com.umc.devine.global.enums.EmbeddingStatus.SUCCESS).build();
        em.persist(reportEmbedding);
        em.persist(com.umc.devine.domain.project.entity.ProjectEmbedding.builder().project(project)
                .embedding(vector).status(com.umc.devine.global.enums.EmbeddingStatus.SUCCESS).build());
        em.persist(com.umc.devine.domain.project.entity.ProjectRequirementMember.builder().project(project)
                .part(ProjectPart.BACKEND).requirementNum(1).build());
        em.persist(com.umc.devine.domain.techstack.entity.mapping.DevTechstack.builder().member(dev)
                .techstack(techstacks.findByName(com.umc.devine.domain.techstack.enums.TechName.BACKEND).orElseThrow())
                .source(com.umc.devine.domain.techstack.enums.TechstackSource.MANUAL).build());
        em.flush();
        assertThat(projectRecommendations.findRecommendedProjects(dev.getId(), reportEmbedding.getId(), 10,
                null, null, null, null)).hasSize(1);
        assertThat(memberRecommendations.findRecommendedDevelopers(project.getId(), pm.getId(), 10, 0)).hasSize(1);
        assertThat(memberRecommendations.countRecommendedDevelopers(project.getId(), pm.getId())).isEqualTo(1);
        blocks.block(dev, pm.getNickname());
        assertRecommendationsEmpty(reportEmbedding.getId());
        blocks.unblock(dev, pm.getNickname());
        blocks.block(pm, dev.getNickname());
        assertRecommendationsEmpty(reportEmbedding.getId());
    }

    private void assertRecommendationsEmpty(Long embeddingId) {
        assertThat(projectRecommendations.findRecommendedProjects(dev.getId(), embeddingId, 10,
                null, null, null, null)).isEmpty();
        assertThat(memberRecommendations.findRecommendedDevelopers(project.getId(), pm.getId(), 10, 0)).isEmpty();
        assertThat(memberRecommendations.countRecommendedDevelopers(project.getId(), pm.getId())).isZero();
    }
}
