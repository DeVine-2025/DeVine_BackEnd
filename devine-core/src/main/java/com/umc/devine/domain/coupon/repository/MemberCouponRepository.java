package com.umc.devine.domain.coupon.repository;

import com.umc.devine.domain.coupon.entity.MemberCoupon;
import com.umc.devine.domain.member.entity.Member;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MemberCouponRepository extends JpaRepository<MemberCoupon, Long> {

    List<MemberCoupon> findByMemberOrderByCreatedAtDesc(Member member);

    Optional<MemberCoupon> findByIdAndMember(Long id, Member member);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT mc FROM MemberCoupon mc WHERE mc.id = :id AND mc.member = :member")
    Optional<MemberCoupon> findByIdAndMemberWithLock(@Param("id") Long id, @Param("member") Member member);
}
