package com.umc.devine.infrastructure.fastapi;

import com.umc.devine.domain.embedding.service.command.EmbeddingCommandService;
import com.umc.devine.domain.project.entity.Project;
import com.umc.devine.domain.project.entity.ProjectEmbedding;
import com.umc.devine.domain.project.event.ProjectEmbeddingEvent;
import com.umc.devine.domain.project.repository.ProjectRepository;
import com.umc.devine.infrastructure.fastapi.dto.FastApiReqDto;
import com.umc.devine.infrastructure.fastapi.dto.FastApiResDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Slf4j
@Component
public class FastApiEmbeddingClient {

    private static final int EXPECTED_DIMENSION = 1536;

    private final RestClient fastApiSyncRestClient;
    private final ProjectRepository projectRepository;
    private final EmbeddingCommandService embeddingCommandService;

    public FastApiEmbeddingClient(
            @Qualifier("fastApiSyncRestClient") RestClient fastApiSyncRestClient,
            ProjectRepository projectRepository,
            EmbeddingCommandService embeddingCommandService) {
        this.fastApiSyncRestClient = fastApiSyncRestClient;
        this.projectRepository = projectRepository;
        this.embeddingCommandService = embeddingCommandService;
    }

    /**
     * 트랜잭션 커밋 후 비동기로 프로젝트 임베딩 요청
     * - 레이스 컨디션 방지: AFTER_COMMIT으로 프로젝트 저장 완료 후 실행
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleProjectEmbeddingRequest(ProjectEmbeddingEvent event) {
        log.debug("프로젝트 임베딩 요청 시작 - projectId: {}", event.getProjectId());

        Project project = projectRepository.findById(event.getProjectId())
                .orElse(null);

        if (project == null) {
            log.error("프로젝트 임베딩 실패 - 프로젝트를 찾을 수 없음: {}", event.getProjectId());
            return;
        }

        processEmbeddingRequest(project.getId(), event.getContent());
    }

    /**
     * 실패한 프로젝트 임베딩 재시도 (스케줄러에서 호출)
     *
     * @return 성공 여부 (true: 성공, false: 실패)
     */
    public boolean retryProjectEmbedding(ProjectEmbedding embedding) {
        Project project = embedding.getProject();
        log.debug("프로젝트 임베딩 재시도 - projectId: {}, retryCount: {}",
                project.getId(), embedding.getRetryCount());

        return processEmbeddingRequest(project.getId(), project.getContent());
    }

    /**
     * 임베딩 요청 공통 처리 로직
     *
     * @return 성공 여부
     */
    private boolean processEmbeddingRequest(Long projectId, String content) {
        FastApiReqDto.ProjectEmbeddingReq request = FastApiReqDto.ProjectEmbeddingReq.builder()
                .text(content)
                .build();

        try {
            FastApiResDto.EmbeddingRes response = fastApiSyncRestClient.post()
                    .uri("/api/v1/embeddings/project")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(FastApiResDto.EmbeddingRes.class);

            if (response != null && response.vector() != null) {
                validateVector(response.vector().size(), response.dimension());
                float[] vector = convertToFloatArray(response.vector());
                embeddingCommandService.saveProjectEmbedding(projectId, vector);
                return true;
            } else {
                embeddingCommandService.saveProjectEmbeddingFailure(projectId, "AI 서버 응답이 비어있습니다");
                log.error("프로젝트 임베딩 실패 - projectId: {}, error: 응답 없음", projectId);
                return false;
            }

        } catch (InvalidVectorDimensionException e) {
            embeddingCommandService.saveProjectEmbeddingFailure(projectId, e.getMessage());
            log.error("프로젝트 임베딩 실패 - 벡터 차원 불일치 - projectId: {}, error: {}",
                    projectId, e.getMessage());
            return false;
        } catch (RestClientException e) {
            embeddingCommandService.saveProjectEmbeddingFailure(projectId, e.getMessage());
            log.error("프로젝트 임베딩 API 호출 실패 - projectId: {}", projectId, e);
            return false;
        }
    }

    /**
     * 벡터 차원 검증 - 불일치 시 예외 발생
     */
    private void validateVector(int vectorSize, Integer dimension) {
        if (vectorSize != EXPECTED_DIMENSION) {
            throw new InvalidVectorDimensionException(
                    String.format("벡터 차원 불일치: expected=%d, actual=%d", EXPECTED_DIMENSION, vectorSize));
        }
        if (dimension != null && dimension != EXPECTED_DIMENSION) {
            throw new InvalidVectorDimensionException(
                    String.format("응답 dimension 불일치: expected=%d, actual=%d", EXPECTED_DIMENSION, dimension));
        }
    }

    private float[] convertToFloatArray(List<Double> vector) {
        float[] result = new float[vector.size()];
        for (int i = 0; i < vector.size(); i++) {
            result[i] = vector.get(i).floatValue();
        }
        return result;
    }

    /**
     * 벡터 차원 불일치 예외
     */
    private static class InvalidVectorDimensionException extends RuntimeException {
        public InvalidVectorDimensionException(String message) {
            super(message);
        }
    }
}
