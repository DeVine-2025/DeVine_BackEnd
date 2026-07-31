package com.umc.devine.global.scheduler.harddelete;

import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.repository.MemberAgreementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(70)
@RequiredArgsConstructor
public class MemberAgreementHardDeleteHandler implements MemberHardDeleteHandler {

    private final MemberAgreementRepository memberAgreementRepository;

    @Override
    public void handle(Member member) {
        memberAgreementRepository.bulkDeleteByMember(member);
    }
}
