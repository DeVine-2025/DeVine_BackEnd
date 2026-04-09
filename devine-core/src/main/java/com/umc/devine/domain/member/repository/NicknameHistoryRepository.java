package com.umc.devine.domain.member.repository;

import com.umc.devine.domain.member.entity.NicknameHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NicknameHistoryRepository extends JpaRepository<NicknameHistory, Long> {
}
