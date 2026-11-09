package com.umc.devine.admin.payment.service;

import com.umc.devine.admin.payment.dto.AdminPaymentResDTO;

public interface AdminWithdrawalRefundService {

    AdminPaymentResDTO.WithdrawalRefundCompleteDTO complete(Long refundId);
}
