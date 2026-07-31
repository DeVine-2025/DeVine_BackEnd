package com.umc.devine.admin.ticket.service.command;

import com.umc.devine.admin.ticket.dto.AdminTicketResDTO;

public interface AdminTicketCommandService {

    AdminTicketResDTO.ProcessRefundRes processRefundRequest(Long refundRequestId, String processorClerkId);
}
