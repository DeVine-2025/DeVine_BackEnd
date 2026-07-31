package com.umc.devine.admin.ticket.service.query;

import com.umc.devine.admin.ticket.converter.AdminTicketConverter;
import com.umc.devine.admin.ticket.dto.AdminTicketReqDTO;
import com.umc.devine.admin.ticket.dto.AdminTicketResDTO;
import com.umc.devine.domain.ticket.entity.CreditRefundRequest;
import com.umc.devine.domain.ticket.repository.CreditRefundRequestRepository;
import com.umc.devine.global.dto.PagedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminTicketQueryServiceImpl implements AdminTicketQueryService {

    private final CreditRefundRequestRepository creditRefundRequestRepository;

    @Override
    public PagedResponse<AdminTicketResDTO.RefundRequestDTO> getRefundRequests(AdminTicketReqDTO.RefundRequestSearchReq request) {
        Page<CreditRefundRequest> page = request.status() != null
                ? creditRefundRequestRepository.findAllByStatus(request.status(), request.toPageable())
                : creditRefundRequestRepository.findAll(request.toPageable());

        List<AdminTicketResDTO.RefundRequestDTO> content = page.getContent().stream()
                .map(AdminTicketConverter::toRefundRequestDTO)
                .toList();

        return PagedResponse.of(page, content);
    }
}
