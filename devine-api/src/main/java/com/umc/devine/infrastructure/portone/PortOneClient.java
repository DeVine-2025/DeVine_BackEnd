package com.umc.devine.infrastructure.portone;

import com.umc.devine.domain.payment.exception.PaymentException;
import com.umc.devine.domain.payment.exception.code.PaymentErrorReason;
import com.umc.devine.infrastructure.portone.dto.PortOnePaymentResponse;
import io.portone.sdk.server.common.Card;
import io.portone.sdk.server.payment.PaidPayment;
import io.portone.sdk.server.payment.PaymentClient;
import io.portone.sdk.server.payment.PaymentInstallment;
import io.portone.sdk.server.payment.PaymentMethod;
import io.portone.sdk.server.payment.PaymentMethodCard;
import io.portone.sdk.server.payment.PaymentMethodEasyPay;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Component
public class PortOneClient {

    private static final long API_TIMEOUT_SECONDS = 5;

    private final PaymentClient paymentClient;

    public PortOneClient(
            @Value("${portone.store-id}") String storeId,
            @Value("${portone.api-secret}") String apiSecret,
            @Value("${portone.base-url:https://api.portone.io}") String baseUrl
    ) {
        this.paymentClient = new PaymentClient(apiSecret, baseUrl, storeId);
    }

    public PortOnePaymentResponse getPayment(String paymentId) {
        try {
            io.portone.sdk.server.payment.Payment payment =
                    paymentClient.getPayment(paymentId).get(API_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            return mapToResponse(payment);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PaymentException(PaymentErrorReason.PORTONE_API_ERROR);
        } catch (PaymentException e) {
            throw e;
        } catch (TimeoutException e) {
            log.error("PortOne API 타임아웃 - paymentId: {}", paymentId);
            throw new PaymentException(PaymentErrorReason.PORTONE_API_ERROR);
        } catch (ExecutionException | RuntimeException e) {
            log.error("PortOne API 결제 조회 실패 - paymentId: {}, error: {}", paymentId, e.getMessage());
            throw new PaymentException(PaymentErrorReason.PORTONE_API_ERROR);
        }
    }

    private PortOnePaymentResponse mapToResponse(io.portone.sdk.server.payment.Payment payment) {
        if (!(payment instanceof PaidPayment paidPayment)) {
            return new PortOnePaymentResponse(null, "NOT_PAID", null, null, null, null, null, null);
        }

        if (paidPayment.getAmount() == null) {
            log.error("PortOne PaidPayment.amount is null - transactionId: {}", paidPayment.getTransactionId());
            throw new PaymentException(PaymentErrorReason.PORTONE_API_ERROR);
        }
        PortOnePaymentResponse.AmountDetail amountDetail =
                new PortOnePaymentResponse.AmountDetail(paidPayment.getAmount().getTotal());

        String currency = paidPayment.getCurrency() != null
                ? paidPayment.getCurrency().getValue()
                : null;

        String paidAt = paidPayment.getPaidAt() != null
                ? paidPayment.getPaidAt().toString()
                : null;

        PortOnePaymentResponse.MethodDetail methodDetail = buildMethodDetail(paidPayment.getMethod());
        if (methodDetail == null) {
            log.warn("지원하지 않는 결제 수단 - transactionId: {}, method: {}", paidPayment.getTransactionId(), paidPayment.getMethod());
            throw new PaymentException(PaymentErrorReason.UNSUPPORTED_PAYMENT_METHOD);
        }

        String pgProvider = null;
        if (paidPayment.getChannel() != null && paidPayment.getChannel().getPgProvider() != null) {
            pgProvider = paidPayment.getChannel().getPgProvider().getValue();
        }

        String customData = paidPayment.getCustomData();

        return new PortOnePaymentResponse(
                paidPayment.getTransactionId(),
                "PAID",
                amountDetail,
                currency,
                paidAt,
                methodDetail,
                pgProvider,
                customData
        );
    }

    private PortOnePaymentResponse.MethodDetail buildMethodDetail(PaymentMethod method) {
        if (method instanceof PaymentMethodCard cardMethod) {
            return new PortOnePaymentResponse.MethodDetail(
                    "PaymentMethodCard",
                    buildCardInfo(cardMethod.getCard()),
                    cardMethod.getApprovalNumber(),
                    buildInstallmentInfo(cardMethod.getInstallment()),
                    null,
                    null
            );
        }

        if (method instanceof PaymentMethodEasyPay easyPayMethod) {
            String provider = easyPayMethod.getProvider() != null
                    ? easyPayMethod.getProvider().getValue()
                    : null;

            PortOnePaymentResponse.EasyPayMethodInfo easyPayMethodInfo = null;
            if (easyPayMethod.getEasyPayMethod() instanceof PaymentMethodCard easyCard) {
                easyPayMethodInfo = new PortOnePaymentResponse.EasyPayMethodInfo(
                        buildCardInfo(easyCard.getCard()),
                        easyCard.getApprovalNumber(),
                        buildInstallmentInfo(easyCard.getInstallment())
                );
            }

            return new PortOnePaymentResponse.MethodDetail(
                    "PaymentMethodEasyPay",
                    null,
                    null,
                    null,
                    provider,
                    easyPayMethodInfo
            );
        }

        return null;
    }

    private PortOnePaymentResponse.CardInfo buildCardInfo(Card card) {
        if (card == null) return null;
        String brand = card.getBrand() != null ? card.getBrand().getValue() : null;
        String name = card.getName() != null ? card.getName()
                : card.getIssuer();
        return new PortOnePaymentResponse.CardInfo(name, card.getNumber(), brand);
    }

    private PortOnePaymentResponse.InstallmentInfo buildInstallmentInfo(PaymentInstallment installment) {
        if (installment == null) return null;
        return new PortOnePaymentResponse.InstallmentInfo(installment.getMonth());
    }
}
