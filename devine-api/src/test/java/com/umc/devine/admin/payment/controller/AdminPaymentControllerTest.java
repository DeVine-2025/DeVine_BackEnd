package com.umc.devine.admin.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.devine.admin.auth.security.AdminPrincipal;
import com.umc.devine.admin.enums.AdminLevel;
import com.umc.devine.admin.payment.dto.AdminPaymentReqDTO;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.enums.MemberMainType;
import com.umc.devine.domain.member.enums.MemberStatus;
import com.umc.devine.domain.member.repository.MemberRepository;
import com.umc.devine.domain.payment.entity.Payment;
import com.umc.devine.domain.payment.entity.Transaction;
import com.umc.devine.domain.payment.enums.PaymentMethod;
import com.umc.devine.domain.payment.enums.TransactionStatus;
import com.umc.devine.domain.payment.enums.TransactionType;
import com.umc.devine.domain.payment.repository.PaymentRepository;
import com.umc.devine.domain.ticket.entity.PaymentTicket;
import com.umc.devine.domain.ticket.entity.TicketProduct;
import com.umc.devine.domain.ticket.repository.TicketProductRepository;
import com.umc.devine.infrastructure.portone.PortOneClient;
import com.umc.devine.infrastructure.portone.dto.CancelOutcome;
import com.umc.devine.support.ControllerIntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminPaymentControllerTest extends ControllerIntegrationTestSupport {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private TicketProductRepository ticketProductRepository;

    @Autowired
    private ObjectMapper objectMapper;

    /** 환불은 외부 PG 호출을 포함하므로 컨트롤러 계층 검증에서는 취소 결과만 주입한다. */
    @MockitoBean
    private PortOneClient portOneClient;

    private Member member;
    private Payment payment;
    private Authentication adminAuth;
    private Authentication memberAuth;

    @BeforeEach
    void setUp() {
        AdminPrincipal principal = AdminPrincipal.builder()
                .clerkId("clerk_admin")
                .email("admin@example.com")
                .name("관리자")
                .level(AdminLevel.ADMIN)
                .build();
        adminAuth = new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        memberAuth = new UsernamePasswordAuthenticationToken(
                "clerk_member", null, List.of(new SimpleGrantedAuthority("ROLE_USER")));

        member = memberRepository.save(Member.builder()
                .clerkId("clerk_payer")
                .name("결제자")
                .nickname("payer")
                .mainType(MemberMainType.DEVELOPER)
                .disclosure(true)
                .used(MemberStatus.ACTIVE)
                .build());

        TicketProduct product = ticketProductRepository.save(TicketProduct.builder()
                .name("리포트 생성권 1개")
                .price(4900L)
                .creditAmount(1)
                .active(true)
                .build());

        payment = savePayment(product);
    }

    @Nested
    @DisplayName("관리자 인증/인가")
    class AuthorizationTest {

        @Test
        @DisplayName("인증 없이 호출하면 401을 반환한다")
        void returns401WithoutAuthentication() throws Exception {
            mockMvc.perform(get("/admin/v1/payments"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("일반 회원 권한으로 호출하면 403을 반환한다")
        void returns403ForNonAdmin() throws Exception {
            mockMvc.perform(get("/admin/v1/payments")
                            .with(authentication(memberAuth)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("환불도 관리자 권한이 없으면 403을 반환한다")
        void refundRequiresAdminRole() throws Exception {
            mockMvc.perform(post("/admin/v1/payments/{paymentId}/refund", payment.getId())
                            .with(authentication(memberAuth))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new AdminPaymentReqDTO.RefundDTO("고객 요청"))))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /admin/v1/payments")
    class SearchPayments {

        @Test
        @DisplayName("관리자는 결제 내역을 페이징 조회할 수 있다")
        void returnsPagedList() throws Exception {
            mockMvc.perform(get("/admin/v1/payments").param("page", "1").param("size", "10")
                            .with(authentication(adminAuth)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.result.content").isArray())
                    .andExpect(jsonPath("$.result.totalElements").value(1))
                    .andExpect(jsonPath("$.result.content[0].memberNickname").value("payer"));
        }

        @Test
        @DisplayName("닉네임 조건에 맞는 결제가 없으면 빈 목록을 반환한다")
        void returnsEmptyWhenNoMatch() throws Exception {
            mockMvc.perform(get("/admin/v1/payments").param("memberNickname", "no-such-user")
                            .with(authentication(adminAuth)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.totalElements").value(0));
        }
    }

    @Nested
    @DisplayName("GET /admin/v1/payments/{paymentId}")
    class GetPaymentDetail {

        @Test
        @DisplayName("결제 상세를 조회한다")
        void returnsDetail() throws Exception {
            mockMvc.perform(get("/admin/v1/payments/{paymentId}", payment.getId())
                            .with(authentication(adminAuth)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.paymentId").value(payment.getId()))
                    .andExpect(jsonPath("$.result.memberNickname").value("payer"))
                    .andExpect(jsonPath("$.result.tickets").isArray());
        }

        @Test
        @DisplayName("존재하지 않는 결제면 404를 반환한다")
        void returns404WhenNotFound() throws Exception {
            mockMvc.perform(get("/admin/v1/payments/{paymentId}", 999_999L)
                            .with(authentication(adminAuth)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /admin/v1/payments/{paymentId}/refund")
    class Refund {

        @Test
        @DisplayName("관리자는 결제를 환불할 수 있다")
        void refundsPayment() throws Exception {
            given(portOneClient.cancelPayment(anyString(), anyString()))
                    .willReturn(new CancelOutcome.Succeeded(
                            "cancellation_1", 4900L, LocalDateTime.now(), false));

            mockMvc.perform(post("/admin/v1/payments/{paymentId}/refund", payment.getId())
                            .with(authentication(adminAuth))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new AdminPaymentReqDTO.RefundDTO("고객 요청"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.cancellationId").value("cancellation_1"))
                    .andExpect(jsonPath("$.result.amount").value(4900));
        }

        @Test
        @DisplayName("환불 사유가 비어 있으면 400을 반환한다")
        void returns400WhenReasonBlank() throws Exception {
            mockMvc.perform(post("/admin/v1/payments/{paymentId}/refund", payment.getId())
                            .with(authentication(adminAuth))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new AdminPaymentReqDTO.RefundDTO(" "))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("존재하지 않는 결제면 404를 반환한다")
        void returns404WhenNotFound() throws Exception {
            mockMvc.perform(post("/admin/v1/payments/{paymentId}/refund", 999_999L)
                            .with(authentication(adminAuth))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new AdminPaymentReqDTO.RefundDTO("고객 요청"))))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("PG 취소 결과가 불명이면 504를 반환한다")
        void returns504WhenCancelResultUnknown() throws Exception {
            given(portOneClient.cancelPayment(anyString(), anyString()))
                    .willReturn(new CancelOutcome.Unknown("timeout"));

            mockMvc.perform(post("/admin/v1/payments/{paymentId}/refund", payment.getId())
                            .with(authentication(adminAuth))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new AdminPaymentReqDTO.RefundDTO("고객 요청"))))
                    .andExpect(status().isGatewayTimeout());
        }
    }

    private Payment savePayment(TicketProduct product) {
        Payment saved = Payment.builder()
                .portonePaymentId("payment_admin_ctrl")
                .member(member)
                .orderName("리포트 생성권 1개")
                .amount(product.getPrice())
                .currency("KRW")
                .build();

        saved.addTransaction(Transaction.builder()
                .portoneTransactionId("tx_admin_ctrl")
                .payment(saved)
                .type(TransactionType.PAYMENT)
                .status(TransactionStatus.PAID)
                .method(PaymentMethod.CARD)
                .pgProvider("TOSSPAYMENTS")
                .amount(product.getPrice())
                .paidAt(LocalDateTime.now().minusDays(1))
                .build());

        saved.addPaymentTicket(PaymentTicket.builder()
                .payment(saved)
                .ticketProduct(product)
                .quantity(1)
                .unitPrice(product.getPrice())
                .unitCreditAmount(product.getCreditAmount())
                .build());

        return paymentRepository.saveAndFlush(saved);
    }
}
