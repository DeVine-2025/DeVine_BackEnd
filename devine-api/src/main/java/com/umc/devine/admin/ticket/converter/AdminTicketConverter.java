package com.umc.devine.admin.ticket.converter;

import com.umc.devine.admin.ticket.dto.AdminTicketResDTO;
import com.umc.devine.domain.ticket.entity.CreditRefundRequest;

public class AdminTicketConverter {

    private static final String DETACHED_MEMBER_LABEL = "(하드삭제된 회원)";

    public static AdminTicketResDTO.RefundRequestDTO toRefundRequestDTO(CreditRefundRequest request) {
        return AdminTicketResDTO.RefundRequestDTO.builder()
                .refundRequestId(request.getId())
                .memberNickname(request.getMember() != null ? request.getMember().getNickname() : DETACHED_MEMBER_LABEL)
                .creditAmountAtRequest(request.getCreditAmountAtRequest())
                .status(request.getStatus())
                .requestedAt(request.getRequestedAt())
                .processedAt(request.getProcessedAt())
                .build();
    }

    public static AdminTicketResDTO.ProcessRefundRes toProcessRefundRes(CreditRefundRequest request) {
        return AdminTicketResDTO.ProcessRefundRes.builder()
                .refundRequestId(request.getId())
                .status(request.getStatus())
                .processedAt(request.getProcessedAt())
                .build();
    }
}
