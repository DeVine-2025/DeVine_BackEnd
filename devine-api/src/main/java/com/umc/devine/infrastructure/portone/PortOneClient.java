package com.umc.devine.infrastructure.portone;

import com.umc.devine.domain.payment.exception.PaymentException;
import com.umc.devine.domain.payment.exception.code.PaymentErrorReason;
import com.umc.devine.infrastructure.portone.dto.CancelOutcome;
import com.umc.devine.infrastructure.portone.dto.PortOnePaymentResponse;
import io.portone.sdk.server.common.Card;
import io.portone.sdk.server.errors.CancelPaymentException;
import io.portone.sdk.server.errors.PaymentAlreadyCancelledException;
import io.portone.sdk.server.errors.PgProviderException;
import io.portone.sdk.server.errors.UnknownException;
import io.portone.sdk.server.payment.CancelPaymentResponse;
import io.portone.sdk.server.payment.CancelledPayment;
import io.portone.sdk.server.payment.PaidPayment;
import io.portone.sdk.server.payment.PaymentCancellation;
import io.portone.sdk.server.payment.PaymentClient;
import io.portone.sdk.server.payment.PaymentInstallment;
import io.portone.sdk.server.payment.PaymentMethod;
import io.portone.sdk.server.payment.PaymentMethodCard;
import io.portone.sdk.server.payment.PaymentMethodEasyPay;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
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

    /**
     * 결제를 전액 취소(환불)
     *
     * 예외를 던지지 않고 CancelOutcome으로 변환해 반환한다. 이중 환불 방지를 위해
     * 타임아웃/IO/모호한 PG 응답은 CancelOutcome.Unknown으로 떨어뜨리고(절대 실패로 단정하지 않음),
     * 이미 취소된 결제는 성공으로 흡수한다(재시도 안전망).
     */
    public CancelOutcome cancelPayment(String paymentId, String reason) {
        try {
            CancelPaymentResponse response = paymentClient
                    // 금액류(amount/taxFree/vat)를 null로 넘기면 전액 취소
                    .cancelPayment(paymentId, null, null, null, reason, null, null, null, null, null, null)
                    .get(API_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            return toSucceeded(response.getCancellation(), false);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new CancelOutcome.Unknown("취소 호출 인터럽트");
        } catch (TimeoutException e) {
            log.error("PortOne 취소 API 타임아웃 - paymentId: {}", paymentId);
            return new CancelOutcome.Unknown("타임아웃");
        } catch (ExecutionException e) {
            return classifyCancelFailure(paymentId, e.getCause() != null ? e.getCause() : e);
        } catch (RuntimeException e) {
            log.error("PortOne 취소 예기치 못한 오류 - paymentId: {}, error: {}", paymentId, e.getMessage());
            return new CancelOutcome.Unknown(e.getMessage());
        }
    }

    private CancelOutcome classifyCancelFailure(String paymentId, Throwable cause) {
        if (cause instanceof PaymentAlreadyCancelledException) {
            log.info("PortOne 이미 취소된 결제 — 성공으로 흡수 - paymentId: {}", paymentId);
            return fetchLatestCancellation(paymentId);
        }
        if (cause instanceof PgProviderException || cause instanceof UnknownException) {
            log.error("PortOne 취소 결과 불명(PG/알수없음) - paymentId: {}, error: {}", paymentId, cause.getMessage());
            return new CancelOutcome.Unknown(cause.getMessage());
        }
        if (cause instanceof CancelPaymentException) {
            log.warn("PortOne 취소 거절 - paymentId: {}, error: {}", paymentId, cause.getMessage());
            return new CancelOutcome.Rejected(cause.getMessage());
        }
        log.error("PortOne 취소 실패(상태 불명) - paymentId: {}, error: {}", paymentId, cause.getMessage());
        return new CancelOutcome.Unknown(cause.getMessage());
    }

    /** 이미 취소된 결제의 최신 취소 내역을 조회해 Succeeded로 변환한다. 조회 실패 시 Unknown. */
    private CancelOutcome fetchLatestCancellation(String paymentId) {
        try {
            io.portone.sdk.server.payment.Payment payment =
                    paymentClient.getPayment(paymentId).get(API_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (payment instanceof CancelledPayment cancelledPayment) {
                List<PaymentCancellation> cancellations = cancelledPayment.getCancellations();
                if (cancellations != null && !cancellations.isEmpty()) {
                    return toSucceeded(cancellations.get(cancellations.size() - 1), true);
                }
            }
            log.error("이미 취소됐으나 취소 내역을 확인할 수 없음 - paymentId: {}", paymentId);
            return new CancelOutcome.Unknown("이미 취소됨(취소 내역 조회 실패)");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new CancelOutcome.Unknown("취소 내역 조회 인터럽트");
        } catch (Exception e) {
            log.error("취소 내역 조회 실패 - paymentId: {}, error: {}", paymentId, e.getMessage());
            return new CancelOutcome.Unknown("취소 내역 조회 실패");
        }
    }

    private CancelOutcome toSucceeded(PaymentCancellation cancellation, boolean alreadyCancelled) {
        if (!(cancellation instanceof PaymentCancellation.Recognized recognized)) {
            log.error("PortOne 취소 응답을 인식할 수 없음 - cancellation: {}", cancellation);
            return new CancelOutcome.Unknown("취소 응답 인식 불가");
        }
        LocalDateTime cancelledAt = recognized.getCancelledAt() != null
                ? LocalDateTime.ofInstant(recognized.getCancelledAt(), ZoneId.systemDefault())
                : null;
        return new CancelOutcome.Succeeded(
                recognized.getId(),
                recognized.getTotalAmount(),
                cancelledAt,
                alreadyCancelled
        );
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
