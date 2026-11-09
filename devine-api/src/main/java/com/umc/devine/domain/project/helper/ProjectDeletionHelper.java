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

        if (matchingRepository.existsByProject(project)) {
            project.eraseForDeletion();
        } else {
            projectRepository.delete(project);
        }
    }
}
