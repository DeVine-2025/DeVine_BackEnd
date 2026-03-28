package com.umc.devine.domain.ticket.service.query;

import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.ticket.converter.TicketConverter;
import com.umc.devine.domain.ticket.dto.TicketResDTO;
import com.umc.devine.domain.ticket.repository.MemberReportCreditRepository;
import com.umc.devine.domain.ticket.repository.TicketProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TicketQueryServiceImpl implements TicketQueryService {

    private final TicketProductRepository ticketProductRepository;
    private final MemberReportCreditRepository memberReportCreditRepository;

    @Override
    public TicketResDTO.TicketProductListDTO getActiveProducts() {
        return TicketConverter.toTicketProductListDTO(ticketProductRepository.findAllByActiveTrue());
    }

    @Override
    public TicketResDTO.ReportCreditDTO getMyCredits(Member member) {
        return memberReportCreditRepository.findByMember(member)
                .map(TicketConverter::toReportCreditDTO)
                .orElseGet(TicketConverter::toReportCreditDTOEmpty);
    }
}
