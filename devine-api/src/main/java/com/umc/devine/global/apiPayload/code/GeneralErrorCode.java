package com.umc.devine.global.apiPayload.code;

import com.umc.devine.global.exception.DomainErrorReason;
import com.umc.devine.global.exception.GeneralErrorReason;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum GeneralErrorCode implements BaseErrorCode {

    BAD_REQUEST(HttpStatus.BAD_REQUEST, "COMMON400_1", "잘못된 요청입니다.", GeneralErrorReason.BAD_REQUEST),
    VALID_FAIL(HttpStatus.BAD_REQUEST, "COMMON400_2", "요청 데이터의 유효성 검사에 실패했습니다.", GeneralErrorReason.VALID_FAIL),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH401_1", "인증이 필요합니다.", GeneralErrorReason.UNAUTHORIZED),
    FORBIDDEN(HttpStatus.FORBIDDEN, "AUTH403_1", "요청이 거부되었습니다.", GeneralErrorReason.FORBIDDEN),
    NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON404_1", "요청한 리소스를 찾을 수 없습니다.", GeneralErrorReason.NOT_FOUND),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON500_1", "예기치 않은 서버 에러가 발생했습니다.", GeneralErrorReason.INTERNAL_SERVER_ERROR),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
    private final DomainErrorReason reason;
}
