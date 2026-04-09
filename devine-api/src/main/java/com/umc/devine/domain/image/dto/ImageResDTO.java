package com.umc.devine.domain.image.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

public class ImageResDTO {

    @Builder
    public record PresignedUrlRes(
            @Schema(description = "생성된 이미지 ID", example = "1")
            Long imageId,

            @Schema(description = "S3 presigned PUT URL (이 URL로 파일을 PUT 업로드)")
            String presignedUrl,

            @Schema(description = "업로드 완료 후 접근 가능한 이미지 URL")
            String imageUrl
    ) {}
}
