package com.umc.devine.admin.ticket.service.command;

import com.umc.devine.admin.ticket.converter.AdminTicketConverter;
import com.umc.devine.admin.ticket.dto.AdminTicketResDTO;
import com.umc.devine.admin.ticket.exception.AdminTicketException;
import com.umc.devine.admin.ticket.exception.code.AdminTicketErrorReason;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.repository.MemberRepository;
import com.umc.devine.domain.ticket.entity.CreditRefundRequest;
import com.umc.devine.domain.ticket.enums.CreditRefundStatus;
import com.umc.devine.domain.ticket.repository.CreditRefundRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminTicketCommandServiceImpl implements AdminTicketCommandService {

    private final CreditRefundRequestRepository creditRefundRequestRepository;
    private final MemberRepository memberRepository;

    @Override
    public AdminTicketResDTO.ProcessRefundRes processRefundRequest(Long refundRequestId, String processorClerkId) {
        CreditRefundRequest request = creditRefundRequestRepository.findByIdForUpdate(refundRequestId)
                .orElseThrow(() -> new AdminTicketException(AdminTicketErrorReason.REFUND_REQUEST_NOT_FOUND));

        if (request.getStatus() == CreditRefundStatus.PROCESSED) {
            throw new AdminTicketException(AdminTicketErrorReason.ALREADY_PROCESSED);
        }

        Member processor = processorClerkId != null ? memberRepository.findByClerkId(processorClerkId).orElse(null) : null;
        request.process(processor);

        return AdminTicketConverter.toProcessRefundRes(request);
    }
}
