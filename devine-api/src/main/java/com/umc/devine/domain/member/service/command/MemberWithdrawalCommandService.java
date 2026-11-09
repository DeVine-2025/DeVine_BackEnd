package com.umc.devine.domain.member.service.command;

import com.umc.devine.domain.member.dto.WithdrawalResDTO;
import com.umc.devine.domain.member.entity.Member;

public interface MemberWithdrawalCommandService {

    /** 대기 지원·제안 일괄 취소(FUNC-MEM-002). 상대방에게 알리고, 걸린 프로젝트마다 마감 확인 훅을 부른다. */
    WithdrawalResDTO.CancelPendingResultDTO cancelPendingMatchings(Member member);
}
