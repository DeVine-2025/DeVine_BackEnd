package com.umc.devine.domain.project.controller;

import com.umc.devine.domain.project.dto.ProjectReqDTO;
import com.umc.devine.domain.project.dto.ProjectResDTO;
import com.umc.devine.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Project", description = "프로젝트 관련 API")
public interface ProjectControllerDocs {

    @Operation(summary = "프로젝트 생성 API", description = "새로운 프로젝트를 생성하는 API입니다. 현재는 하드코딩된 ID(1L)를 사용합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "CREATED, 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청입니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증이 필요합니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "접근 권한이 없습니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없습니다.")
    })
    ApiResponse<ProjectResDTO.CreateProjectRes> createProject(
            @Valid @RequestBody ProjectReqDTO.CreateProjectReq request
    );

    @Operation(summary = "프로젝트 수정 API", description = "기존 프로젝트 정보를 수정하는 API입니다. 현재는 하드코딩된 ID(1L)를 사용하며, 작성자 본인만 수정 가능합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK, 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청입니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증이 필요합니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "접근 권한이 없습니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "해당 프로젝트를 찾을 수 없습니다.")
    })
    ApiResponse<ProjectResDTO.UpdateProjectRes> updateProject(
            @PathVariable("projectId") Long projectId,
            @Valid @RequestBody ProjectReqDTO.UpdateProjectReq request
    );

    @Operation(summary = "프로젝트 삭제 API", description = "기존 프로젝트를 삭제하는 API입니다. 현재는 하드코딩된 ID(1L)를 사용하며, 작성자 본인만 삭제 가능합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK, 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청입니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증이 필요합니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "접근 권한이 없습니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "해당 프로젝트를 찾을 수 없습니다.")
    })
    ApiResponse<Void> deleteProject(
            @PathVariable("projectId") Long projectId
    );

    @Operation(summary = "프로젝트 상세 조회 API", description = "프로젝트 ID를 기반으로 상세 내용을 조회하는 API입니다. 현재는 하드코딩된 ID(1L)를 사용합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK, 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청입니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증이 필요합니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "접근 권한이 없습니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "해당 프로젝트를 찾을 수 없습니다.")
    })
    ApiResponse<ProjectResDTO.UpdateProjectRes> getProjectDetail(
            @PathVariable("projectId") Long projectId
    );
}