package com.umc.devine.domain.project.repository;

import com.umc.devine.domain.project.entity.ProjectEmbedding;
import com.umc.devine.global.enums.EmbeddingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectEmbeddingRepository extends JpaRepository<ProjectEmbedding, Long> {

    Optional<ProjectEmbedding> findByProjectId(Long projectId);

    boolean existsByProjectId(Long projectId);

    boolean existsByProjectIdAndStatusAndEmbeddingIsNotNull(Long projectId, EmbeddingStatus status);

    List<ProjectEmbedding> findByStatusAndRetryCountLessThan(EmbeddingStatus status, Integer maxRetryCount);
}
