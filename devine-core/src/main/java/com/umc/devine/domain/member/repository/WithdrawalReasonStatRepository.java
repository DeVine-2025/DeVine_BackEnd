package com.umc.devine.domain.member.repository;

import com.umc.devine.domain.member.entity.WithdrawalReasonStat;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WithdrawalReasonStatRepository extends JpaRepository<WithdrawalReasonStat, Long> {
}
