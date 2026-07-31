package com.umc.devine.admin.ticket.service.query;

import com.umc.devine.admin.ticket.dto.AdminTicketReqDTO;
import com.umc.devine.admin.ticket.dto.AdminTicketResDTO;
import com.umc.devine.global.dto.PagedResponse;

public interface AdminTicketQueryService {

    PagedResponse<AdminTicketResDTO.RefundRequestDTO> getRefundRequests(AdminTicketReqDTO.RefundRequestSearchReq request);
}
