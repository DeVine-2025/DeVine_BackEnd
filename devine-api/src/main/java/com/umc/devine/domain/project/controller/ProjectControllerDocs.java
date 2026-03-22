package com.umc.devine.domain.project.controller;

import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.project.dto.ProjectReqDTO;
import com.umc.devine.domain.project.dto.ProjectResDTO;
import com.umc.devine.global.apiPayload.ApiResponse;
import com.umc.devine.global.security.CurrentMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Project", description = "프로젝트 관련 API")
public interface ProjectControllerDocs {

        @Operation(summary = "프로젝트 생성 API", description = "새로운 프로젝트를 생성하는 API입니다.")
        @ApiResponses({
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "CREATED, 성공"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청입니다."),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증이 필요합니다."),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "접근 권한이 없습니다."),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없습니다.")
        })
        ApiResponse<ProjectResDTO.CreateProjectRes> createProject(
                        @Parameter(hidden = true) @CurrentMember Member member,
                        @Valid @RequestBody ProjectReqDTO.CreateProjectReq request);

        @Operation(summary = "프로젝트 수정 API", description = "기존 프로젝트 정보를 수정하는 API입니다. 작성자 본인만 수정 가능합니다.")
        @ApiResponses({
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK, 성공"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청입니다."),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증이 필요합니다."),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "접근 권한이 없습니다."),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "해당 프로젝트를 찾을 수 없습니다.")
        })
        ApiResponse<ProjectResDTO.UpdateProjectRes> updateProject(
                        @Parameter(hidden = true) @CurrentMember Member member,
                        @PathVariable("projectId") Long projectId,
                        @Valid @RequestBody ProjectReqDTO.UpdateProjectReq request);

        @Operation(summary = "프로젝트 삭제 API", description = "기존 프로젝트를 삭제하는 API입니다. 작성자 본인만 삭제 가능합니다.")
        @ApiResponses({
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK, 성공"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청입니다."),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증이 필요합니다."),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "접근 권한이 없습니다."),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "해당 프로젝트를 찾을 수 없습니다.")
        })
        ApiResponse<Void> deleteProject(
                        @Parameter(hidden = true) @CurrentMember Member member,
                        @PathVariable("projectId") Long projectId);

        @Operation(summary = "프로젝트 상세 조회 API", description = "프로젝트 ID를 기반으로 상세 내용을 조회하는 API입니다. 비회원도 조회 가능합니다.")
        @ApiResponses({
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK, 성공"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청입니다."),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "해당 프로젝트를 찾을 수 없습니다.")
        })
        ApiResponse<ProjectResDTO.UpdateProjectRes> getProjectDetail(
                        @PathVariable("projectId") Long projectId);

        @Operation(summary = "이번 주 주목 프로젝트 조회 (메인 화면 상단)", description = "이번 주(월~일)에 생성된 프로젝트 중 주간 조회수가 높은 상위 4개를 반환합니다.")
        @ApiResponses({
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "이번 주 주목 프로젝트 조회 성공", content = @Content(schema = @Schema(implementation = ProjectResDTO.WeeklyBestProjectsRes.class)))
        })
        ApiResponse<ProjectResDTO.WeeklyBestProjectsRes> getWeeklyBestProjects();

        @Operation(summary = "프로젝트 필터링 조회 (프로젝트/개발자 보기 탭 하단)", description = "다양한 필터 조건으로 프로젝트를 검색합니다.")
        @ApiResponses({
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "프로젝트 검색 성공", content = @Content(schema = @Schema(implementation = ProjectResDTO.SearchProjectsRes.class)))
        })
        ApiResponse<ProjectResDTO.SearchProjectsRes> searchProjects(
                        @ParameterObject @Valid @ModelAttribute ProjectReqDTO.SearchProjectReq request);

        @Operation(
                        summary = "추천 프로젝트 미리보기 조회 (메인 하단 / 프로젝트·개발자 보기 탭 상단)",
                        description = """
                                        벡터 유사도 기반 복합 점수로 추천 프로젝트를 미리보기 형태로 제공합니다.

                                        - 메인 화면 하단: limit=6 (기본값)
                                        - 프로젝트·개발자 보기 탭 상단: limit=4

                                        개발자는 리포트 임베딩 기반 개인화 추천, PM 또는 임베딩 미보유 시 최신 모집 중 프로젝트를 반환합니다.
                                        """
        )
        @ApiResponses({
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "추천 프로젝트 미리보기 조회 성공", content = @Content(schema = @Schema(implementation = ProjectResDTO.RecommendedProjectsRes.class))),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청입니다."),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증이 필요합니다.")
        })
        ApiResponse<ProjectResDTO.RecommendedProjectsRes> getRecommendedProjectsPreview(
                        @Parameter(hidden = true) @CurrentMember Member member,
                        @ParameterObject @Valid @ModelAttribute ProjectReqDTO.RecommendProjectsPreviewReq request);

        @Operation(
                        summary = "추천 프로젝트 조회 (추천 프로젝트 탭용)",
                        description = """
                                        벡터 유사도 기반 복합 점수(리포트 유사도 60% + 기술스택 매칭 20% + 도메인 일치도 20%)로
                                        상위 10개 프로젝트를 추천합니다.

                                        - 개발자: 리포트 임베딩 기반 개인화 추천
                                        - PM 또는 임베딩 미보유: 최신 모집 중 프로젝트 반환

                                        필터 미선택 시 전체 대상으로 추천합니다.
                                        """
        )
        @ApiResponses({
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "추천 프로젝트 조회 성공", content = @Content(schema = @Schema(implementation = ProjectResDTO.RecommendedProjectsRes.class))),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청입니다."),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증이 필요합니다.")
        })
        ApiResponse<ProjectResDTO.RecommendedProjectsRes> getRecommendedProjects(
                        @Parameter(hidden = true) @CurrentMember Member member,
                        @ParameterObject @Valid @ModelAttribute ProjectReqDTO.RecommendProjectsReq request);

        @Operation(summary = "프로젝트 상태 변경 API", description = "프로젝트 상태를 변경합니다. 모집 중 → 진행 중 → 완료 순서로만 전환 가능합니다. 작성자 본인만 변경 가능합니다.")
        @ApiResponses({
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK, 상태 변경 성공"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 상태 전환입니다."),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증이 필요합니다."),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "접근 권한이 없습니다."),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "해당 프로젝트를 찾을 수 없습니다.")
        })
        ApiResponse<Void> changeProjectStatus(
                        @Parameter(hidden = true) @CurrentMember Member member,
                        @PathVariable("projectId") Long projectId,
                        @Valid @RequestBody ProjectReqDTO.ChangeStatusReq request);

        @Operation(summary = "내가 생성한 모집 중인 프로젝트 목록 조회 (개발자 추천 필터용)", description = "내가 생성한 모집 중인 프로젝트만 반환합니다. 수락된 매칭 프로젝트는 포함되지 않습니다.")
        @ApiResponses({
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증이 필요합니다.")
        })
        ApiResponse<ProjectResDTO.MyProjectsRes> getMyCreatedRecruitingProjects(
                        @Parameter(hidden = true) @CurrentMember Member member,
                        @Parameter(hidden = true) Pageable pageable);

        @Operation(summary = "내 프로젝트 - 모집 중", description = "내 프로젝트 중 모집 중인 프로젝트 목록을 조회합니다. PM은 본인이 생성한 프로젝트, 개발자는 수락된 프로젝트가 조회됩니다.")
        @ApiResponses({
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증이 필요합니다.")
        })
        ApiResponse<ProjectResDTO.MyProjectsRes> getMyRecruitingProjects(
                        @Parameter(hidden = true) @CurrentMember Member member,
                        @Parameter(hidden = true) Pageable pageable);

        @Operation(summary = "내 프로젝트 - 진행 중", description = "내 프로젝트 중 진행 중인 프로젝트 목록을 조회합니다. PM은 본인이 생성한 프로젝트, 개발자는 수락된 프로젝트가 조회됩니다.")
        @ApiResponses({
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증이 필요합니다.")
        })
        ApiResponse<ProjectResDTO.MyProjectsRes> getMyInProgressProjects(
                        @Parameter(hidden = true) @CurrentMember Member member,
                        @Parameter(hidden = true) Pageable pageable);

        @Operation(summary = "내 프로젝트 - 완료", description = "내 프로젝트 중 완료된 프로젝트 목록을 조회합니다. PM은 본인이 생성한 프로젝트, 개발자는 수락된 프로젝트가 조회됩니다.")
        @ApiResponses({
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증이 필요합니다.")
        })
        ApiResponse<ProjectResDTO.MyProjectsRes> getMyCompletedProjects(
                        @Parameter(hidden = true) @CurrentMember Member member,
                        @Parameter(hidden = true) Pageable pageable);
}
