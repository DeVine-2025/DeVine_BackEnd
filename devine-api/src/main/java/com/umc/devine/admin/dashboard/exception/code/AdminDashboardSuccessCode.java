package com.umc.devine.admin.dashboard.exception.code;

import com.umc.devine.global.apiPayload.code.BaseSuccessCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum AdminDashboardSuccessCode implements BaseSuccessCode {

    DASHBOARD_FOUND(HttpStatus.OK,
            "ADMINDASHBOARD200_1",
            "대시보드 지표를 성공적으로 조회했습니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
