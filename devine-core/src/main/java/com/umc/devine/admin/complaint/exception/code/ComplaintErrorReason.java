package com.umc.devine.admin.complaint.exception.code;

import com.umc.devine.global.exception.DomainErrorReason;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ComplaintErrorReason implements DomainErrorReason {

    COMPLAINT_NOT_FOUND(HttpStatus.NOT_FOUND, "COMPLAINT404_1", "해당 신고를 찾을 수 없습니다."),
    ACTION_REQUIRED(HttpStatus.BAD_REQUEST, "COMPLAINT400_1", "처리완료 시 세부 액션은 필수입니다."),
    RESOLUTION_REASON_REQUIRED(HttpStatus.BAD_REQUEST, "COMPLAINT400_2", "처리 사유는 필수입니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
