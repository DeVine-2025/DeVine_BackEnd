package com.umc.devine.admin.project.service.query;

import com.umc.devine.admin.project.converter.AdminProjectConverter;
import com.umc.devine.admin.project.dto.AdminProjectReqDTO;
import com.umc.devine.admin.project.dto.AdminProjectResDTO;
import com.umc.devine.domain.project.entity.Project;
import com.umc.devine.domain.project.repository.ProjectRepository;
import com.umc.devine.global.dto.PagedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminProjectQueryServiceImpl implements AdminProjectQueryService {

    private final ProjectRepository projectRepository;

    @Override
    public PagedResponse<AdminProjectResDTO.ProjectSummaryDTO> getProjectList(AdminProjectReqDTO.SearchReq request) {
        Page<Project> projectPage = projectRepository.findForAdmin(request.toHiddenFilter(), request.toPageable());

        List<AdminProjectResDTO.ProjectSummaryDTO> content = projectPage.getContent().stream()
                .map(AdminProjectConverter::toProjectSummaryDTO)
                .toList();

        return PagedResponse.of(projectPage, content);
    }
}