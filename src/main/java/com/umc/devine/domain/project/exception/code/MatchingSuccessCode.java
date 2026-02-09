package com.umc.devine.domain.project.exception.code;

import com.umc.devine.global.apiPayload.code.BaseSuccessCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum MatchingSuccessCode implements BaseSuccessCode {

    APPLY_SUCCESS(HttpStatus.CREATED,
            "MATCHING201_1",
            "프로젝트 지원이 완료되었습니다."),
    CANCEL_SUCCESS(HttpStatus.OK,
            "MATCHING200_1",
            "프로젝트 지원이 취소되었습니다."),
    PROPOSE_SUCCESS(HttpStatus.CREATED,
            "MATCHING201_2",
            "프로젝트 제안이 완료되었습니다."),
    APPLICATION_ACCEPTED(HttpStatus.OK,
            "MATCHING200_2",
            "지원을 수락했습니다."),
    APPLICATION_REJECTED(HttpStatus.OK,
            "MATCHING200_3",
            "지원을 거절했습니다."),
    PROPOSAL_ACCEPTED(HttpStatus.OK,
            "MATCHING200_4",
            "제안을 수락했습니다."),
    PROPOSAL_REJECTED(HttpStatus.OK,
            "MATCHING200_5",
            "제안을 거절했습니다."),
    GET_DEVELOPERS_SUCCESS(HttpStatus.OK,
            "MATCHING200_6",
            "개발자 목록 조회에 성공했습니다."),
    GET_PROJECTS_SUCCESS(HttpStatus.OK,
            "MATCHING200_7",
            "프로젝트 목록 조회에 성공했습니다."),
    GET_MY_MATCHING_STATUS_SUCCESS(HttpStatus.OK,
            "MATCHING200_8",
            "내 지원 상태 조회에 성공했습니다."),
    GET_APPLICANT_STATUS_SUCCESS(HttpStatus.OK,
            "MATCHING200_9",
            "지원자 상태 조회에 성공했습니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
