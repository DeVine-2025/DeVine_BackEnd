package com.umc.devine.admin.payment.service;

import com.umc.devine.admin.payment.dto.AdminPaymentResDTO;

/**
 * 회원 탈퇴 환불 신청 처리(01-1 FUNC-MEM-004 3번).
 * 관리자 "탈퇴 환불 신청 목록" 화면(Phase 3) 전까지 쓰는 최소 API다.
 */
public interface AdminWithdrawalRefundService {

    /**
     * 운영자가 포트원 콘솔에서 refund_amount만 부분 취소한 뒤 호출한다.
     * 신청 행을 COMPLETED로 바꾸고 환불 연락 이메일을 지운다. PG 호출은 하지 않는다.
     */
    AdminPaymentResDTO.WithdrawalRefundCompleteDTO complete(Long refundId);
}
