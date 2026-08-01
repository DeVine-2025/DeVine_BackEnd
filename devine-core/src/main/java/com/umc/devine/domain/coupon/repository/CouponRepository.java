package com.umc.devine.domain.coupon.repository;

import com.umc.devine.domain.coupon.entity.Coupon;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

    Page<Coupon> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * 발급 수량을 원자적으로 증가시킨다. total_issue_limit이 없거나 issued_count가 아직 한도 미만일 때만 증가하며,
     * 영향행이 0이면 호출부에서 한도 초과로 판단한다.
     */
    @Modifying
    @Query("UPDATE Coupon c SET c.issuedCount = c.issuedCount + :count " +
            "WHERE c.id = :couponId AND (c.totalIssueLimit IS NULL OR c.issuedCount + :count <= c.totalIssueLimit)")
    int incrementIssuedCountById(@Param("couponId") Long couponId, @Param("count") int count);

    @Modifying
    @Query("UPDATE Coupon c SET c.usedCount = c.usedCount + 1 WHERE c.id = :couponId")
    int incrementUsedCountById(@Param("couponId") Long couponId);
}
