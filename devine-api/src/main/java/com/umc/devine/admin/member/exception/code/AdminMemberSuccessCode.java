package com.umc.devine.admin.member.exception.code;

import com.umc.devine.global.apiPayload.code.BaseSuccessCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum AdminMemberSuccessCode implements BaseSuccessCode {

    MEMBER_LIST_FOUND(HttpStatus.OK, "MEMBERADMIN200_1", "성공적으로 유저 목록을 조회했습니다."),
    MEMBER_DETAIL_FOUND(HttpStatus.OK, "MEMBERADMIN200_2", "성공적으로 유저 상세를 조회했습니다."),
    STATUS_CHANGED(HttpStatus.OK, "MEMBERADMIN200_3", "성공적으로 계정 상태를 변경했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
