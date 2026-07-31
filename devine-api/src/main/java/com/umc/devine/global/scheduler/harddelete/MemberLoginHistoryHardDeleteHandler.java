package com.umc.devine.global.scheduler.harddelete;

import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.repository.MemberLoginHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(80)
@RequiredArgsConstructor
public class MemberLoginHistoryHardDeleteHandler implements MemberHardDeleteHandler {

    private final MemberLoginHistoryRepository memberLoginHistoryRepository;

    @Override
    public void handle(Member member) {
        memberLoginHistoryRepository.bulkDeleteByMember(member);
    }
}
