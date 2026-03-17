package com.umc.devine.domain.embedding.controller;

import com.umc.devine.domain.embedding.dto.EmbeddingCallbackDto.ProjectEmbeddingCallback;
import com.umc.devine.domain.embedding.dto.EmbeddingCallbackDto.ReportEmbeddingCallback;
import com.umc.devine.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Embedding Callback (Internal)", description = "AI 서버 내부 통신용 임베딩 콜백 API 헤더를 통한 마스터키 인증이 필요합니다.")
public interface EmbeddingCallbackDocs {

    @Operation(
            summary = "리포트 임베딩 콜백",
            description = "AI 서버(FastAPI)에서 리포트 임베딩 완료 후 호출하는 내부 통신용 콜백입니다. "
                    + "헤더에 Internal API Key를 포함해야 합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "콜백 처리 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Internal API Key가 없거나 유효하지 않음")
    })
    @SecurityRequirement(name = "Authorization")
    ApiResponse<Void> handleReportEmbeddingCallback(
            @RequestBody @Valid ReportEmbeddingCallback request
    );

    @Operation(
            summary = "프로젝트 임베딩 콜백",
            description = "AI 서버(FastAPI)에서 프로젝트 임베딩 완료 후 호출하는 내부 통신용 콜백입니다. "
                    + "헤더에 Internal API Key를 포함해야 합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "콜백 처리 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Internal API Key가 없거나 유효하지 않음")
    })
    @SecurityRequirement(name = "Authorization")
    ApiResponse<Void> handleProjectEmbeddingCallback(
            @RequestBody @Valid ProjectEmbeddingCallback request
    );
}
