package com.umc.devine.domain.ticket.service.query;

import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.ticket.dto.TicketResDTO;

public interface TicketQueryService {
    TicketResDTO.TicketProductListDTO getActiveProducts();
    TicketResDTO.ReportCreditDTO getMyCredits(Member member);
}
