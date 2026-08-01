package com.umc.devine.admin.project.service.command;

import com.umc.devine.admin.project.dto.AdminProjectResDTO;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.repository.MemberRepository;
import com.umc.devine.domain.project.entity.Project;
import com.umc.devine.domain.project.enums.ProjectStatus;
import com.umc.devine.domain.project.exception.ProjectException;
import com.umc.devine.domain.project.exception.code.ProjectErrorReason;
import com.umc.devine.domain.project.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class ProjectVisibilityCommandServiceImpl implements ProjectVisibilityCommandService {

    private final ProjectRepository projectRepository;
    private final MemberRepository memberRepository;

    @Override
    public AdminProjectResDTO.UpdateVisibilityRes changeVisibility(Long projectId, boolean visible, String processorClerkId) {
        Project project = findChangeableProject(projectId)
                .orElseThrow(() -> new ProjectException(ProjectErrorReason.PROJECT_NOT_FOUND));

        Member processor = findProcessor(processorClerkId);
        LocalDateTime changedAt = LocalDateTime.now();
        boolean changed = project.changeVisibility(visible, processor, changedAt);

        return AdminProjectResDTO.UpdateVisibilityRes.builder()
                .projectId(project.getId())
                .visible(visible)
                .changed(changed)
                .processorMemberId(processor != null ? processor.getId() : null)
                .changedAt(changedAt)
                .build();
    }

    @Override
    public boolean hideForModeration(Long projectId, String processorClerkId) {
        Optional<Project> target = findChangeableProject(projectId);
        if (target.isEmpty()) {
            return false;
        }

        target.get().changeVisibility(false, findProcessor(processorClerkId), LocalDateTime.now());
        return true;
    }

    // 삭제된 프로젝트는 노출 전환 대상이 아니다. 이미 비노출인 프로젝트는 멱등 처리를 위해 대상에 포함한다.
    private Optional<Project> findChangeableProject(Long projectId) {
        if (projectId == null) {
            return Optional.empty();
        }
        return projectRepository.findById(projectId)
                .filter(project -> project.getStatus() != ProjectStatus.DELETED);
    }

    // 관리자 계정에 대응하는 member 레코드가 없을 수 있으므로 처리자는 없을 수 있다.
    private Member findProcessor(String processorClerkId) {
        return processorClerkId != null
                ? memberRepository.findByClerkId(processorClerkId).orElse(null)
                : null;
    }
}
