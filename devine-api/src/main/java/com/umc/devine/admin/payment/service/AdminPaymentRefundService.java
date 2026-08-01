package com.umc.devine.admin.payment.service;

import com.umc.devine.admin.payment.dto.AdminPaymentResDTO;

public interface AdminPaymentRefundService {

    AdminPaymentResDTO.RefundResultDTO refund(Long paymentId, String reason);
}
