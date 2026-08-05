package com.umc.devine.domain.notice.enums;

/**
 * 공지사항의 노출 상태. DB에 저장하는 값이 아니라 조회 시점의 현재 시각으로 계산하는 파생값이다.
 * (게시 기간에 따른 자동 노출/비노출을 스케줄러 없이 처리하므로 저장할 상태 컬럼이 없다.)
 */
public enum NoticeDisplayStatus {

    /** 관리자가 수동으로 비노출 처리함 (기간과 무관하게 노출되지 않음) */
    HIDDEN,

    /** 노출 대상이지만 게시 시작 일시가 아직 도래하지 않음 */
    SCHEDULED,

    /** 현재 노출 중 */
    DISPLAYING,

    /** 게시 종료 일시가 지남 */
    ENDED
}