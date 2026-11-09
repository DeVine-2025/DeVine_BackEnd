package com.umc.devine.domain.complaint.helper;

import com.umc.devine.domain.chat.entity.ChatMessage;
import com.umc.devine.domain.chat.repository.ChatMessageRepository;
import com.umc.devine.domain.complaint.entity.Complaint;
import com.umc.devine.domain.complaint.enums.ComplaintTargetType;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.project.entity.Project;
import com.umc.devine.domain.project.enums.ProjectStatus;
import com.umc.devine.domain.project.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ComplaintTargetReader {

    public static final String DELETED_PROJECT_NAME = "삭제된 프로젝트";
    public static final String CHAT_MESSAGE_NAME = "채팅 메시지";
    public static final String DELETED_CONTENT_PLACEHOLDER = "삭제된 콘텐츠입니다";

    private final ProjectRepository projectRepository;
    private final ChatMessageRepository chatMessageRepository;

    public String displayName(Complaint complaint) {
        return switch (complaint.getTargetType()) {
            case PROJECT -> projectRepository.findById(complaint.getTargetId())
                    .filter(project -> project.getStatus() != ProjectStatus.DELETED)
                    .map(Project::getName)
                    .orElse(DELETED_PROJECT_NAME);
            case DEVELOPER -> complaint.getRespondentMember().getNickname();
            case CHAT -> CHAT_MESSAGE_NAME;
        };
    }

    public String resolveContent(Complaint complaint) {
        return switch (complaint.getTargetType()) {
            case PROJECT -> projectRepository.findById(complaint.getTargetId())
                    .filter(project -> project.getStatus() != ProjectStatus.DELETED)
                    .map(Project::getContent)
                    .orElse(DELETED_CONTENT_PLACEHOLDER);
            case CHAT -> chatMessageRepository.findById(complaint.getTargetId())
                    .map(this::formatChatContext)
                    .orElse(DELETED_CONTENT_PLACEHOLDER);
            case DEVELOPER -> null;
        };
    }

    public String buildEvidence(ComplaintTargetType targetType, Long targetId, Member respondent) {
        return switch (targetType) {
            case PROJECT -> projectRepository.findByIdWithCategory(targetId)
                    .map(project -> """
                            [프로젝트명] %s
                            [소개] %s
                            [모집 정보] 분야 %s / 진행 방식 %s / 기간 %s / 모집 마감일 %s
                            """.formatted(project.getName(), project.getContent(), project.getProjectField(),
                            project.getMode(), project.getDurationRange(), project.getRecruitmentDeadline()))
                    .orElse(DELETED_CONTENT_PLACEHOLDER);
            // shortcut: 회원 복사본은 소개 글까지만 담는다. 기술스택·공개 리포트 목록은 복사 기능을 켤 때(약관 개정 후) 보강한다.
            case DEVELOPER -> """
                    [닉네임] %s
                    [소개] %s
                    """.formatted(respondent.getNickname(), respondent.getBody());
            case CHAT -> chatMessageRepository.findById(targetId)
                    .map(this::formatChatContext)
                    .orElse(DELETED_CONTENT_PLACEHOLDER);
        };
    }

    private String formatChatContext(ChatMessage reported) {
        Long roomId = reported.getChatRoom().getId();

        List<ChatMessage> before = new ArrayList<>(
                chatMessageRepository.findTop5ByChatRoomIdAndIdLessThanOrderByIdDesc(roomId, reported.getId()));
        java.util.Collections.reverse(before);
        List<ChatMessage> fromReported =
                chatMessageRepository.findTop6ByChatRoomIdAndIdGreaterThanEqualOrderByIdAsc(roomId, reported.getId());

        List<ChatMessage> context = new ArrayList<>(before);
        context.addAll(fromReported);

        return context.stream()
                .map(message -> "%s[%s] %s".formatted(
                        message.getId().equals(reported.getId()) ? "(신고된 메시지) " : "",
                        message.getSender().getNickname(),
                        message.getContent()))
                .collect(Collectors.joining("\n"));
    }

    public Optional<Project> findVisibleProjectWithMember(Long projectId) {
        return projectId == null ? Optional.empty() : projectRepository.findVisibleByIdWithMember(projectId);
    }

    public Optional<Project> findProject(Long projectId) {
        return projectId == null ? Optional.empty() : projectRepository.findById(projectId);
    }

    public Optional<Project> findBlindableProject(Long projectId) {
        return findProject(projectId).filter(project -> project.getStatus() != ProjectStatus.DELETED);
    }

    public Optional<ChatMessage> findChatMessage(Long chatMessageId) {
        return chatMessageId == null ? Optional.empty() : chatMessageRepository.findById(chatMessageId);
    }
}
