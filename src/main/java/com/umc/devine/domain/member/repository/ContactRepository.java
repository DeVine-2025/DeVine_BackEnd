package com.umc.devine.domain.member.repository;

import com.umc.devine.domain.member.entity.Contact;
import com.umc.devine.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContactRepository extends JpaRepository<Contact, Long> {
    List<Contact> findAllByMember(Member member);
    void deleteAllByMember(Member member);
}
