package com.umc.devine.domain.techstack.repository;

import com.umc.devine.domain.techstack.entity.Techstack;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TechstackRepository extends JpaRepository<Techstack, Long> {
}