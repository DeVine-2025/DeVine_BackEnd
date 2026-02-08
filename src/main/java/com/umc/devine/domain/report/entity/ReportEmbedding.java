package com.umc.devine.domain.report.entity;

import com.umc.devine.global.enums.EmbeddingStatus;
import com.umc.devine.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Array;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@Table(name = "report_embedding")
public class ReportEmbedding extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_embedding_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dev_report_id", nullable = false, unique = true)
    private DevReport devReport;

    @JdbcTypeCode(SqlTypes.VECTOR)
    @Array(length = 1536)
    @Column(name = "embedding", columnDefinition = "vector(1536)")
    private float[] embedding;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private EmbeddingStatus status = EmbeddingStatus.PENDING;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private Integer retryCount = 0;

    public void updateSuccess(float[] embedding) {
        this.embedding = embedding;
        this.status = EmbeddingStatus.SUCCESS;
        this.errorMessage = null;
    }

    public void updateFailure(String errorMessage) {
        this.status = EmbeddingStatus.FAILED;
        this.errorMessage = errorMessage;
        this.retryCount++;
    }
}
