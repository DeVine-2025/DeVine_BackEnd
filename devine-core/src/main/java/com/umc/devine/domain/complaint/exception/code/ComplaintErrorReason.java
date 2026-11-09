package com.umc.devine.domain.complaint.exception.code;

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
    NOTIFY_SUMMARY_REQUIRED(HttpStatus.BAD_REQUEST, "COMPLAINT400_3", "통보용 사유 요약은 필수입니다."),
    ACTION_NOT_ALLOWED_FOR_TARGET(HttpStatus.BAD_REQUEST, "COMPLAINT400_4", "회원 신고에는 고를 수 없는 세부 액션입니다."),
    DETAIL_REQUIRED(HttpStatus.BAD_REQUEST, "COMPLAINT400_5", "사유가 기타일 때는 상세 내용을 입력해 주세요."),

    TARGET_NOT_FOUND(HttpStatus.NOT_FOUND, "COMPLAINT404_2", "지금은 이 대상을 볼 수 없어요."),
    SELF_COMPLAINT(HttpStatus.BAD_REQUEST, "COMPLAINT400_6", "본인 항목은 신고할 수 없어요."),
    DUPLICATE_TARGET(HttpStatus.CONFLICT, "COMPLAINT409_1", "이미 신고한 항목이에요."),
    DUPLICATE_RESPONDENT(HttpStatus.CONFLICT, "COMPLAINT409_2", "이미 신고한 회원이에요. 처리 결과를 알림으로 알려 드려요."),
    DAILY_LIMIT_EXCEEDED(HttpStatus.CONFLICT, "COMPLAINT409_3",
            "오늘은 더 신고할 수 없어요. 급한 경우 projectdevine2025@gmail.com으로 알려 주세요."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
