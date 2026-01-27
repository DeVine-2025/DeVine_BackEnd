package com.umc.devine.domain.report.exception.code;

import com.umc.devine.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ReportErrorCode implements BaseErrorCode {

    REPORT_NOT_FOUND(HttpStatus.NOT_FOUND, "REPORT404_1", "해당 리포트를 찾을 수 없습니다."),
    GIT_REPO_NOT_FOUND(HttpStatus.NOT_FOUND, "REPORT404_2", "해당 Git 저장소를 찾을 수 없습니다."),
    UNAUTHORIZED_ACCESS(HttpStatus.FORBIDDEN, "REPORT403_1", "해당 리포트에 대한 권한이 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
