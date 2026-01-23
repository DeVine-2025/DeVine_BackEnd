package com.umc.devine.domain.project.exception.code;

import com.umc.devine.global.apiPayload.code.BaseSuccessCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ProjectSuccessCode implements BaseSuccessCode {

    CREATED(HttpStatus.CREATED,
            "PROJECT201_1",
            "프로젝트가 성공적으로 생성되었습니다."),

    UPDATED(HttpStatus.OK,
            "PROJECT200_1",
            "프로젝트가 성공적으로 수정되었습니다."),

    DELETED(HttpStatus.OK,
            "PROJECT200_2",
            "프로젝트가 성공적으로 삭제되었습니다."),

    FOUND(HttpStatus.OK,
            "PROJECT200_3",
            "프로젝트 조회에 성공하였습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}