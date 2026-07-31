package com.umc.devine.global.scheduler.harddelete;

import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.repository.MemberStatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 보존 요구사항은 없지만 member_id가 NOT NULL FK라 남겨두면 하드삭제가 막힌다.
 * 이 회원이 다른 회원의 상태변경을 처리한 이력(processor)이 있으면 그쪽 참조도 끊는다.
 */
@Component
@Order(40)
@RequiredArgsConstructor
public class MemberStatusHistoryHardDeleteHandler implements MemberHardDeleteHandler {

    private final MemberStatusHistoryRepository memberStatusHistoryRepository;

    @Override
    public void handle(Member member) {
        memberStatusHistoryRepository.bulkDeleteByMember(member);
        memberStatusHistoryRepository.bulkNullifyProcessor(member);
    }
}
