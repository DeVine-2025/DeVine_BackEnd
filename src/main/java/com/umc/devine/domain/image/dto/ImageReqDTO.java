package com.umc.devine.domain.image.dto;

import com.umc.devine.domain.image.enums.ImageType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

public class ImageReqDTO {

    @Builder
    public record PresignedUrlReq(
            @Schema(description = "이미지 유형 (PROFILE, PROJECT, EDITOR)", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull(message = "이미지 유형은 필수입니다.")
            ImageType imageType,

            @Schema(description = "파일 이름 (확장자 포함)", requiredMode = Schema.RequiredMode.REQUIRED, example = "photo.jpg")
            @NotBlank(message = "파일 이름은 필수입니다.")
            String fileName
    ) {}
}
