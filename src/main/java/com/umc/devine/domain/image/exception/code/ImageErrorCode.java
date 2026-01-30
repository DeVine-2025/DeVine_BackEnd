package com.umc.devine.domain.image.exception.code;

import com.umc.devine.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ImageErrorCode implements BaseErrorCode {

    IMAGE_NOT_FOUND(HttpStatus.NOT_FOUND,
            "IMAGE404_1",
            "존재하지 않는 이미지입니다."),

    INVALID_FILE_NAME(HttpStatus.BAD_REQUEST,
            "IMAGE400_1",
            "유효하지 않은 파일 이름입니다."),

    IMAGE_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST,
            "IMAGE400_2",
            "프로젝트 대표사진은 최대 3개까지 등록 가능합니다."),

    UNSUPPORTED_FILE_EXTENSION(HttpStatus.BAD_REQUEST,
            "IMAGE400_3",
            "지원하지 않는 파일 확장자입니다. (jpg, jpeg, png, gif, webp)"),

    S3_PRESIGN_FAILED(HttpStatus.INTERNAL_SERVER_ERROR,
            "IMAGE500_1",
            "Presigned URL 생성 중 오류가 발생했습니다."),

    S3_DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR,
            "IMAGE500_2",
            "이미지 삭제 처리 중 오류가 발생했습니다."),

    IMAGE_NOT_UPLOADED(HttpStatus.BAD_REQUEST,
            "IMAGE400_4",
            "아직 업로드가 완료되지 않은 이미지입니다."),

    IMAGE_ACCESS_DENIED(HttpStatus.FORBIDDEN,
            "IMAGE403_1",
            "해당 이미지에 대한 권한이 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
