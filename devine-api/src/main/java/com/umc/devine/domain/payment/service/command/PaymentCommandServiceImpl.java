package com.umc.devine.domain.payment.service.command;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.repository.MemberRepository;
import com.umc.devine.domain.payment.converter.PaymentConverter;
import com.umc.devine.domain.payment.dto.PaymentReqDTO;
import com.umc.devine.domain.payment.dto.PaymentResDTO;
import com.umc.devine.domain.payment.entity.Payment;
import com.umc.devine.domain.payment.entity.Transaction;
import com.umc.devine.domain.payment.enums.PaymentMethod;
import com.umc.devine.domain.payment.exception.PaymentException;
import com.umc.devine.domain.payment.exception.code.PaymentErrorReason;
import com.umc.devine.domain.payment.repository.PaymentRepository;
import com.umc.devine.domain.ticket.entity.MemberReportCredit;
import com.umc.devine.domain.ticket.entity.PaymentTicket;
import com.umc.devine.domain.ticket.entity.TicketProduct;
import com.umc.devine.domain.ticket.exception.TicketException;
import com.umc.devine.domain.ticket.exception.code.TicketErrorReason;
import com.umc.devine.domain.ticket.repository.MemberReportCreditRepository;
import com.umc.devine.domain.ticket.repository.PaymentTicketRepository;
import com.umc.devine.domain.ticket.repository.TicketProductRepository;
import com.umc.devine.infrastructure.portone.PortOneClient;
import com.umc.devine.infrastructure.portone.dto.PortOnePaymentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentCommandServiceImpl implements PaymentCommandService {

    private final PaymentRepository paymentRepository;
    private final PortOneClient portOneClient;
    private final TransactionTemplate transactionTemplate;
    private final TicketProductRepository ticketProductRepository;
    private final PaymentTicketRepository paymentTicketRepository;
    private final MemberReportCreditRepository memberReportCreditRepository;
    private final MemberRepository memberRepository;
    private final ObjectMapper objectMapper;

    @Override
    public PaymentResDTO.PaymentDTO completePayment(PaymentReqDTO.CompletePaymentDTO request, Member member) {
        // 1. 멱등성 체크
        if (paymentRepository.existsByPortonePaymentId(request.paymentId())) {
            throw new PaymentException(PaymentErrorReason.ALREADY_PROCESSED_PAYMENT);
        }

        // 2. 중복 상품 검증
        long distinctCount = request.items().stream()
                .map(PaymentReqDTO.TicketPurchaseItem::ticketProductId)
                .distinct().count();
        if (distinctCount != request.items().size()) {
            throw new PaymentException(PaymentErrorReason.DUPLICATE_TICKET_PRODUCT);
        }

        // 3. 티켓 상품 유효성 검증 (트랜잭션 밖 — DB 커넥션 점유 방지)
        List<TicketProduct> ticketProducts = validateAndGetProducts(request.items());

        // 3. 서버 측 금액 검증 (상품 가격 × 수량 합계 == 요청 금액)
        long expectedAmount = calculateExpectedAmount(request.items(), ticketProducts);
        if (!request.amount().equals(expectedAmount)) {
            throw new PaymentException(PaymentErrorReason.AMOUNT_MISMATCH);
        }

        // 4. PortOne API에서 결제 정보 조회 (트랜잭션 밖 — DB 커넥션 점유 방지)
        PortOnePaymentResponse portOneResponse = portOneClient.getPayment(request.paymentId());

        // 5. 결제 상태 검증
        if (!"PAID".equals(portOneResponse.status())) {
            throw new PaymentException(PaymentErrorReason.PAYMENT_NOT_PAID);
        }

        // 6. 결제 소유자 검증 (customData.memberId == 현재 사용자)
        verifyPaymentOwner(portOneResponse, member);

        // 7. 결제 금액 검증 (PortOne 실결제 금액 == 요청 금액)
        if (!request.amount().equals(portOneResponse.amount().total())) {
            throw new PaymentException(PaymentErrorReason.AMOUNT_MISMATCH);
        }

        // 7. MemberReportCredit 행을 미리 보장 (트랜잭션 밖 — 동시 요청 시 UNIQUE 위반 방지)
        ensureCreditRowExists(member);

        // 8. Payment, Transaction, PaymentTicket 저장 및 크레딧 지급 (트랜잭션 안)
        try {
            return Objects.requireNonNull(transactionTemplate.execute(status -> {
                Payment payment = PaymentConverter.toPayment(request, member, portOneResponse);
                Payment savedPayment = savePaymentWithTickets(payment, portOneResponse, request.items(), ticketProducts, member);
                return PaymentConverter.toPaymentDTO(savedPayment);
            }));
        } catch (DataIntegrityViolationException e) {
            throw new PaymentException(PaymentErrorReason.ALREADY_PROCESSED_PAYMENT);
        }
    }

    @Override
    public void handleWebhookPayment(String portonePaymentId) {
        // 이미 처리된 결제면 무시 (멱등성)
        if (paymentRepository.existsByPortonePaymentId(portonePaymentId)) {
            log.info("웹훅: 이미 처리된 결제 - paymentId: {}", portonePaymentId);
            return;
        }

        // PortOne API에서 결제 정보 조회
        PortOnePaymentResponse portOneResponse = portOneClient.getPayment(portonePaymentId);
        if (!"PAID".equals(portOneResponse.status())) {
            log.info("웹훅: 결제 미완료 상태 - paymentId: {}, status: {}", portonePaymentId, portOneResponse.status());
            return;
        }

        // customData에서 memberId, items 파싱
        // 프론트엔드에서 결제 요청 시 customData에 JSON 형태로 포함해야 함
        // 예: {"memberId": 1, "items": [{"ticketProductId": 1, "quantity": 2}], "orderName": "리포트 생성권 1개 x2"}
        if (portOneResponse.customData() == null) {
            log.warn("웹훅: customData 없음 - paymentId: {}", portonePaymentId);
            return;
        }

        JsonNode customData;
        try {
            customData = objectMapper.readTree(portOneResponse.customData());
        } catch (JsonProcessingException e) {
            log.error("웹훅: customData 파싱 실패 - paymentId: {}", portonePaymentId, e);
            return;
        }

        Long memberId = customData.path("memberId").asLong(0);
        String orderName = customData.path("orderName").asText("웹훅 결제");
        if (memberId == 0) {
            log.error("웹훅: customData에 memberId 없음 - paymentId: {}", portonePaymentId);
            return;
        }

        Member member = memberRepository.findById(memberId).orElse(null);
        if (member == null) {
            log.error("웹훅: 존재하지 않는 회원 - paymentId: {}, memberId: {}", portonePaymentId, memberId);
            return;
        }

        // 상품 목록 파싱 및 검증
        JsonNode itemsNode = customData.path("items");
        if (!itemsNode.isArray() || itemsNode.isEmpty()) {
            log.error("웹훅: customData에 items 없음 - paymentId: {}", portonePaymentId);
            return;
        }

        List<PaymentReqDTO.TicketPurchaseItem> items = new java.util.ArrayList<>();
        for (JsonNode itemNode : itemsNode) {
            items.add(new PaymentReqDTO.TicketPurchaseItem(
                    itemNode.path("ticketProductId").asLong(),
                    itemNode.path("quantity").asInt()
            ));
        }

        List<TicketProduct> ticketProducts = items.stream()
                .map(item -> ticketProductRepository.findById(item.ticketProductId()).orElse(null))
                .toList();

        if (ticketProducts.stream().anyMatch(p -> p == null || !p.getActive())) {
            log.error("웹훅: 유효하지 않은 상품 포함 - paymentId: {}", portonePaymentId);
            return;
        }

        // 금액 검증
        long expectedAmount = calculateExpectedAmount(items, ticketProducts);
        if (expectedAmount != portOneResponse.amount().total()) {
            log.error("웹훅: 금액 불일치 - paymentId: {}, expected: {}, actual: {}",
                    portonePaymentId, expectedAmount, portOneResponse.amount().total());
            return;
        }

        ensureCreditRowExists(member);

        try {
            transactionTemplate.execute(status -> {
                Payment payment = Payment.builder()
                        .portonePaymentId(portonePaymentId)
                        .member(member)
                        .orderName(orderName)
                        .amount(portOneResponse.amount().total())
                        .currency(portOneResponse.currency())
                        .build();

                savePaymentWithTickets(payment, portOneResponse, items, ticketProducts, member);
                return null;
            });
            log.info("웹훅: 결제 처리 완료 - paymentId: {}, memberId: {}", portonePaymentId, memberId);
        } catch (DataIntegrityViolationException e) {
            // 동시에 /complete API 호출로 이미 처리됨
            log.info("웹훅: 동시 처리로 이미 저장됨 - paymentId: {}", portonePaymentId);
        }
    }

    private List<TicketProduct> validateAndGetProducts(List<PaymentReqDTO.TicketPurchaseItem> items) {
        return items.stream()
                .map(item -> {
                    TicketProduct product = ticketProductRepository.findById(item.ticketProductId())
                            .orElseThrow(() -> new TicketException(TicketErrorReason.PRODUCT_NOT_FOUND));
                    if (!product.getActive()) {
                        throw new TicketException(TicketErrorReason.PRODUCT_NOT_ACTIVE);
                    }
                    return product;
                })
                .toList();
    }

    private long calculateExpectedAmount(List<PaymentReqDTO.TicketPurchaseItem> items, List<TicketProduct> ticketProducts) {
        long expectedAmount = 0;
        for (int i = 0; i < items.size(); i++) {
            expectedAmount += ticketProducts.get(i).getPrice() * items.get(i).quantity();
        }
        return expectedAmount;
    }

    private Payment savePaymentWithTickets(Payment payment, PortOnePaymentResponse portOneResponse,
                                           List<PaymentReqDTO.TicketPurchaseItem> items,
                                           List<TicketProduct> ticketProducts, Member member) {
        Transaction transaction = PaymentConverter.toTransaction(portOneResponse, payment);
        attachPaymentDetail(transaction, portOneResponse.method());
        payment.addTransaction(transaction);
        paymentRepository.save(payment);

        int totalCredits = 0;
        for (int i = 0; i < items.size(); i++) {
            PaymentReqDTO.TicketPurchaseItem item = items.get(i);
            TicketProduct product = ticketProducts.get(i);

            PaymentTicket paymentTicket = PaymentTicket.builder()
                    .payment(payment)
                    .ticketProduct(product)
                    .quantity(item.quantity())
                    .unitPrice(product.getPrice())
                    .unitCreditAmount(product.getCreditAmount())
                    .build();
            paymentTicketRepository.save(paymentTicket);
            payment.addPaymentTicket(paymentTicket);

            totalCredits += product.getCreditAmount() * item.quantity();
        }

        grantCredits(member, totalCredits);
        return payment;
    }

    private void ensureCreditRowExists(Member member) {
        if (memberReportCreditRepository.findByMember(member).isEmpty()) {
            try {
                memberReportCreditRepository.saveAndFlush(MemberReportCredit.of(member, 0));
            } catch (DataIntegrityViolationException ignored) {
                // 동시 요청으로 이미 생성됨 — 무시
            }
        }
    }

    private void grantCredits(Member member, int amount) {
        int updated = memberReportCreditRepository.addCreditsByMember(member, amount);
        if (updated == 0) {
            throw new PaymentException(PaymentErrorReason.CREDIT_UPDATE_FAILED);
        }
    }

    private void verifyPaymentOwner(PortOnePaymentResponse portOneResponse, Member member) {
        if (portOneResponse.customData() == null) return;
        try {
            JsonNode customData = objectMapper.readTree(portOneResponse.customData());
            long paymentMemberId = customData.path("memberId").asLong(0);
            if (paymentMemberId != 0 && !member.getId().equals(paymentMemberId)) {
                throw new PaymentException(PaymentErrorReason.PAYMENT_OWNER_MISMATCH);
            }
        } catch (PaymentException e) {
            throw e;
        } catch (Exception e) {
            log.warn("customData 파싱 실패, 소유자 검증 스킵 - paymentId: {}", portOneResponse.transactionId());
        }
    }

    private void attachPaymentDetail(Transaction transaction, PortOnePaymentResponse.MethodDetail method) {
        if (method == null) return;

        PaymentMethod paymentMethod;
        try {
            paymentMethod = PaymentMethod.fromPortOneType(method.type());
        } catch (IllegalArgumentException e) {
            throw new PaymentException(PaymentErrorReason.PORTONE_API_ERROR);
        }

        if (paymentMethod == PaymentMethod.CARD) {
            transaction.addCardDetail(PaymentConverter.toCardDetail(method));
        } else if (paymentMethod == PaymentMethod.EASY_PAY) {
            transaction.addEasyPayDetail(PaymentConverter.toEasyPayDetail(method));
        }
    }
}
