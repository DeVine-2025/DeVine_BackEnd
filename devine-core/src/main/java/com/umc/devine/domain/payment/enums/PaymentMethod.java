package com.umc.devine.domain.payment.enums;

public enum PaymentMethod {
    CARD("PaymentMethodCard"),
    EASY_PAY("PaymentMethodEasyPay"),
    TRANSFER("PaymentMethodTransfer"),
    VIRTUAL_ACCOUNT("PaymentMethodVirtualAccount");

    private final String portOneType;

    PaymentMethod(String portOneType) {
        this.portOneType = portOneType;
    }

    public static PaymentMethod fromPortOneType(String portOneType) {
        for (PaymentMethod method : values()) {
            if (method.portOneType.equals(portOneType)) {
                return method;
            }
        }
        throw new IllegalArgumentException("Unknown PortOne payment method type: " + portOneType);
    }
}
