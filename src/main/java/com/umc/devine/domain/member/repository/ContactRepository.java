package com.umc.devine.domain.member.repository;

import com.umc.devine.domain.member.entity.Contact;
import com.umc.devine.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ContactRepository extends JpaRepository<Contact, Long> {
    List<Contact> findAllByMember(Member member);
    List<Contact> findAllByMemberIn(List<Member> members);
    void deleteAllByMember(Member member);

    @Query("SELECT c FROM Contact c JOIN FETCH c.member WHERE c.member = :member")
    List<Contact> findAllByMemberWithMember(@Param("member") Member member);

    @Query("SELECT c FROM Contact c JOIN FETCH c.member WHERE c.member IN :members")
    List<Contact> findAllByMemberInWithMember(@Param("members") List<Member> members);
}
