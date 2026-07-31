package com.umc.devine.global.scheduler.harddelete;

import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.ticket.repository.MemberReportCreditRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** 잔여 생성권은 탈퇴 시점에 이미 소멸/환불신청 처리가 끝난 상태라, 남은 행은 그대로 삭제한다. */
@Component
@Order(60)
@RequiredArgsConstructor
public class MemberReportCreditHardDeleteHandler implements MemberHardDeleteHandler {

    private final MemberReportCreditRepository memberReportCreditRepository;

    @Override
    public void handle(Member member) {
        memberReportCreditRepository.bulkDeleteByMember(member);
    }
}
