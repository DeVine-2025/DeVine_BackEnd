package com.umc.devine.domain.image.controller;

import com.umc.devine.domain.image.dto.ImageReqDTO;
import com.umc.devine.domain.image.dto.ImageResDTO;
import com.umc.devine.domain.image.exception.code.ImageSuccessCode;
import com.umc.devine.domain.image.service.command.ImageCommandService;
import com.umc.devine.global.apiPayload.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/images")
public class ImageController implements ImageControllerDocs {

    private final ImageCommandService imageCommandService;

    @Override
    @PostMapping("/presigned-url")
    public ApiResponse<ImageResDTO.PresignedUrlRes> createPresignedUrl(
            @RequestBody @Valid ImageReqDTO.PresignedUrlReq request
    ) {
        // TODO: 토큰 방식으로 변경
        Long memberId = 1L;

        ImageSuccessCode code = ImageSuccessCode.PRESIGNED_URL_CREATED;
        ImageResDTO.PresignedUrlRes response = imageCommandService.createPresignedUrl(memberId, request);
        return ApiResponse.onSuccess(code, response);
    }

    @Override
    @PatchMapping("/confirm/{imageId}")
    public ApiResponse<Void> confirmUpload(@PathVariable Long imageId) {
        // TODO: 토큰 방식으로 변경
        Long memberId = 1L;

        imageCommandService.confirmUpload(memberId, imageId);
        return ApiResponse.onSuccess(ImageSuccessCode.UPLOAD_CONFIRMED, null);
    }

    @Override
    @DeleteMapping("/{imageId}")
    public ApiResponse<Void> deleteImage(@PathVariable Long imageId) {
        // TODO: 토큰 방식으로 변경
        Long memberId = 1L;

        imageCommandService.deleteImage(memberId, imageId);
        return ApiResponse.onSuccess(ImageSuccessCode.IMAGE_DELETED, null);
    }
}
