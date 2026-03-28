package com.umc.devine.infrastructure.portone.dto;

public record PortOnePaymentResponse(
        String transactionId,
        String status,
        AmountDetail amount,
        String currency,
        String paidAt,
        MethodDetail method,
        String pgProvider
) {

    public record AmountDetail(Long total) {}

    public record MethodDetail(
            String type,
            CardInfo card,
            String approvalNumber,
            InstallmentInfo installment,
            String provider,
            EasyPayMethodInfo easyPayMethod
    ) {}

    public record CardInfo(
            String name,
            String number,
            String brand
    ) {}

    public record InstallmentInfo(Integer month) {}

    public record EasyPayMethodInfo(
            CardInfo card,
            String approvalNumber,
            InstallmentInfo installment
    ) {}
}
