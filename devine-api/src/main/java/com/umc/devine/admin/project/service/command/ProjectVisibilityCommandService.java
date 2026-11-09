package com.umc.devine.admin.project.service.command;

import com.umc.devine.admin.project.dto.AdminProjectResDTO;

public interface ProjectVisibilityCommandService {

    /**
     * 프로젝트의 유저 화면 노출 상태를 전환한다(관리자 API용).
     * 이미 동일한 상태여도 예외 없이 정상 처리된다(멱등성 보장).
     *
     * @throws com.umc.devine.domain.project.exception.ProjectException 프로젝트가 없거나 이미 삭제된 경우
     */
    AdminProjectResDTO.UpdateVisibilityRes changeVisibility(Long projectId, boolean visible, String processorClerkId);
}
