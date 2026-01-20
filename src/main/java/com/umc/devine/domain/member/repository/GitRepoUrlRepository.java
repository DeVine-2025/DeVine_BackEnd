package com.umc.devine.domain.member.repository;

import com.umc.devine.domain.member.entity.GitRepoUrl;
import com.umc.devine.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GitRepoUrlRepository extends JpaRepository<GitRepoUrl, Long> {
    List<GitRepoUrl> findAllByMember(Member member);
}
