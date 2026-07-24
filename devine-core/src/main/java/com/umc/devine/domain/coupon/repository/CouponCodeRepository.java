package com.umc.devine.domain.coupon.repository;

import com.umc.devine.domain.coupon.entity.CouponCode;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CouponCodeRepository extends JpaRepository<CouponCode, Long> {

    Optional<CouponCode> findByCode(String code);

    boolean existsByCode(String code);

    /** 동시에 같은 코드가 등록되어 이중으로 보유 쿠폰이 생성되는 것을 막기 위한 잠금 조회. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT cc FROM CouponCode cc JOIN FETCH cc.coupon WHERE cc.code = :code")
    Optional<CouponCode> findByCodeWithLock(@Param("code") String code);
}
