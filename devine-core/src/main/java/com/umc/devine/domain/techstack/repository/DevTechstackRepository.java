package com.umc.devine.domain.techstack.repository;

import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.techstack.entity.Techstack;
import com.umc.devine.domain.techstack.entity.mapping.DevTechstack;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DevTechstackRepository extends JpaRepository<DevTechstack, Long> {
    List<DevTechstack> findAllByMember(Member member);
    List<DevTechstack> findAllByMemberIn(List<Member> members);
    List<DevTechstack> findAllByMemberAndTechstackIn(Member member, List<Techstack> techstacks);
    void deleteAllByMemberAndTechstackIn(Member member, List<Techstack> techstacks);

    @Query("SELECT dt FROM DevTechstack dt JOIN FETCH dt.techstack WHERE dt.member = :member")
    List<DevTechstack> findAllByMemberWithTechstack(@Param("member") Member member);

    @Query("SELECT dt FROM DevTechstack dt JOIN FETCH dt.techstack WHERE dt.member IN :members")
    List<DevTechstack> findAllByMemberInWithTechstack(@Param("members") List<Member> members);

    @Query("SELECT dt FROM DevTechstack dt JOIN FETCH dt.techstack WHERE dt.member = :member AND dt.techstack IN :techstacks")
    List<DevTechstack> findAllByMemberAndTechstackInWithTechstack(@Param("member") Member member, @Param("techstacks") List<Techstack> techstacks);
}
