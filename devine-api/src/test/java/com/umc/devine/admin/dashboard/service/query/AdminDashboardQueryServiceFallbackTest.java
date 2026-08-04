package com.umc.devine.admin.dashboard.service.query;

import com.umc.devine.admin.complaint.repository.ComplaintRepository;
import com.umc.devine.admin.dashboard.dto.AdminDashboardResDTO;
import com.umc.devine.domain.coupon.repository.CouponRepository;
import com.umc.devine.domain.payment.repository.TransactionRepository;
import com.umc.devine.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

/**
 * 한 지표의 조회 실패가 대시보드 전체 실패로 번지지 않는지 검증한다.
 * 리포지토리에서 실제 장애를 재현할 수 없으므로 세 리포지토리를 모두 목으로 대체한다.
 */
class AdminDashboardQueryServiceFallbackTest extends IntegrationTestSupport {

    @Autowired
    private AdminDashboardQueryService adminDashboardQueryService;

    @MockitoBean
    private ComplaintRepository complaintRepository;

    @MockitoBean
    private CouponRepository couponRepository;

    @MockitoBean
    private TransactionRepository transactionRepository;

    @BeforeEach
    void setUp() {
        CouponRepository.UsageSum usageSum = Mockito.mock(CouponRepository.UsageSum.class);
        Mockito.when(usageSum.getIssuedCount()).thenReturn(100L);
        Mockito.when(usageSum.getUsedCount()).thenReturn(25L);

        Mockito.when(complaintRepository.countByStatus(any())).thenReturn(7L);
        Mockito.when(couponRepository.sumIssuedAndUsedCount()).thenReturn(usageSum);
        Mockito.when(transactionRepository.countByStatusAndPaidAtGreaterThanEqualAndPaidAtLessThan(
                any(), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(3L);
    }

    @Test
    @DisplayName("신고 대기 건수 조회가 실패해도 나머지 지표는 정상 반환한다")
    void pendingComplaintCountFails() {
        Mockito.when(complaintRepository.countByStatus(any()))
                .thenThrow(new DataAccessResourceFailureException("DB 연결 실패"));

        AdminDashboardResDTO.DashboardDTO result = adminDashboardQueryService.getDashboard();

        assertThat(result.pendingComplaintCount()).isNull();
        assertThat(result.couponUsageRate()).isEqualTo(25.0);
        assertThat(result.todayPaymentCount()).isEqualTo(3L);
    }

    @Test
    @DisplayName("쿠폰 사용률 조회가 실패해도 나머지 지표는 정상 반환한다")
    void couponUsageRateFails() {
        Mockito.when(couponRepository.sumIssuedAndUsedCount())
                .thenThrow(new DataAccessResourceFailureException("DB 연결 실패"));

        AdminDashboardResDTO.DashboardDTO result = adminDashboardQueryService.getDashboard();

        assertThat(result.pendingComplaintCount()).isEqualTo(7L);
        assertThat(result.couponUsageRate()).isNull();
        assertThat(result.todayPaymentCount()).isEqualTo(3L);
    }

    @Test
    @DisplayName("오늘 결제 건수 조회가 실패해도 나머지 지표는 정상 반환한다")
    void todayPaymentCountFails() {
        Mockito.when(transactionRepository.countByStatusAndPaidAtGreaterThanEqualAndPaidAtLessThan(
                        any(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenThrow(new DataAccessResourceFailureException("DB 연결 실패"));

        AdminDashboardResDTO.DashboardDTO result = adminDashboardQueryService.getDashboard();

        assertThat(result.pendingComplaintCount()).isEqualTo(7L);
        assertThat(result.couponUsageRate()).isEqualTo(25.0);
        assertThat(result.todayPaymentCount()).isNull();
    }
}
