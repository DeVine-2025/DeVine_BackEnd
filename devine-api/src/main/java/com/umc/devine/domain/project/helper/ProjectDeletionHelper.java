package com.umc.devine.domain.project.helper;

import com.umc.devine.domain.bookmark.enums.BookmarkType;
import com.umc.devine.domain.bookmark.repository.BookmarkRepository;
import com.umc.devine.domain.project.entity.Project;
import com.umc.devine.domain.project.entity.ProjectRequirementMember;
import com.umc.devine.domain.project.repository.MatchingRepository;
import com.umc.devine.domain.project.repository.ProjectEmbeddingRepository;
import com.umc.devine.domain.project.repository.ProjectImageRepository;
import com.umc.devine.domain.project.repository.ProjectRepository;
import com.umc.devine.domain.project.repository.ProjectRequirementMemberRepository;
import com.umc.devine.domain.techstack.repository.ProjectRequirementTechstackRepository;
import com.umc.devine.domain.techstack.repository.ProjectTechstackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/** 프로젝트 삭제와 회원 탈퇴에서 같이 쓴다. 탈퇴에서는 상태 확인 없이 부른다. */
@Component
@RequiredArgsConstructor
public class ProjectDeletionHelper {

    private final ProjectRepository projectRepository;
    private final ProjectImageRepository projectImageRepository;
    private final ProjectRequirementMemberRepository projectRequirementMemberRepository;
    private final ProjectRequirementTechstackRepository projectRequirementTechstackRepository;
    private final ProjectTechstackRepository projectTechstackRepository;
    private final ProjectEmbeddingRepository projectEmbeddingRepository;
    private final BookmarkRepository bookmarkRepository;
    private final MatchingRepository matchingRepository;

    public void delete(Project project) {
        // 이미지 파일(S3)은 ImageCleanupScheduler가 연결이 끊긴 프로젝트 이미지로 보고 정리한다
        projectImageRepository.deleteAllByProject(project);
        project.clearImages();

        List<ProjectRequirementMember> requirements = projectRequirementMemberRepository.findAllByProject(project);
        if (!requirements.isEmpty()) {
            projectRequirementTechstackRepository.deleteAllByRequirementIn(requirements);
        }
        projectRequirementMemberRepository.deleteAllByProject(project);
        project.clearRequirements();

        projectTechstackRepository.deleteAllByProject(project);
        projectEmbeddingRepository.findByProjectId(project.getId()).ifPresent(projectEmbeddingRepository::delete);
        bookmarkRepository.deleteAllByTargetTypeAndTargetId(BookmarkType.PROJECT, project.getId());

        // 매칭 이력이 있으면 상대 회원의 지원 이력에 이름을 남겨야 하므로 행을 유지한다
        if (matchingRepository.existsByProject(project)) {
            project.eraseForDeletion();
        } else {
            projectRepository.delete(project);
        }
    }
}
