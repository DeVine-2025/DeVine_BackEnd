package com.umc.devine.domain.techstack.repository;

import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.techstack.entity.Techstack;
import com.umc.devine.domain.techstack.entity.mapping.DevTechstack;
import com.umc.devine.domain.techstack.enums.TechstackSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DevTechstackRepository extends JpaRepository<DevTechstack, Long> {
    List<DevTechstack> findAllByMember(Member member);

    /** GitHub 데이터 삭제용: GitHub(리포트 분석)에서 가져온 AUTO 행만 지우고, 직접 입력한 MANUAL은 남긴다. */
    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM DevTechstack dt WHERE dt.member = :member AND dt.source = :source")
    int deleteAllByMemberAndSource(@Param("member") Member member, @Param("source") TechstackSource source);

    /** 회원 탈퇴용: 보유 기술 전체 삭제. */
    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM DevTechstack dt WHERE dt.member = :member")
    int deleteAllByMemberBulk(@Param("member") Member member);
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
