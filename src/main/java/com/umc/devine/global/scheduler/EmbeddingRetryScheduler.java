package com.umc.devine.global.scheduler;

import com.umc.devine.domain.project.entity.ProjectEmbedding;
import com.umc.devine.domain.project.repository.ProjectEmbeddingRepository;
import com.umc.devine.global.enums.EmbeddingStatus;
import com.umc.devine.infrastructure.fastapi.FastApiEmbeddingClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmbeddingRetryScheduler {

    private static final int MAX_RETRY_COUNT = 5;

    private final ProjectEmbeddingRepository projectEmbeddingRepository;
    private final FastApiEmbeddingClient fastApiEmbeddingClient;

    /**
     * 매 30분마다 실패한 프로젝트 임베딩 재시도
     * - FAILED 상태이면서 retryCount < 5인 것들만 재시도
     * - 5회 초과 시 더 이상 재시도하지 않음 (관리자 개입 필요)
     * - @Transactional 제거: 개별 retryProjectEmbedding 호출마다 별도 트랜잭션 처리
     *   (장시간 HTTP 호출 동안 DB 커넥션 점유 방지)
     */
    @Scheduled(cron = "0 */30 * * * *", zone = "Asia/Seoul")
    public void retryFailedProjectEmbeddings() {
        List<ProjectEmbedding> failedEmbeddings = projectEmbeddingRepository
                .findByStatusAndRetryCountLessThan(EmbeddingStatus.FAILED, MAX_RETRY_COUNT);

        if (failedEmbeddings.isEmpty()) {
            log.debug("[EmbeddingRetry] 재시도할 프로젝트 임베딩 없음");
            return;
        }

        log.info("[EmbeddingRetry] 프로젝트 임베딩 재시도 시작 - {}건", failedEmbeddings.size());

        int successCount = 0;
        int failCount = 0;

        for (ProjectEmbedding embedding : failedEmbeddings) {
            try {
                // retryProjectEmbedding이 boolean 반환 (성공: true, 실패: false)
                boolean success = fastApiEmbeddingClient.retryProjectEmbedding(embedding);
                if (success) {
                    successCount++;
                } else {
                    failCount++;
                }
            } catch (Exception e) {
                failCount++;
                if (embedding.getRetryCount() >= MAX_RETRY_COUNT - 1) {
                    log.error("[EmbeddingRetry] 최종 재시도 실패 - 관리자 확인 필요 - projectId: {}, retryCount: {}",
                            embedding.getProject().getId(), embedding.getRetryCount(), e);
                } else {
                    log.warn("[EmbeddingRetry] 프로젝트 임베딩 재시도 실패 - projectId: {}, retryCount: {}",
                            embedding.getProject().getId(), embedding.getRetryCount(), e);
                }
            }
        }

        log.info("[EmbeddingRetry] 프로젝트 임베딩 재시도 완료 - 총 {}건 중 성공 {}건, 실패 {}건",
                failedEmbeddings.size(), successCount, failCount);
    }
}
