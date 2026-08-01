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

    /**
     * 신고 처리 연동용 비노출 전환.
     *
     * <p>관리자 API와 달리 예외를 던지지 않는다. 신고 처리는 대상 프로젝트가 이미 삭제됐거나 존재하지 않아도
     * 정상 완료돼야 하는데, 같은 트랜잭션 안에서 예외가 밖으로 나가면 호출자가 catch하더라도 트랜잭션이
     * rollback-only로 마킹돼 신고 상태 변경까지 함께 롤백되기 때문이다.
     *
     * @return 비노출 처리가 실제로 수행됐으면 true, 조치 대상이 없으면 false
     */
    boolean hideForModeration(Long projectId, String processorClerkId);
}
