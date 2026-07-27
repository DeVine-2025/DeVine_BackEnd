package com.umc.devine.admin.project.controller;

import com.umc.devine.admin.project.dto.AdminProjectReqDTO;
import com.umc.devine.admin.project.dto.AdminProjectResDTO;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.global.apiPayload.ApiResponse;
import com.umc.devine.global.dto.PagedResponse;
import com.umc.devine.global.security.CurrentMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * TODO: 관리자 인증/인가 기능이 추가되면 관리자 권한 검증을 추가.
 */
@Tag(name = "Admin Project", description = "관리자 프로젝트 노출 관리 API")
public interface AdminProjectControllerDocs {

    @Operation(
            summary = "관리자 프로젝트 목록 조회",
            description = "관리자 페이지용 프로젝트 목록을 조회합니다. 프로젝트 ID, 제목, 글 작성자, 등록일, 노출 상태를 등록일 최신순으로 반환합니다. "
                    + "삭제된 프로젝트는 노출 전환 대상이 아니므로 목록에서 제외됩니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "프로젝트 목록 조회 성공")
    })
    ApiResponse<PagedResponse<AdminProjectResDTO.ProjectSummaryDTO>> getProjectList(
            @ParameterObject @ModelAttribute @Valid AdminProjectReqDTO.SearchReq request
    );

    @Operation(
            summary = "프로젝트 노출/비노출 전환",
            description = "신고 처리 결과 등에 따라 프로젝트 게시글을 유저 화면에서 노출/비노출로 전환합니다. "
                    + "프로젝트의 라이프사이클 상태(모집중/진행중/완료)는 그대로 보존되므로 다시 노출로 되돌리면 원래 상태로 복귀합니다. "
                    + "이미 동일한 노출 상태여도 예외 없이 정상 처리되며(멱등성 보장), 이 경우 응답의 changed가 false입니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "노출 상태 변경 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "노출 상태 누락"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "프로젝트를 찾을 수 없거나 이미 삭제됨")
    })
    ApiResponse<AdminProjectResDTO.UpdateVisibilityRes> updateVisibility(
            @Parameter(hidden = true) @CurrentMember(required = false) Member member,
            @Parameter(description = "프로젝트 ID", required = true) @PathVariable Long projectId,
            @RequestBody @Valid AdminProjectReqDTO.UpdateVisibilityReq request
    );
}
