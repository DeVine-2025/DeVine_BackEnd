package com.umc.devine.domain.report.exception.code;

import com.umc.devine.global.apiPayload.code.BaseSuccessCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ReportSuccessCode implements BaseSuccessCode {

    REPORT_FOUND(HttpStatus.OK, "REPORT200_1", "성공적으로 리포트를 조회했습니다."),
    VISIBILITY_UPDATED(HttpStatus.OK, "REPORT200_2", "성공적으로 공개 범위를 수정했습니다."),
    REPORT_GENERATION_REQUESTED(HttpStatus.ACCEPTED, "REPORT202_1", "리포트 생성 요청이 접수되었습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
