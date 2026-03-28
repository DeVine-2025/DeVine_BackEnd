package com.umc.devine.domain.payment.service.command;

import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.payment.dto.PaymentReqDTO;
import com.umc.devine.domain.payment.dto.PaymentResDTO;

public interface PaymentCommandService {
    PaymentResDTO.PaymentDTO completePayment(PaymentReqDTO.CompletePaymentDTO request, Member member);

    void handleWebhookPayment(String portonePaymentId);
}
