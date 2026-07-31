package com.umc.devine.domain.member.repository;

import com.umc.devine.domain.member.entity.WithdrawnMemberEmailHash;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface WithdrawnMemberEmailHashRepository extends JpaRepository<WithdrawnMemberEmailHash, Long> {

    @Query("SELECT COUNT(w) > 0 FROM WithdrawnMemberEmailHash w WHERE w.emailHash = :emailHash AND w.expiresAt > :now")
    boolean existsActiveByEmailHash(@Param("emailHash") String emailHash, @Param("now") LocalDateTime now);
}
