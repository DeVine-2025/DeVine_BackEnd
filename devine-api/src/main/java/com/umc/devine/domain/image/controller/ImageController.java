package com.umc.devine.domain.image.controller;

import com.umc.devine.domain.image.dto.ImageReqDTO;
import com.umc.devine.domain.image.dto.ImageResDTO;
import com.umc.devine.domain.image.exception.code.ImageSuccessCode;
import com.umc.devine.domain.image.service.command.ImageCommandService;
import com.umc.devine.global.apiPayload.ApiResponse;
import com.umc.devine.global.security.ClerkPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/images")
@Validated
public class ImageController implements ImageControllerDocs {

    private final ImageCommandService imageCommandService;

    @Override
    @PostMapping("/presigned-url")
    public ApiResponse<ImageResDTO.PresignedUrlRes> createPresignedUrl(
            @AuthenticationPrincipal ClerkPrincipal principal,
            @RequestBody @Valid ImageReqDTO.PresignedUrlReq request
    ) {
        ImageSuccessCode code = ImageSuccessCode.PRESIGNED_URL_CREATED;
        ImageResDTO.PresignedUrlRes response = imageCommandService.createPresignedUrl(principal, request);
        return ApiResponse.onSuccess(code, response);
    }

    @Override
    @PatchMapping("/confirm/{imageId}")
    public ApiResponse<Void> confirmUpload(
            @AuthenticationPrincipal ClerkPrincipal principal,
            @PathVariable Long imageId
    ) {
        imageCommandService.confirmUpload(principal, imageId);
        return ApiResponse.onSuccess(ImageSuccessCode.UPLOAD_CONFIRMED, null);
    }

    @Override
    @DeleteMapping("/{imageId}")
    public ApiResponse<Void> deleteImage(
            @AuthenticationPrincipal ClerkPrincipal principal,
            @PathVariable Long imageId
    ) {
        imageCommandService.deleteImage(principal, imageId);
        return ApiResponse.onSuccess(ImageSuccessCode.IMAGE_DELETED, null);
    }
}
