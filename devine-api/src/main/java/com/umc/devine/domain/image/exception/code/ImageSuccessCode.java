package com.umc.devine.domain.image.exception.code;

import com.umc.devine.global.apiPayload.code.BaseSuccessCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ImageSuccessCode implements BaseSuccessCode {

    PRESIGNED_URL_CREATED(HttpStatus.CREATED,
            "IMAGE201_1",
            "Presigned URL이 성공적으로 생성되었습니다."),

    IMAGE_DELETED(HttpStatus.OK,
            "IMAGE200_1",
            "이미지가 성공적으로 삭제되었습니다."),

    UPLOAD_CONFIRMED(HttpStatus.OK,
            "IMAGE200_2",
            "업로드가 확인되었습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
