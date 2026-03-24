package com.umc.devine.domain.image.exception.code;

import com.umc.devine.global.exception.DomainErrorReason;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ImageErrorReason implements DomainErrorReason {

    IMAGE_NOT_FOUND("IMAGE404_1", "존재하지 않는 이미지입니다."),
    INVALID_FILE_NAME("IMAGE400_1", "유효하지 않은 파일 이름입니다."),
    IMAGE_LIMIT_EXCEEDED("IMAGE400_2", "프로젝트 대표사진은 최대 3개까지 등록 가능합니다."),
    UNSUPPORTED_FILE_EXTENSION("IMAGE400_3", "지원하지 않는 파일 확장자입니다. (jpg, jpeg, png, gif, webp)"),
    S3_PRESIGN_FAILED("IMAGE500_1", "Presigned URL 생성 중 오류가 발생했습니다."),
    S3_DELETE_FAILED("IMAGE500_2", "이미지 삭제 처리 중 오류가 발생했습니다."),
    IMAGE_NOT_UPLOADED("IMAGE400_4", "아직 업로드가 완료되지 않은 이미지입니다."),
    IMAGE_ACCESS_DENIED("IMAGE403_1", "해당 이미지에 대한 권한이 없습니다."),
    IMAGE_TYPE_MISMATCH("IMAGE400_5", "이미지 타입이 일치하지 않습니다."),
    ;

    private final String code;
    private final String message;
}
