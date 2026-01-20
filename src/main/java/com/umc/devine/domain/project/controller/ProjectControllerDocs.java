package com.umc.devine.domain.project.controller;

import com.umc.devine.domain.project.dto.ProjectReqDTO;
import com.umc.devine.domain.project.dto.ProjectResDTO;
import com.umc.devine.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.ModelAttribute;
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
                        @Valid @RequestBody ProjectReqDTO.CreateProjectReq request);

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
                        @Valid @RequestBody ProjectReqDTO.UpdateProjectReq request);

        @Operation(summary = "프로젝트 삭제 API", description = "기존 프로젝트를 삭제하는 API입니다. 현재는 하드코딩된 ID(1L)를 사용하며, 작성자 본인만 삭제 가능합니다.")
        @ApiResponses({
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK, 성공"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청입니다."),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증이 필요합니다."),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "접근 권한이 없습니다."),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "해당 프로젝트를 찾을 수 없습니다.")
        })
        ApiResponse<Void> deleteProject(
                        @PathVariable("projectId") Long projectId);

        @Operation(summary = "프로젝트 상세 조회 API", description = "프로젝트 ID를 기반으로 상세 내용을 조회하는 API입니다. 현재는 하드코딩된 ID(1L)를 사용합니다.")
        @ApiResponses({
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK, 성공"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청입니다."),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증이 필요합니다."),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "접근 권한이 없습니다."),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "해당 프로젝트를 찾을 수 없습니다.")
        })
        ApiResponse<ProjectResDTO.UpdateProjectRes> getProjectDetail(
                        @PathVariable("projectId") Long projectId);

        @Operation(summary = "이번 주 주목 프로젝트 조회 (메인 화면 상단)", description = "이번 주(월~일)에 생성된 프로젝트 중 주간 조회수가 높은 상위 4개를 반환합니다.")
        @ApiResponses({
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "이번 주 주목 프로젝트 조회 성공", content = @Content(schema = @Schema(implementation = ProjectResDTO.WeeklyBestProjectsRes.class)))
        })
        ApiResponse<ProjectResDTO.WeeklyBestProjectsRes> getWeeklyBestProjects();

        @Operation(summary = "프로젝트 필터링 조회 (프로젝트/개발자 보기 탭 하단)", description = "다양한 필터 조건으로 프로젝트를 검색합니다. (페이징)")
        @ApiResponses({
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "프로젝트 검색 성공", content = @Content(schema = @Schema(implementation = ProjectResDTO.SearchProjectsRes.class)))
        })
        ApiResponse<ProjectResDTO.SearchProjectsRes> searchProjects(
                        @Valid @ModelAttribute ProjectReqDTO.SearchProjectReq request);

        // 통합 추천 프로젝트 API
        @Operation(summary = "추천 프로젝트 조회 (통합)", description = """
                        나에게 맞는 추천 프로젝트 조회/검색을 통합 제공합니다.

                        - PREVIEW: limit개만 반환 (필터링 X)
                          * mode=PREVIEW & limit=6  -> 메인 화면 하단 추천 6개
                          * mode=PREVIEW & limit=4  -> 프로젝트/개발자 보기 탭 상단 추천 4개
                          * limit이 null이면 기본값 6 적용

                        - PAGE: 필터링 + 페이징 반환 (추천 프로젝트 탭 용도)
                          * mode=PAGE & page=1 & size=4 (+필터들)

                        추천 알고리즘: TODO - 현재는 최신순으로 반환
                        """)
        @ApiResponses({
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "추천 프로젝트 조회 성공", content = @Content(schema = @Schema(implementation = ProjectResDTO.RecommendedProjectsRes.class))),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청입니다."),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증이 필요합니다."),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "접근 권한이 없습니다.")
        })
        ApiResponse<ProjectResDTO.RecommendedProjectsRes> getRecommendedProjects(
                        @Valid @RequestBody ProjectReqDTO.RecommendProjectsReq request);
}
