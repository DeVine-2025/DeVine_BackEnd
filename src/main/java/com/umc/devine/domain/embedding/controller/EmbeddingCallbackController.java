package com.umc.devine.domain.embedding.controller;

import com.umc.devine.domain.embedding.dto.EmbeddingCallbackDto.ProjectEmbeddingCallback;
import com.umc.devine.domain.embedding.dto.EmbeddingCallbackDto.ReportEmbeddingCallback;
import com.umc.devine.domain.embedding.service.command.EmbeddingCommandService;
import com.umc.devine.global.apiPayload.ApiResponse;
import com.umc.devine.global.apiPayload.code.GeneralSuccessCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/embeddings")
public class EmbeddingCallbackController implements EmbeddingCallbackDocs {

    private final EmbeddingCommandService embeddingCommandService;

    @Override
    @PostMapping("/callback")
    public ApiResponse<Void> handleReportEmbeddingCallback(
            @RequestBody @Valid ReportEmbeddingCallback request
    ) {
        log.debug("리포트 임베딩 콜백 수신 - mainReportId: {}", request.mainReportId());
        embeddingCommandService.processReportEmbeddingCallback(request);
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, null);
    }

    @Override
    @PostMapping("/callback/project")
    public ApiResponse<Void> handleProjectEmbeddingCallback(
            @RequestBody @Valid ProjectEmbeddingCallback request
    ) {
        log.debug("프로젝트 임베딩 콜백 수신 - projectId: {}", request.projectId());
        embeddingCommandService.processProjectEmbeddingCallback(request);
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, null);
    }
}
