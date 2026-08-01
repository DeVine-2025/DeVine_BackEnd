package com.umc.devine.admin.member.repository;

import com.umc.devine.admin.member.entity.MemberStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MemberStatusHistoryRepository extends JpaRepository<MemberStatusHistory, Long> {

    List<MemberStatusHistory> findByMemberIdOrderByCreatedAtDesc(Long memberId);
}
