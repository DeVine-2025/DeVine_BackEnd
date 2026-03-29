package com.umc.devine.domain.ticket.controller;

import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.ticket.dto.TicketResDTO;
import com.umc.devine.domain.ticket.exception.code.TicketSuccessCode;
import com.umc.devine.domain.ticket.service.query.TicketQueryService;
import com.umc.devine.global.apiPayload.ApiResponse;
import com.umc.devine.global.security.CurrentMember;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/tickets")
public class TicketController implements TicketControllerDocs {

    private final TicketQueryService ticketQueryService;

    @Override
    @GetMapping("/products")
    public ApiResponse<TicketResDTO.TicketProductListDTO> getActiveProducts() {
        return ApiResponse.onSuccess(
                TicketSuccessCode.PRODUCTS_FOUND,
                ticketQueryService.getActiveProducts()
        );
    }

    @Override
    @GetMapping("/my-credits")
    public ApiResponse<TicketResDTO.ReportCreditDTO> getMyCredits(
            @CurrentMember Member member
    ) {
        return ApiResponse.onSuccess(
                TicketSuccessCode.CREDIT_FOUND,
                ticketQueryService.getMyCredits(member)
        );
    }
}
