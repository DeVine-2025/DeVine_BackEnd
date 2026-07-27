package com.umc.devine.admin.project.exception.code;

import com.umc.devine.global.apiPayload.code.BaseSuccessCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum AdminProjectSuccessCode implements BaseSuccessCode {

    VISIBILITY_UPDATED(HttpStatus.OK, "ADMIN_PROJECT200_1", "프로젝트 노출 상태가 변경되었습니다."),
    PROJECT_LIST_FOUND(HttpStatus.OK, "ADMIN_PROJECT200_2", "성공적으로 프로젝트 목록을 조회했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
