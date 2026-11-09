package com.umc.devine.admin.payment.service;

import com.umc.devine.admin.payment.dto.AdminPaymentResDTO;

/** 관리자 탈퇴 환불 신청 목록 화면이 생기기 전까지 쓰는 최소 API */
public interface AdminWithdrawalRefundService {

    /** 운영자가 포트원 콘솔에서 refund_amount만 부분 취소한 뒤 호출한다. PG는 호출하지 않는다. */
    AdminPaymentResDTO.WithdrawalRefundCompleteDTO complete(Long refundId);
}
