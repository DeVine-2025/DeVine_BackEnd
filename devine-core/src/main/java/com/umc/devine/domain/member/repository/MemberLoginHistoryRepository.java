package com.umc.devine.domain.member.repository;

import com.umc.devine.domain.member.entity.MemberLoginHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface MemberLoginHistoryRepository extends JpaRepository<MemberLoginHistory, Long> {

    List<MemberLoginHistory> findTop10ByMemberIdOrderByLoginAtDesc(Long memberId);

    boolean existsByMemberIdAndLoginAtAfter(Long memberId, LocalDateTime threshold);
}
