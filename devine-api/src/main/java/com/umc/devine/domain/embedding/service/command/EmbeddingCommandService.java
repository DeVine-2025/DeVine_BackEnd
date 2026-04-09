package com.umc.devine.domain.embedding.service.command;

import com.umc.devine.domain.embedding.dto.EmbeddingCallbackDto.ProjectEmbeddingCallback;
import com.umc.devine.domain.embedding.dto.EmbeddingCallbackDto.ReportEmbeddingCallback;

public interface EmbeddingCommandService {

    void processReportEmbeddingCallback(ReportEmbeddingCallback callback);

    void processProjectEmbeddingCallback(ProjectEmbeddingCallback callback);

    /**
     * 프로젝트 임베딩 저장/업데이트 (FastApiEmbeddingClient에서 호출)
     */
    void saveProjectEmbedding(Long projectId, float[] vector);

    void saveProjectEmbeddingFailure(Long projectId, String errorMessage);

    /**
     * 실패한 프로젝트 임베딩 재시도 (스케줄러에서 호출)
     * @return 성공 여부
     */
    boolean retryProjectEmbedding(Long projectEmbeddingId);
}
