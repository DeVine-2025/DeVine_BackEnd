package com.umc.devine.domain.ticket.enums;

public enum CreditRefundStatus {
    REQUESTED,
    PROCESSED,
    /** 관리자가 처리하지 않은 채 회원 하드삭제 유예기간이 만료되어 환불 청구권이 소멸된 상태. */
    EXPIRED
}
