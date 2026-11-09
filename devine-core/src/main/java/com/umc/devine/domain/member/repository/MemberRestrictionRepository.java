package com.umc.devine.domain.member.repository;

import com.umc.devine.domain.member.entity.MemberRestriction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface MemberRestrictionRepository extends JpaRepository<MemberRestriction, Long> {

    boolean existsByEmailHashAndExpiresAtAfter(String emailHash, LocalDateTime now);

    List<MemberRestriction> findAllByExpiresAtBefore(LocalDateTime now);
}
