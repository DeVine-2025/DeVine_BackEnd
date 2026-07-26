package com.umc.devine.admin.project.exception.code;

import com.umc.devine.global.apiPayload.code.BaseSuccessCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum AdminProjectSuccessCode implements BaseSuccessCode {

    VISIBILITY_UPDATED(HttpStatus.OK, "ADMIN_PROJECT200_1", "프로젝트 노출 상태가 변경되었습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
