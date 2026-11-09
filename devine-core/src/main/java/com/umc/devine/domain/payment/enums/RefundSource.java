package com.umc.devine.domain.payment.enums;

public enum RefundSource {
    /** 관리자가 결제 전체를 PG 취소한 환불. */
    ADMIN,
    /** 회원 탈퇴 시 남은 생성권 환불 신청. 운영자가 refund_amount만 부분 취소한다. */
    WITHDRAWAL
}
