package com.umc.devine.admin.maintenance.exception.code;

import com.umc.devine.global.apiPayload.code.BaseSuccessCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum MaintenanceSuccessCode implements BaseSuccessCode {

    MAINTENANCE_STATE_FOUND(HttpStatus.OK,
            "MAINTENANCE200_1",
            "점검 모드 상태를 성공적으로 조회했습니다."),
    MAINTENANCE_STATE_UPDATED(HttpStatus.OK,
            "MAINTENANCE200_2",
            "점검 모드 상태를 성공적으로 변경했습니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
