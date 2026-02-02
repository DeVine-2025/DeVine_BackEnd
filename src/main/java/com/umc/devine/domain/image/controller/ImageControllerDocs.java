package com.umc.devine.domain.image.controller;

import com.umc.devine.domain.image.dto.ImageReqDTO;
import com.umc.devine.domain.image.dto.ImageResDTO;
import com.umc.devine.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Image", description = "이미지 업로드 관련 API")
public interface ImageControllerDocs {

    @Operation(
            summary = "Presigned URL 생성 API",
            description = "S3 presigned PUT URL을 생성합니다. 클라이언트는 반환된 presignedUrl로 파일을 직접 업로드합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "CREATED, Presigned URL 생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청입니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증이 필요합니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없습니다.")
    })
    ApiResponse<ImageResDTO.PresignedUrlRes> createPresignedUrl(
            @Valid @RequestBody ImageReqDTO.PresignedUrlReq request);

    @Operation(
            summary = "이미지 업로드 확인 API",
            description = "클라이언트가 S3 업로드 완료 후 호출하여 업로드 상태를 확인합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK, 업로드 확인 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 이미지입니다.")
    })
    ApiResponse<Void> confirmUpload(@PathVariable Long imageId);

    @Operation(
            summary = "이미지 삭제 API",
            description = "이미지 ID로 S3 오브젝트 및 DB 레코드를 삭제합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK, 이미지 삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 이미지입니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "이미지 삭제 처리 중 오류가 발생했습니다.")
    })
    ApiResponse<Void> deleteImage(@PathVariable Long imageId);
}
