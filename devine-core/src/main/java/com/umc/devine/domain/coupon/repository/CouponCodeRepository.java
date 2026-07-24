package com.umc.devine.domain.coupon.repository;

import com.umc.devine.domain.coupon.entity.CouponCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CouponCodeRepository extends JpaRepository<CouponCode, Long> {

    Optional<CouponCode> findByCode(String code);

    boolean existsByCode(String code);
}
