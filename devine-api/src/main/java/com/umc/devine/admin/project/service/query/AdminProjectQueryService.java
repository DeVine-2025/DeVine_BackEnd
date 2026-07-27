package com.umc.devine.admin.project.service.query;

import com.umc.devine.admin.project.dto.AdminProjectReqDTO;
import com.umc.devine.admin.project.dto.AdminProjectResDTO;
import com.umc.devine.global.dto.PagedResponse;

public interface AdminProjectQueryService {

    /**
     * 관리자 페이지용 프로젝트 목록을 조회한다.
     * 삭제(DELETED)된 프로젝트는 노출 전환 대상이 아니므로 제외된다.
     */
    PagedResponse<AdminProjectResDTO.ProjectSummaryDTO> getProjectList(AdminProjectReqDTO.SearchReq request);
}