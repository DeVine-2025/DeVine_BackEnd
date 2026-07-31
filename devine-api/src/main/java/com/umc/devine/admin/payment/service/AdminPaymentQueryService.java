package com.umc.devine.admin.payment.service;

import com.umc.devine.admin.payment.dto.AdminPaymentReqDTO;
import com.umc.devine.admin.payment.dto.AdminPaymentResDTO;
import com.umc.devine.global.dto.PagedResponse;
import org.springframework.data.domain.Pageable;

public interface AdminPaymentQueryService {

    PagedResponse<AdminPaymentResDTO.PaymentSummaryDTO> searchPayments(
            AdminPaymentReqDTO.SearchDTO condition, Pageable pageable);

    AdminPaymentResDTO.PaymentDetailDTO getPaymentDetail(Long paymentId);
}
