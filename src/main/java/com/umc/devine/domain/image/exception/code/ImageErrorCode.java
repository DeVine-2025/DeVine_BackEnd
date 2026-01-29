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
            "프로젝트 대표사진은 최대 3개까지 등록 가능합니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
