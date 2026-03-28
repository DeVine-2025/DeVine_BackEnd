package com.umc.devine.domain.ticket.converter;

import com.umc.devine.domain.ticket.dto.TicketResDTO;
import com.umc.devine.domain.ticket.entity.MemberReportCredit;
import com.umc.devine.domain.ticket.entity.TicketProduct;

import java.util.List;

public class TicketConverter {

    public static TicketResDTO.TicketProductDTO toTicketProductDTO(TicketProduct product) {
        return TicketResDTO.TicketProductDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .price(product.getPrice())
                .creditAmount(product.getCreditAmount())
                .build();
    }

    public static TicketResDTO.TicketProductListDTO toTicketProductListDTO(List<TicketProduct> products) {
        return TicketResDTO.TicketProductListDTO.builder()
                .products(products.stream().map(TicketConverter::toTicketProductDTO).toList())
                .build();
    }

    public static TicketResDTO.ReportCreditDTO toReportCreditDTO(MemberReportCredit credit) {
        return TicketResDTO.ReportCreditDTO.builder()
                .remainingCount(credit.getRemainingCount())
                .build();
    }

    public static TicketResDTO.ReportCreditDTO toReportCreditDTOEmpty() {
        return TicketResDTO.ReportCreditDTO.builder()
                .remainingCount(0)
                .build();
    }
}
