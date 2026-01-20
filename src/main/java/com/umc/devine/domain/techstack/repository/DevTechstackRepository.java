package com.umc.devine.domain.techstack.repository;

import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.techstack.entity.mapping.DevTechstack;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DevTechstackRepository extends JpaRepository<DevTechstack, DevTechstack.DevTechstackId> {
    List<DevTechstack> findAllByMember(Member member);
}
