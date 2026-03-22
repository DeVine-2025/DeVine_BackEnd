package com.umc.devine.domain.embedding.service.command;

import com.umc.devine.domain.embedding.dto.EmbeddingCallbackDto.CallbackStatus;
import com.umc.devine.domain.embedding.dto.EmbeddingCallbackDto.ProjectEmbeddingCallback;
import com.umc.devine.domain.embedding.dto.EmbeddingCallbackDto.ReportEmbeddingCallback;
import com.umc.devine.domain.project.entity.Project;
import com.umc.devine.domain.project.entity.ProjectEmbedding;
import com.umc.devine.domain.project.exception.ProjectException;
import com.umc.devine.domain.project.exception.code.ProjectErrorCode;
import com.umc.devine.domain.project.repository.ProjectEmbeddingRepository;
import com.umc.devine.domain.project.repository.ProjectRepository;
import com.umc.devine.domain.report.entity.DevReport;
import com.umc.devine.domain.report.entity.ReportEmbedding;
import com.umc.devine.domain.report.exception.ReportException;
import com.umc.devine.domain.report.exception.code.ReportErrorCode;
import com.umc.devine.domain.report.repository.DevReportRepository;
import com.umc.devine.domain.report.repository.ReportEmbeddingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class EmbeddingCommandServiceImpl implements EmbeddingCommandService {

    private static final int EXPECTED_DIMENSION = 1536;

    private final ReportEmbeddingRepository reportEmbeddingRepository;
    private final ProjectEmbeddingRepository projectEmbeddingRepository;
    private final DevReportRepository devReportRepository;
    private final ProjectRepository projectRepository;

    @Override
    public void processReportEmbeddingCallback(ReportEmbeddingCallback callback) {
        log.info("리포트 임베딩 콜백 수신 - mainReportId: {}, status: {}",
                callback.mainReportId(), callback.status());

        DevReport devReport = devReportRepository.findById(callback.mainReportId())
                .orElseThrow(() -> {
                    log.warn("리포트를 찾을 수 없음 (삭제되었을 수 있음) - mainReportId: {}", callback.mainReportId());
                    return new ReportException(ReportErrorCode.REPORT_NOT_FOUND);
                });

        ReportEmbedding embedding = reportEmbeddingRepository.findByDevReportId(callback.mainReportId())
                .orElseGet(() -> ReportEmbedding.builder()
                        .devReport(devReport)
                        .build());

        if (callback.status() == CallbackStatus.SUCCESS) {
            validateReportVector(callback.vector(), callback.dimension());
            float[] vector = convertToFloatArray(callback.vector());
            embedding.updateSuccess(vector);
            log.info("리포트 임베딩 저장 성공 - mainReportId: {}, dimension: {}",
                    callback.mainReportId(), vector.length);
        } else {
            embedding.updateFailure(callback.errorMessage());
            log.warn("리포트 임베딩 실패 - mainReportId: {}, error: {}",
                    callback.mainReportId(), callback.errorMessage());
        }

        reportEmbeddingRepository.save(embedding);
    }

    @Override
    public void processProjectEmbeddingCallback(ProjectEmbeddingCallback callback) {
        log.info("프로젝트 임베딩 콜백 수신 - projectId: {}, status: {}",
                callback.projectId(), callback.status());

        Project project = projectRepository.findById(callback.projectId())
                .orElseThrow(() -> {
                    log.warn("프로젝트를 찾을 수 없음 (삭제되었을 수 있음) - projectId: {}", callback.projectId());
                    return new ProjectException(ProjectErrorCode.PROJECT_NOT_FOUND);
                });

        ProjectEmbedding embedding = projectEmbeddingRepository.findByProjectId(callback.projectId())
                .orElseGet(() -> ProjectEmbedding.builder()
                        .project(project)
                        .build());

        if (callback.status() == CallbackStatus.SUCCESS) {
            validateProjectVector(callback.vector(), callback.dimension());
            float[] vector = convertToFloatArray(callback.vector());
            embedding.updateSuccess(vector);
            log.info("프로젝트 임베딩 저장 성공 - projectId: {}, dimension: {}",
                    callback.projectId(), vector.length);
        } else {
            embedding.updateFailure(callback.errorMessage());
            log.warn("프로젝트 임베딩 실패 - projectId: {}, error: {}",
                    callback.projectId(), callback.errorMessage());
        }

        projectEmbeddingRepository.save(embedding);
    }

    @Override
    public void saveProjectEmbedding(Long projectId, float[] vector) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectException(ProjectErrorCode.PROJECT_NOT_FOUND));

        ProjectEmbedding embedding = projectEmbeddingRepository.findByProjectId(projectId)
                .orElseGet(() -> ProjectEmbedding.builder()
                        .project(project)
                        .build());

        embedding.updateSuccess(vector);
        projectEmbeddingRepository.save(embedding);
        log.info("프로젝트 임베딩 저장 성공 - projectId: {}, dimension: {}", projectId, vector.length);
    }

    @Override
    public void saveProjectEmbeddingFailure(Long projectId, String errorMessage) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectException(ProjectErrorCode.PROJECT_NOT_FOUND));

        ProjectEmbedding embedding = projectEmbeddingRepository.findByProjectId(projectId)
                .orElseGet(() -> ProjectEmbedding.builder()
                        .project(project)
                        .build());

        embedding.updateFailure(errorMessage);
        projectEmbeddingRepository.save(embedding);
        log.warn("프로젝트 임베딩 실패 저장 - projectId: {}, error: {}", projectId, errorMessage);
    }

    @Override
    public boolean retryProjectEmbedding(Long projectEmbeddingId) {
        // 스케줄러에서 호출 시, 이미 조회된 embedding을 사용하도록 별도 구현 필요
        // 현재는 EmbeddingRetryScheduler에서 직접 FastApiEmbeddingClient 호출
        throw new UnsupportedOperationException("스케줄러에서 직접 호출");
    }

    private void validateReportVector(List<Double> vector, Integer dimension) {
        if (vector == null || vector.isEmpty()) {
            throw new ReportException(ReportErrorCode.EMBEDDING_VECTOR_EMPTY);
        }
        if (dimension != null && dimension != EXPECTED_DIMENSION) {
            log.error("응답 dimension 불일치: expected={}, actual={}", EXPECTED_DIMENSION, dimension);
        }
        if (vector.size() != EXPECTED_DIMENSION) {
            log.error("벡터 차원 불일치: expected={}, actual={}", EXPECTED_DIMENSION, vector.size());
            throw new ReportException(ReportErrorCode.EMBEDDING_INVALID_DIMENSION);
        }
    }

    private void validateProjectVector(List<Double> vector, Integer dimension) {
        if (vector == null || vector.isEmpty()) {
            throw new ProjectException(ProjectErrorCode.EMBEDDING_VECTOR_EMPTY);
        }
        if (dimension != null && dimension != EXPECTED_DIMENSION) {
            log.error("응답 dimension 불일치: expected={}, actual={}", EXPECTED_DIMENSION, dimension);
        }
        if (vector.size() != EXPECTED_DIMENSION) {
            log.error("벡터 차원 불일치: expected={}, actual={}", EXPECTED_DIMENSION, vector.size());
            throw new ProjectException(ProjectErrorCode.EMBEDDING_INVALID_DIMENSION);
        }
    }

    private float[] convertToFloatArray(List<Double> vector) {
        float[] result = new float[vector.size()];
        for (int i = 0; i < vector.size(); i++) {
            result[i] = vector.get(i).floatValue();
        }
        return result;
    }
}
