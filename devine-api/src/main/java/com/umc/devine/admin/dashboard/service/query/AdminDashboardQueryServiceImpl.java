package com.umc.devine.admin.dashboard.service.query;

import com.umc.devine.admin.complaint.enums.ComplaintStatus;
import com.umc.devine.admin.complaint.repository.ComplaintRepository;
import com.umc.devine.admin.dashboard.dto.AdminDashboardResDTO;
import com.umc.devine.domain.coupon.repository.CouponRepository;
import com.umc.devine.domain.payment.enums.TransactionStatus;
import com.umc.devine.domain.payment.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminDashboardQueryServiceImpl implements AdminDashboardQueryService {

    private final ComplaintRepository complaintRepository;
    private final CouponRepository couponRepository;
    private final TransactionRepository transactionRepository;

    @Override
    public AdminDashboardResDTO.DashboardDTO getDashboard() {
        return new AdminDashboardResDTO.DashboardDTO(
                metricOrNull("신고 대기 건수", this::countPendingComplaints),
                metricOrNull("쿠폰 사용률", this::calculateCouponUsageRate),
                metricOrNull("오늘 결제 건수", this::countTodayPayments)
        );
    }

    /**
     * 한 지표의 조회 실패가 대시보드 전체 실패로 번지지 않도록, 실패한 지표만 null로 내린다.
     * 조회 전용 트랜잭션이라 실패한 쿼리가 나머지 조회를 오염시키지 않는다.
     */
    private <T> T metricOrNull(String metricName, Supplier<T> supplier) {
        try {
            return supplier.get();
        } catch (Exception e) {
            log.warn("관리자 대시보드 지표 조회 실패 - {}", metricName, e);
            return null;
        }
    }

    private Long countPendingComplaints() {
        return complaintRepository.countByStatus(ComplaintStatus.PENDING);
    }

    private Double calculateCouponUsageRate() {
        CouponRepository.UsageSum sum = couponRepository.sumIssuedAndUsedCount();
        if (sum.getIssuedCount() == 0) {
            return 0.0;
        }
        return BigDecimal.valueOf(sum.getUsedCount())
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(sum.getIssuedCount()), 1, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private Long countTodayPayments() {
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        return transactionRepository.countByStatusAndPaidAtGreaterThanEqualAndPaidAtLessThan(
                TransactionStatus.PAID, startOfToday, startOfToday.plusDays(1));
    }
}
