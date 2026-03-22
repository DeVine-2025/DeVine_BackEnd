package com.umc.devine.domain.member.repository;

import com.umc.devine.domain.member.entity.Terms;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TermsRepository extends JpaRepository<Terms, Long> {

    List<Terms> findAllByRequired(Boolean required);
}
