package com.umc.devine.admin.member.exception.code;

import com.umc.devine.global.exception.DomainErrorReason;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum MemberAdminErrorReason implements DomainErrorReason {

    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "MEMBERADMIN404_1", "해당 유저를 찾을 수 없습니다."),
    REASON_REQUIRED(HttpStatus.BAD_REQUEST, "MEMBERADMIN400_1", "처리 사유는 필수입니다."),
    ALREADY_WITHDRAWN(HttpStatus.BAD_REQUEST, "MEMBERADMIN400_2", "이미 탈퇴 상태인 계정입니다."),
    INVALID_STATUS_TRANSITION(HttpStatus.BAD_REQUEST, "MEMBERADMIN400_3", "현재 계정 상태에서는 처리할 수 없는 요청입니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
