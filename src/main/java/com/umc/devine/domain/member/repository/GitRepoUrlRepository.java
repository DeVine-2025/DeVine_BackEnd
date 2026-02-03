package com.umc.devine.domain.member.repository;

import com.umc.devine.domain.member.entity.GitRepoUrl;
import com.umc.devine.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GitRepoUrlRepository extends JpaRepository<GitRepoUrl, Long> {
    List<GitRepoUrl> findAllByMember(Member member);

    @Query("SELECT g FROM GitRepoUrl g JOIN FETCH g.member WHERE g.id = :id")
    Optional<GitRepoUrl> findByIdWithMember(@Param("id") Long id);
}
