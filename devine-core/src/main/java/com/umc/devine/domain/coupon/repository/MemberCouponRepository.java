package com.umc.devine.domain.coupon.repository;

import com.umc.devine.domain.coupon.entity.Coupon;
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

    /**
     * coupon과 (nullable) applicableTicketProduct까지 즉시 로딩한다.
     * 트랜잭션 밖(예: PaymentCommandServiceImpl의 1차 검증)에서도 LazyInitializationException 없이 안전하게 사용하기 위함.
     */
    @Query("SELECT mc FROM MemberCoupon mc " +
            "JOIN FETCH mc.coupon c " +
            "LEFT JOIN FETCH c.applicableTicketProduct " +
            "WHERE mc.id = :id AND mc.member = :member")
    Optional<MemberCoupon> findByIdAndMember(@Param("id") Long id, @Param("member") Member member);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT mc FROM MemberCoupon mc JOIN FETCH mc.coupon WHERE mc.id = :id AND mc.member = :member")
    Optional<MemberCoupon> findByIdAndMemberWithLock(@Param("id") Long id, @Param("member") Member member);

    /** 주어진 회원 후보 목록 중 이미 해당 쿠폰을 보유한 회원만 골라낸다 (직접 발급 시 중복 발급 방지용). */
    @Query("SELECT mc.member FROM MemberCoupon mc WHERE mc.coupon = :coupon AND mc.member IN :members")
    List<Member> findMembersAlreadyHolding(@Param("coupon") Coupon coupon, @Param("members") List<Member> members);
}
